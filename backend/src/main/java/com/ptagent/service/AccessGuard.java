package com.ptagent.service;

import com.ptagent.domain.RoleType;
import com.ptagent.domain.User;
import com.ptagent.exception.ApiException;
import com.ptagent.exception.ErrorCode;
import com.ptagent.repository.Repository;

import java.util.Arrays;

public class AccessGuard {
    private final Repository repository;

    public AccessGuard(Repository repository) {
        this.repository = repository;
    }

    public User requireUser(long userId) {
        if (userId == 0) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "actorId is required");
        }
        User user = repository.findUser(userId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.USER_NOT_FOUND, "actor not found"));
        if (!user.enabled) {
            throw new ApiException(403, ErrorCode.AUTH_DISABLED, "actor is disabled");
        }
        return user;
    }

    public User requireTeacher(long teacherId) {
        return requireRole(teacherId, RoleType.TEACHER);
    }

    public User requireAdmin(long adminId) {
        return requireRole(adminId, RoleType.ADMIN, RoleType.SUPER_ADMIN);
    }

    public User requireSuperAdmin(long adminId) {
        return requireRole(adminId, RoleType.SUPER_ADMIN);
    }

    public User requireSelfOrAdmin(long actorId, long ownerId) {
        User actor = requireUser(actorId);
        if (actor.id == ownerId || actor.role == RoleType.ADMIN || actor.role == RoleType.SUPER_ADMIN) {
            return actor;
        }
        throw new ApiException(403, ErrorCode.ACCESS_DENIED, "permission denied");
    }

    private User requireRole(long userId, RoleType... roles) {
        User user = requireUser(userId);
        boolean allowed = Arrays.stream(roles).anyMatch(role -> role == user.role);
        if (!allowed) {
            throw new ApiException(403, ErrorCode.ACCESS_DENIED, "permission denied");
        }
        return user;
    }
}
