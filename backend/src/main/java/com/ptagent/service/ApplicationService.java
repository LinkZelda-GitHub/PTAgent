package com.ptagent.service;

import com.ptagent.common.Json;
import com.ptagent.domain.ApplicationStatus;
import com.ptagent.domain.CourseOrder;
import com.ptagent.domain.Demand;
import com.ptagent.domain.DemandApplication;
import com.ptagent.domain.DemandStatus;
import com.ptagent.domain.TeacherProfile;
import com.ptagent.exception.ApiException;
import com.ptagent.exception.ErrorCode;
import com.ptagent.repository.Repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ApplicationService {
    private final Repository repository;
    private final AccessGuard accessGuard;

    public ApplicationService(Repository repository) {
        this.repository = repository;
        this.accessGuard = new AccessGuard(repository);
    }

    public List<Map<String, Object>> listApplications(String status) {
        return repository.allApplications().stream()
                .filter(application -> status == null || status.isBlank() || status.equalsIgnoreCase("ALL")
                        || application.status.name().equalsIgnoreCase(status))
                .sorted(Comparator.comparing((DemandApplication item) -> item.applyTime).reversed())
                .map(this::toMap)
                .toList();
    }

    public Map<String, Object> apply(long demandId, Map<String, Object> body) {
        long teacherId = Json.longValue(body, "teacherId", 0);
        accessGuard.requireTeacher(teacherId);
        String selfIntro = Json.str(body, "selfIntro");
        if (teacherId == 0) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "教师ID不能为空");
        }
        if (selfIntro.isBlank()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "自荐说明不能为空");
        }
        Demand demand = repository.findDemand(demandId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.DEMAND_NOT_FOUND, "需求不存在"));
        if (demand.status != DemandStatus.OPEN) {
            throw ApiException.badRequest(ErrorCode.DEMAND_INVALID_STATUS, "当前需求不可申请");
        }
        repository.findProfile(teacherId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.TEACHER_PROFILE_NOT_FOUND, "教师资料不存在"));
        if (repository.findApplicationByDemandAndTeacher(demandId, teacherId).isPresent()) {
            throw ApiException.badRequest(ErrorCode.APPLICATION_DUPLICATED, "你已申请过该需求");
        }
        DemandApplication application = repository.createApplication(demandId, teacherId, selfIntro);
        repository.createAuditLog(teacherId, "APPLICATION_CREATE", "DEMAND", demandId,
                "applicationId=" + application.id);
        repository.createNotification(teacherId, "申请已提交", demand.grade + demand.subject + " 需求已进入管理员审核。");
        return toMap(application);
    }

    public Map<String, Object> review(long applicationId, Map<String, Object> body) {
        DemandApplication application = repository.findApplication(applicationId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.APPLICATION_NOT_FOUND, "申请不存在"));
        String status = Json.str(body, "status");
        ApplicationStatus next;
        if ("APPROVED".equalsIgnoreCase(status)) {
            next = ApplicationStatus.APPROVED;
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            next = ApplicationStatus.REJECTED;
        } else {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "审核状态不正确");
        }
        long adminId = Json.longValue(body, "adminId", 0);
        accessGuard.requireSuperAdmin(adminId);
        application.status = next;
        application.confirmAdminId = adminId;
        application.confirmTime = LocalDateTime.now();
        repository.saveApplication(application);
        repository.createAuditLog(adminId, "APPLICATION_REVIEW", "APPLICATION", application.id,
                next.name());

        Demand demand = repository.findDemand(application.demandId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.DEMAND_NOT_FOUND, "需求不存在"));
        TeacherProfile teacher = repository.findProfile(application.teacherId).orElse(null);
        if (next == ApplicationStatus.APPROVED) {
            demand.status = DemandStatus.TEACHING;
            repository.saveDemand(demand);
            CourseOrder order = repository.createOrder(demand.id, application.teacherId, adminId);
            repository.createNotification(application.teacherId, "匹配成功",
                    demand.grade + demand.subject + " 已确认接单，请查看课程订单和家长联系方式。");
            Map<String, Object> result = toMap(application);
            result.put("order", order.toMap(demand, teacher));
            return result;
        }
        repository.createNotification(application.teacherId, "匹配未通过",
                demand.grade + demand.subject + " 暂未匹配成功，可继续关注其他需求。");
        return toMap(application);
    }

    private Map<String, Object> toMap(DemandApplication application) {
        Demand demand = repository.findDemand(application.demandId).orElse(null);
        TeacherProfile teacher = repository.findProfile(application.teacherId).orElse(null);
        return application.toMap(demand, teacher);
    }
}
