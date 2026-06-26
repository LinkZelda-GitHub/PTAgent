package com.ptagent.service;

import com.ptagent.common.Json;
import com.ptagent.common.XlsxReader;
import com.ptagent.domain.Demand;
import com.ptagent.exception.ApiException;
import com.ptagent.exception.ErrorCode;
import com.ptagent.repository.Repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DemandImportService {
    private static final Pattern CODE_PATTERN = Pattern.compile("([A-Z]{1,4}\\d{3,6})");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final List<String> REGIONS = List.of("南山区", "宝安区", "龙岗区", "罗湖区", "龙华区", "盐田区",
            "光明区", "观澜区", "福田区", "天河区", "越秀区", "海珠区", "荔湾区", "番禺区");

    private final Repository repository;

    public DemandImportService(Repository repository) {
        this.repository = repository;
    }

    public Map<String, Object> importDemandXlsx(Map<String, Object> body) {
        long adminId = Json.longValue(body, "adminId", 0);
        if (adminId == 0) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "管理员ID不能为空");
        }
        String filePath = Json.str(body, "filePath");
        if (filePath.isBlank()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "文件路径不能为空");
        }
        return importDemandXlsx(resolveImportPath(filePath), adminId);
    }

    public Map<String, Object> importDemandXlsx(Path path, long adminId) {
        if (!Files.isRegularFile(path)) {
            throw ApiException.notFound(ErrorCode.IMPORT_FILE_NOT_FOUND, "导入文件不存在");
        }
        if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw ApiException.badRequest(ErrorCode.IMPORT_INVALID_FILE, "仅支持.xlsx文件");
        }

        List<XlsxReader.Sheet> sheets = XlsxReader.read(path);
        if (sheets.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.IMPORT_INVALID_FILE, "XLSX中没有可读取的工作表");
        }
        XlsxReader.Sheet sheet = sheets.get(0);
        if (sheet.rows().isEmpty()) {
            throw ApiException.badRequest(ErrorCode.IMPORT_INVALID_FILE, "XLSX第一张表为空");
        }

        List<String> headers = sheet.rows().get(0);
        List<Map<String, Object>> imported = new ArrayList<>();
        int skipped = 0;
        int failed = 0;
        for (int rowIndex = 1; rowIndex < sheet.rows().size(); rowIndex++) {
            List<String> row = sheet.rows().get(rowIndex);
            int max = Math.max(row.size(), headers.size());
            for (int column = 0; column < max; column++) {
                String raw = value(row, column).trim();
                if (raw.isBlank()) {
                    continue;
                }
                ParsedDemand parsed = parseCell(raw, value(headers, column), adminId);
                if (!parsed.valid()) {
                    failed++;
                    continue;
                }
                if (!parsed.code().isBlank() && alreadyImported(parsed.code())) {
                    skipped++;
                    continue;
                }
                Demand demand = repository.createDemand(parsed.demand());
                imported.add(Json.object(
                        "id", demand.id,
                        "code", parsed.code(),
                        "title", demand.grade + demand.subject,
                        "region", demand.region,
                        "salaryRange", demand.salaryRange
                ));
            }
        }

        return Json.object(
                "sheetName", sheet.name(),
                "importedCount", imported.size(),
                "skippedCount", skipped,
                "failedCount", failed,
                "items", imported
        );
    }

    private Path resolveImportPath(String filePath) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path path = Path.of(filePath);
        if (!path.isAbsolute()) {
            path = root.resolve(path);
        }
        path = path.normalize();
        if (!path.startsWith(root)) {
            throw ApiException.badRequest(ErrorCode.IMPORT_INVALID_FILE, "导入文件必须位于项目目录内");
        }
        return path;
    }

    private ParsedDemand parseCell(String raw, String regionHeader, long adminId) {
        String code = firstMatch(CODE_PATTERN, raw);
        String address = stripCityMarker(field(raw, "联系地址"));
        String gradeGender = field(raw, "年级性别");
        String subjectText = field(raw, "辅导科目");
        String score = field(raw, "学员成绩");
        String schedule = field(raw, "时间安排");
        String teacherRequirement = field(raw, "老师要求");
        String salary = field(raw, "老师报酬");

        String subject = parseSubject(subjectText);
        String grade = parseGrade(gradeGender);
        if (subject.isBlank() || grade.isBlank()) {
            return new ParsedDemand(code, null, false);
        }

        Demand demand = new Demand();
        demand.adminId = adminId;
        demand.parentName = code.isBlank() ? "导入订单" : "导入订单 " + code;
        demand.parentPhone = "待补充";
        demand.parentWechat = "";
        demand.address = address.isBlank() ? regionHeader : address;
        demand.region = parseRegion(regionHeader + " " + address);
        demand.subject = subject;
        demand.grade = grade;
        demand.teacherGender = parseGender(gradeGender);
        demand.basicScore = score;
        demand.salaryRange = salary.isBlank() ? "面议" : salary;
        SalaryRange range = parseSalary(salary);
        demand.salaryMin = range.min();
        demand.salaryMax = range.max();
        demand.qualificationTags = parseTags(teacherRequirement);
        demand.is985Required = containsAny(teacherRequirement, "985");
        demand.is211Required = containsAny(teacherRequirement, "211");
        demand.isKeyUniversityRequired = containsAny(teacherRequirement, "重本", "重点本科");
        Geo geo = geo(demand.region);
        demand.longitude = geo.longitude();
        demand.latitude = geo.latitude();
        demand.remark = remark(code, subjectText, schedule, teacherRequirement, raw);
        return new ParsedDemand(code, demand, true);
    }

    private boolean alreadyImported(String code) {
        String marker = "订单号：" + code;
        return repository.allDemands().stream()
                .anyMatch(demand -> demand.remark != null && demand.remark.contains(marker));
    }

    private String field(String raw, String label) {
        for (String line : raw.split("\\R")) {
            String text = line.trim();
            if (text.startsWith(label + "：")) {
                return text.substring((label + "：").length()).trim();
            }
            if (text.startsWith(label + ":")) {
                return text.substring((label + ":").length()).trim();
            }
        }
        return "";
    }

    private String stripCityMarker(String value) {
        return value.replace("深圳#", "深圳").replace("广州#", "广州").trim();
    }

    private String parseRegion(String text) {
        for (String region : REGIONS) {
            if (text.contains(region) || text.contains(region.replace("区", ""))) {
                return region;
            }
        }
        return text.isBlank() ? "未分区" : text.replace("小初高", "").trim();
    }

    private String parseGrade(String text) {
        Map<String, String> grades = new LinkedHashMap<>();
        grades.put("高三", "高三");
        grades.put("高二", "高二");
        grades.put("高一", "高一");
        grades.put("初三", "初三");
        grades.put("初二", "初二");
        grades.put("初一", "初一");
        grades.put("六年级", "小学六年级");
        grades.put("五年级", "小学五年级");
        grades.put("四年级", "小学四年级");
        grades.put("三年级", "小学三年级");
        grades.put("二年级", "小学二年级");
        grades.put("一年级", "小学一年级");
        for (Map.Entry<String, String> entry : grades.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "";
    }

    private int parseGender(String text) {
        if (text.contains("男")) {
            return 1;
        }
        if (text.contains("女")) {
            return 2;
        }
        return 3;
    }

    private String parseSubject(String text) {
        List<SubjectToken> tokens = List.of(
                new SubjectToken("语文", "语文"),
                new SubjectToken("数学", "数学"),
                new SubjectToken("英语", "英语"),
                new SubjectToken("物理", "物理"),
                new SubjectToken("化学", "化学"),
                new SubjectToken("生物", "生物"),
                new SubjectToken("历史", "历史"),
                new SubjectToken("地理", "地理"),
                new SubjectToken("政治", "政治"),
                new SubjectToken("奥数", "奥数"),
                new SubjectToken("编程", "编程"),
                new SubjectToken("钢琴", "钢琴"),
                new SubjectToken("美术", "美术"),
                new SubjectToken("体育", "体育"),
                new SubjectToken("语", "语文"),
                new SubjectToken("数", "数学"),
                new SubjectToken("英", "英语"),
                new SubjectToken("物", "物理"),
                new SubjectToken("化", "化学")
        );
        int best = Integer.MAX_VALUE;
        String subject = "";
        for (SubjectToken token : tokens) {
            int index = text.indexOf(token.token());
            if (index >= 0 && index < best) {
                best = index;
                subject = token.subject();
            }
        }
        if (subject.isBlank() && text.contains("文科")) {
            return "语文";
        }
        if (subject.isBlank() && text.contains("理科")) {
            return "数学";
        }
        return subject;
    }

    private SalaryRange parseSalary(String text) {
        if (text.isBlank()) {
            return new SalaryRange(0, 0);
        }
        String[] parts = text.split("/", 2);
        List<Double> prices = numbers(parts[0]);
        if (prices.isEmpty()) {
            return new SalaryRange(0, 0);
        }
        double hours = 1.0;
        if (parts.length > 1) {
            List<Double> hourNumbers = numbers(parts[1]);
            if (!hourNumbers.isEmpty() && hourNumbers.get(0) > 0) {
                hours = hourNumbers.get(0);
            }
        }
        int min = (int) Math.round(prices.get(0) / hours);
        int max = (int) Math.round((prices.size() > 1 ? prices.get(1) : prices.get(0)) / hours);
        return new SalaryRange(Math.min(min, max), Math.max(min, max));
    }

    private List<Double> numbers(String text) {
        List<Double> numbers = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            numbers.add(Double.parseDouble(matcher.group(1)));
        }
        return numbers;
    }

    private List<String> parseTags(String text) {
        List<String> tags = new ArrayList<>();
        addTag(tags, text, "985", "985");
        addTag(tags, text, "211", "211");
        addTag(tags, text, "重本", "重本");
        addTag(tags, text, "重点本科", "重本");
        addTag(tags, text, "师范", "师范类");
        addTag(tags, text, "教师资格", "有教师资格证");
        addTag(tags, text, "竞赛", "有竞赛经验");
        addTag(tags, text, "经验丰富", "经验丰富");
        addTag(tags, text, "女大", "女大学生优先");
        addTag(tags, text, "男大", "男大学生优先");
        return tags;
    }

    private void addTag(List<String> tags, String text, String keyword, String tag) {
        if (text.contains(keyword) && !tags.contains(tag)) {
            tags.add(tag);
        }
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private Geo geo(String region) {
        return switch (region) {
            case "南山区" -> new Geo(113.9304, 22.5333);
            case "宝安区" -> new Geo(113.8831, 22.5553);
            case "龙岗区" -> new Geo(114.2469, 22.7209);
            case "罗湖区" -> new Geo(114.1312, 22.5485);
            case "龙华区" -> new Geo(114.0443, 22.6917);
            case "盐田区" -> new Geo(114.2368, 22.5570);
            case "光明区" -> new Geo(113.9361, 22.7488);
            case "观澜区" -> new Geo(114.0596, 22.7195);
            case "福田区" -> new Geo(114.0550, 22.5215);
            default -> new Geo(113.2644, 23.1291);
        };
    }

    private String remark(String code, String subjectText, String schedule, String requirement, String raw) {
        List<String> lines = new ArrayList<>();
        if (!code.isBlank()) {
            lines.add("订单号：" + code);
        }
        if (!subjectText.isBlank()) {
            lines.add("原辅导科目：" + subjectText);
        }
        if (!schedule.isBlank()) {
            lines.add("时间安排：" + schedule);
        }
        if (!requirement.isBlank()) {
            lines.add("老师要求：" + requirement);
        }
        lines.add("原始内容：" + raw);
        return String.join("\n", lines);
    }

    private String firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String value(List<String> row, int index) {
        return index >= 0 && index < row.size() ? row.get(index) : "";
    }

    private record ParsedDemand(String code, Demand demand, boolean valid) {
    }

    private record SubjectToken(String token, String subject) {
    }

    private record SalaryRange(int min, int max) {
    }

    private record Geo(double longitude, double latitude) {
    }
}
