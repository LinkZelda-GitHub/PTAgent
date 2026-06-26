package com.ptagent.repository;

import com.ptagent.common.Passwords;
import com.ptagent.domain.ApplicationStatus;
import com.ptagent.domain.AuditLog;
import com.ptagent.domain.CourseOrder;
import com.ptagent.domain.Demand;
import com.ptagent.domain.DemandApplication;
import com.ptagent.domain.DemandStatus;
import com.ptagent.domain.Feedback;
import com.ptagent.domain.Notification;
import com.ptagent.domain.OrderStatus;
import com.ptagent.domain.RoleType;
import com.ptagent.domain.TeacherProfile;
import com.ptagent.domain.TeacherResume;
import com.ptagent.domain.TeachingRecord;
import com.ptagent.domain.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class AppRepository implements Repository {
    private final Map<Long, User> users = new LinkedHashMap<>();
    private final Map<Long, TeacherProfile> teacherProfiles = new LinkedHashMap<>();
    private final Map<Long, TeacherResume> teacherResumes = new LinkedHashMap<>();
    private final Map<Long, Demand> demands = new LinkedHashMap<>();
    private final Map<Long, DemandApplication> applications = new LinkedHashMap<>();
    private final Map<Long, CourseOrder> orders = new LinkedHashMap<>();
    private final Map<Long, TeachingRecord> teachingRecords = new LinkedHashMap<>();
    private final Map<Long, Feedback> feedbacks = new LinkedHashMap<>();
    private final Map<Long, Notification> notifications = new LinkedHashMap<>();
    private final Map<Long, AuditLog> auditLogs = new LinkedHashMap<>();

    private final AtomicLong userIds = new AtomicLong(100);
    private final AtomicLong resumeIds = new AtomicLong(200);
    private final AtomicLong demandIds = new AtomicLong(300);
    private final AtomicLong applicationIds = new AtomicLong(400);
    private final AtomicLong orderIds = new AtomicLong(500);
    private final AtomicLong recordIds = new AtomicLong(600);
    private final AtomicLong feedbackIds = new AtomicLong(700);
    private final AtomicLong notificationIds = new AtomicLong(800);
    private final AtomicLong auditLogIds = new AtomicLong(900);

    public AppRepository() {
        seed();
    }

    public synchronized User createUser(String username, String password, RoleType role, String phone, String email, boolean enabled) {
        User user = new User();
        user.id = userIds.getAndIncrement();
        user.username = username;
        user.passwordHash = Passwords.sha256(password);
        user.role = role;
        user.phoneNumber = phone;
        user.email = email;
        user.enabled = enabled;
        user.registerTime = LocalDateTime.now();
        users.put(user.id, user);
        return user;
    }

    public synchronized Optional<User> findUser(long id) {
        return Optional.ofNullable(users.get(id));
    }

    public synchronized Optional<User> findUserByUsername(String username) {
        return users.values().stream()
                .filter(user -> user.username.equalsIgnoreCase(username))
                .findFirst();
    }

    public synchronized List<User> allUsers() {
        return new ArrayList<>(users.values());
    }

    public synchronized void saveUser(User user) {
        users.put(user.id, user);
    }

    public synchronized void saveProfile(TeacherProfile profile) {
        teacherProfiles.put(profile.teacherId, profile);
    }

    public synchronized Optional<TeacherProfile> findProfile(long teacherId) {
        return Optional.ofNullable(teacherProfiles.get(teacherId));
    }

    public synchronized List<TeacherProfile> allProfiles() {
        return new ArrayList<>(teacherProfiles.values());
    }

    public synchronized TeacherResume createResume(long teacherId, String fileUrl, String summary) {
        TeacherResume resume = new TeacherResume();
        resume.id = resumeIds.getAndIncrement();
        resume.teacherId = teacherId;
        resume.fileUrl = fileUrl;
        resume.summary = summary;
        teacherResumes.values().stream()
                .filter(item -> item.teacherId == teacherId)
                .forEach(item -> item.active = false);
        teacherResumes.put(resume.id, resume);
        return resume;
    }

    public synchronized Optional<TeacherResume> findResume(long resumeId) {
        return Optional.ofNullable(teacherResumes.get(resumeId));
    }

    public synchronized List<TeacherResume> allResumes() {
        return new ArrayList<>(teacherResumes.values());
    }

    public synchronized Demand createDemand(Demand demand) {
        demand.id = demandIds.getAndIncrement();
        demand.createTime = LocalDateTime.now();
        demands.put(demand.id, demand);
        return demand;
    }

    public synchronized Optional<Demand> findDemand(long id) {
        return Optional.ofNullable(demands.get(id));
    }

    public synchronized List<Demand> allDemands() {
        return new ArrayList<>(demands.values());
    }

    public synchronized void saveDemand(Demand demand) {
        demands.put(demand.id, demand);
    }

    public synchronized Optional<DemandApplication> findApplication(long id) {
        return Optional.ofNullable(applications.get(id));
    }

    public synchronized Optional<DemandApplication> findApplicationByDemandAndTeacher(long demandId, long teacherId) {
        return applications.values().stream()
                .filter(application -> application.demandId == demandId && application.teacherId == teacherId)
                .findFirst();
    }

    public synchronized DemandApplication createApplication(long demandId, long teacherId, String selfIntro) {
        DemandApplication application = new DemandApplication();
        application.id = applicationIds.getAndIncrement();
        application.demandId = demandId;
        application.teacherId = teacherId;
        application.selfIntro = selfIntro;
        applications.put(application.id, application);
        return application;
    }

    public synchronized List<DemandApplication> allApplications() {
        return new ArrayList<>(applications.values());
    }

    public synchronized void saveApplication(DemandApplication application) {
        applications.put(application.id, application);
    }

    public synchronized CourseOrder createOrder(long demandId, long teacherId, long adminId) {
        CourseOrder order = new CourseOrder();
        order.id = orderIds.getAndIncrement();
        order.demandId = demandId;
        order.teacherId = teacherId;
        order.adminId = adminId;
        orders.put(order.id, order);
        return order;
    }

    public synchronized Optional<CourseOrder> findOrder(long id) {
        return Optional.ofNullable(orders.get(id));
    }

    public synchronized List<CourseOrder> allOrders() {
        return new ArrayList<>(orders.values());
    }

    public synchronized TeachingRecord createTeachingRecord(long orderId, LocalDate lessonDate, double duration, String content, String performance, String notes) {
        TeachingRecord record = new TeachingRecord();
        record.id = recordIds.getAndIncrement();
        record.orderId = orderId;
        record.lessonDate = lessonDate;
        record.lessonDuration = duration;
        record.content = content;
        record.studentPerformance = performance;
        record.teacherNotes = notes;
        teachingRecords.put(record.id, record);
        return record;
    }

    public synchronized List<TeachingRecord> allTeachingRecords() {
        return new ArrayList<>(teachingRecords.values());
    }

    public synchronized Feedback createFeedback(long orderId, int score, String comment, int source, long adminId) {
        Feedback feedback = new Feedback();
        feedback.id = feedbackIds.getAndIncrement();
        feedback.orderId = orderId;
        feedback.ratingScore = score;
        feedback.commentText = comment;
        feedback.feedbackSource = source;
        feedback.submitAdminId = adminId;
        feedbacks.put(feedback.id, feedback);
        recalculateTeacherRating(orderId);
        return feedback;
    }

    public synchronized List<Feedback> allFeedbacks() {
        return new ArrayList<>(feedbacks.values());
    }

    public synchronized Notification createNotification(long userId, String title, String message) {
        Notification notification = new Notification();
        notification.id = notificationIds.getAndIncrement();
        notification.userId = userId;
        notification.title = title;
        notification.message = message;
        notifications.put(notification.id, notification);
        return notification;
    }

    public synchronized List<Notification> notificationsFor(long userId) {
        return notifications.values().stream()
                .filter(notification -> notification.userId == userId)
                .sorted(Comparator.comparing((Notification n) -> n.createTime).reversed())
                .toList();
    }

    public synchronized AuditLog createAuditLog(long actorId, String action, String targetType, long targetId, String detail) {
        AuditLog auditLog = new AuditLog();
        auditLog.id = auditLogIds.getAndIncrement();
        auditLog.actorId = actorId;
        auditLog.actorRole = findUser(actorId).map(user -> user.role.name()).orElse("UNKNOWN");
        auditLog.action = action;
        auditLog.targetType = targetType;
        auditLog.targetId = targetId;
        auditLog.detail = detail;
        auditLogs.put(auditLog.id, auditLog);
        return auditLog;
    }

    public synchronized List<AuditLog> allAuditLogs() {
        return new ArrayList<>(auditLogs.values());
    }

    private void recalculateTeacherRating(long orderId) {
        CourseOrder order = orders.get(orderId);
        if (order == null) {
            return;
        }
        List<Feedback> teacherFeedbacks = feedbacks.values().stream()
                .filter(feedback -> {
                    CourseOrder feedbackOrder = orders.get(feedback.orderId);
                    return feedbackOrder != null && feedbackOrder.teacherId == order.teacherId;
                })
                .toList();
        if (teacherFeedbacks.isEmpty()) {
            return;
        }
        double average = teacherFeedbacks.stream().mapToInt(feedback -> feedback.ratingScore).average().orElse(5.0);
        TeacherProfile profile = teacherProfiles.get(order.teacherId);
        if (profile != null) {
            profile.avgRating = Math.round(average * 10.0) / 10.0;
        }
    }

    private void seed() {
        User superAdmin = createUser("super", "admin123", RoleType.SUPER_ADMIN, "13800000001", "super@ptagent.local", true);
        User admin = createUser("admin", "admin123", RoleType.ADMIN, "13800000002", "admin@ptagent.local", true);
        User teacherLi = createUser("teacher", "teacher123", RoleType.TEACHER, "13800000003", "teacher@ptagent.local", true);
        User teacherWang = createUser("wang", "teacher123", RoleType.TEACHER, "13800000004", "wang@ptagent.local", true);

        TeacherProfile li = new TeacherProfile();
        li.teacherId = teacherLi.id;
        li.realName = "李明";
        li.gender = 1;
        li.education = "硕士";
        li.graduateSchool = "华南理工大学";
        li.is985 = true;
        li.is211 = true;
        li.subjects = List.of("数学", "物理", "奥数");
        li.teachingExperience = "8年中高考数学与竞赛辅导经验，擅长错题归因和阶段提分。";
        li.expectedRate = "240-320元/小时";
        li.availableTime = List.of("周一晚", "周三晚", "周末全天");
        li.serviceArea = List.of("天河区", "越秀区", "海珠区");
        li.personalIntro = "强调学习路径拆解和可执行练习计划。";
        li.contactPhone = "13800000003";
        li.contactWechat = "liming-tutor";
        li.hasTeacherCert = true;
        li.competitionExperience = true;
        saveProfile(li);

        TeacherProfile wang = new TeacherProfile();
        wang.teacherId = teacherWang.id;
        wang.realName = "王芳";
        wang.gender = 2;
        wang.education = "本科";
        wang.graduateSchool = "华南师范大学";
        wang.is211 = true;
        wang.isKeyUniversity = true;
        wang.subjects = List.of("英语", "语文", "历史");
        wang.teachingExperience = "6年一对一英语阅读与写作辅导经验，熟悉小升初和初中英语体系。";
        wang.expectedRate = "180-260元/小时";
        wang.availableTime = List.of("周二晚", "周四晚", "周六下午");
        wang.serviceArea = List.of("越秀区", "荔湾区", "海珠区");
        wang.personalIntro = "善于用结构化阅读和口语表达训练提升学生自信。";
        wang.contactPhone = "13800000004";
        wang.contactWechat = "wangfang-edu";
        wang.hasTeacherCert = true;
        wang.normalUniversity = true;
        saveProfile(wang);

        createResume(teacherLi.id, "/mock-resumes/li-ming.pdf", "数学/物理方向，含竞赛获奖和中高考提分案例。").status = 1;
        createResume(teacherWang.id, "/mock-resumes/wang-fang.docx", "英语阅读写作方向，含小升初与初中阶段教学案例。");

        Demand math = demand(admin.id, "陈女士", "13900001001", "chen-parent", "天河区体育西路", "天河区",
                113.3212, 23.1317, "数学", "高一", 3, "70/100分，函数基础薄弱", "240-320元/小时", 240, 320,
                "每周两次，优先周三晚和周末。", List.of("985", "有竞赛经验"), true, false, false);
        math.createTime = LocalDateTime.now().minusHours(2);
        demands.put(math.id, math);

        Demand english = demand(admin.id, "刘先生", "13900001002", "liu-parent", "越秀区东风东路", "越秀区",
                113.2893, 23.1290, "英语", "初二", 2, "阅读理解待提升", "180-240元/小时", 180, 240,
                "希望加强阅读和作文，女老师优先。", List.of("师范类", "有教师资格证"), false, true, false);
        english.createTime = LocalDateTime.now().minusHours(5);
        demands.put(english.id, english);

        Demand physics = demand(admin.id, "周女士", "13900001003", "zhou-parent", "海珠区滨江东路", "海珠区",
                113.3048, 23.1058, "物理", "初三", 1, "中等，力学题型不稳定", "220-280元/小时", 220, 280,
                "冲刺中考，要求有理科提分经验。", List.of("重本", "有教师资格证"), false, false, true);
        physics.createTime = LocalDateTime.now().minusDays(1);
        demands.put(physics.id, physics);

        Demand piano = demand(superAdmin.id, "许先生", "13900001004", "xu-parent", "荔湾区沙面", "荔湾区",
                113.2405, 23.1101, "钢琴", "小学四年级", 3, "零基础", "160-220元/小时", 160, 220,
                "兴趣启蒙，周末上门。", List.of(), false, false, false);
        piano.createTime = LocalDateTime.now().minusDays(2);
        demands.put(piano.id, piano);

        Demand chinese = demand(admin.id, "黄女士", "13900001005", "huang-parent", "番禺区大学城", "番禺区",
                113.3972, 23.0587, "语文", "小学六年级", 3, "作文缺少素材和结构", "150-220元/小时", 150, 220,
                "小升初作文和阅读专项。", List.of("师范类"), false, false, false);
        chinese.createTime = LocalDateTime.now().minusHours(9);
        demands.put(chinese.id, chinese);

        DemandApplication application = createApplication(english.id, teacherWang.id, "我熟悉初中英语阅读与作文训练，可按周反馈学习进度。");
        application.status = ApplicationStatus.APPROVED;
        application.confirmAdminId = superAdmin.id;
        application.confirmTime = LocalDateTime.now().minusHours(1);
        english.status = DemandStatus.TEACHING;
        CourseOrder order = createOrder(english.id, teacherWang.id, superAdmin.id);
        order.status = OrderStatus.TEACHING;
        createTeachingRecord(order.id, LocalDate.now().minusDays(1), 2.0, "完成完形填空策略讲解和两篇阅读精读。", "能主动复述文章结构，长难句仍需练习。", "下节课安排作文提纲训练。");
        createFeedback(order.id, 5, "老师沟通清楚，孩子愿意配合练习。", 1, admin.id);
        createNotification(teacherWang.id, "匹配成功", "初二英语需求已确认，请联系家长安排首次授课。");
    }

    private Demand demand(long adminId, String parentName, String phone, String wechat, String address, String region,
                          double longitude, double latitude, String subject, String grade, int gender, String score,
                          String salaryRange, int salaryMin, int salaryMax, String remark, List<String> tags,
                          boolean need985, boolean need211, boolean needKey) {
        Demand demand = new Demand();
        demand.id = demandIds.getAndIncrement();
        demand.adminId = adminId;
        demand.parentName = parentName;
        demand.parentPhone = phone;
        demand.parentWechat = wechat;
        demand.address = address;
        demand.region = region;
        demand.longitude = longitude;
        demand.latitude = latitude;
        demand.subject = subject;
        demand.grade = grade;
        demand.teacherGender = gender;
        demand.basicScore = score;
        demand.salaryRange = salaryRange;
        demand.salaryMin = salaryMin;
        demand.salaryMax = salaryMax;
        demand.remark = remark;
        demand.qualificationTags = new ArrayList<>(tags);
        demand.is985Required = need985;
        demand.is211Required = need211;
        demand.isKeyUniversityRequired = needKey;
        demand.status = DemandStatus.OPEN;
        demand.createTime = LocalDateTime.now();
        return demand;
    }
}
