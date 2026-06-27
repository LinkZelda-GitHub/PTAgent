package com.ptagent.repository;

import com.ptagent.domain.CourseOrder;
import com.ptagent.domain.AuditLog;
import com.ptagent.domain.Demand;
import com.ptagent.domain.DemandApplication;
import com.ptagent.domain.Feedback;
import com.ptagent.domain.Notification;
import com.ptagent.domain.RoleType;
import com.ptagent.domain.TeacherProfile;
import com.ptagent.domain.TeacherResume;
import com.ptagent.domain.TeachingRecord;
import com.ptagent.domain.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface Repository {
    default String storageType() {
        return "memory";
    }

    default String storageLocation() {
        return "";
    }

    User createUser(String username, String password, RoleType role, String phone, String email, boolean enabled);

    Optional<User> findUser(long id);

    Optional<User> findUserByUsername(String username);

    List<User> allUsers();

    void saveUser(User user);

    void saveProfile(TeacherProfile profile);

    Optional<TeacherProfile> findProfile(long teacherId);

    List<TeacherProfile> allProfiles();

    TeacherResume createResume(long teacherId, String fileUrl, String summary);

    Optional<TeacherResume> findResume(long resumeId);

    List<TeacherResume> allResumes();

    Demand createDemand(Demand demand);

    Optional<Demand> findDemand(long id);

    List<Demand> allDemands();

    void saveDemand(Demand demand);

    Optional<DemandApplication> findApplication(long id);

    Optional<DemandApplication> findApplicationByDemandAndTeacher(long demandId, long teacherId);

    DemandApplication createApplication(long demandId, long teacherId, String selfIntro);

    List<DemandApplication> allApplications();

    void saveApplication(DemandApplication application);

    CourseOrder createOrder(long demandId, long teacherId, long adminId);

    Optional<CourseOrder> findOrder(long id);

    List<CourseOrder> allOrders();

    TeachingRecord createTeachingRecord(long orderId, LocalDate lessonDate, double duration, String content,
                                        String performance, String notes);

    List<TeachingRecord> allTeachingRecords();

    Feedback createFeedback(long orderId, int score, String comment, int source, long adminId);

    List<Feedback> allFeedbacks();

    Notification createNotification(long userId, String title, String message);

    List<Notification> notificationsFor(long userId);

    AuditLog createAuditLog(long actorId, String action, String targetType, long targetId, String detail);

    List<AuditLog> allAuditLogs();
}
