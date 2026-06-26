package com.ptagent.service;

import com.ptagent.common.Json;
import com.ptagent.domain.ApplicationStatus;
import com.ptagent.domain.DemandStatus;
import com.ptagent.domain.RoleType;
import com.ptagent.repository.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardService {
    private final Repository repository;

    public DashboardService(Repository repository) {
        this.repository = repository;
    }

    public Map<String, Object> dashboard() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("openDemands", repository.allDemands().stream().filter(demand -> demand.status == DemandStatus.OPEN).count());
        map.put("activeTeachers", repository.allUsers().stream().filter(user -> user.role == RoleType.TEACHER && user.enabled).count());
        map.put("pendingApplications", repository.allApplications().stream().filter(app -> app.status == ApplicationStatus.PENDING).count());
        map.put("courseOrders", repository.allOrders().size());
        map.put("subjects", subjects());
        map.put("grades", grades());
        map.put("regions", regions());
        map.put("tags", tags());
        map.put("demoAccounts", List.of(
                Json.object("username", "super", "password", "admin123", "role", "最高管理员"),
                Json.object("username", "admin", "password", "admin123", "role", "普通管理员"),
                Json.object("username", "teacher", "password", "teacher123", "role", "教师")
        ));
        return map;
    }

    public List<String> subjects() {
        List<String> subjects = new java.util.ArrayList<>(List.of("语文", "数学", "英语", "物理", "化学", "生物", "历史",
                "地理", "政治", "奥数", "编程", "钢琴", "美术", "体育"));
        repository.allDemands().stream()
                .map(demand -> demand.subject)
                .filter(subject -> subject != null && !subject.isBlank() && !subjects.contains(subject))
                .forEach(subjects::add);
        return subjects;
    }

    public List<String> grades() {
        return List.of("小学一年级", "小学二年级", "小学三年级", "小学四年级", "小学五年级", "小学六年级",
                "初一", "初二", "初三", "高一", "高二", "高三");
    }

    public List<String> regions() {
        List<String> regions = new java.util.ArrayList<>(List.of("天河区", "越秀区", "海珠区", "荔湾区", "番禺区",
                "南山区", "宝安区", "龙岗区", "罗湖区", "龙华区", "盐田区", "光明区", "观澜区", "福田区"));
        repository.allDemands().stream()
                .map(demand -> demand.region)
                .filter(region -> region != null && !region.isBlank() && !regions.contains(region))
                .forEach(regions::add);
        return regions;
    }

    public List<String> tags() {
        List<String> tags = new java.util.ArrayList<>(List.of("985", "211", "重本", "师范类", "有竞赛经验", "有教师资格证",
                "经验丰富", "女大学生优先", "男大学生优先"));
        repository.allDemands().stream()
                .flatMap(demand -> demand.allTags().stream())
                .filter(tag -> tag != null && !tag.isBlank() && !tags.contains(tag))
                .forEach(tags::add);
        return tags;
    }
}
