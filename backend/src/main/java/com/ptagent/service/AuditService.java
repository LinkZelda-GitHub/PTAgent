package com.ptagent.service;

import com.ptagent.domain.AuditLog;
import com.ptagent.repository.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class AuditService {
    private final Repository repository;
    private final AccessGuard accessGuard;

    public AuditService(Repository repository) {
        this.repository = repository;
        this.accessGuard = new AccessGuard(repository);
    }

    public List<Map<String, Object>> listAuditLogs(long actorId) {
        accessGuard.requireAdmin(actorId);
        return repository.allAuditLogs().stream()
                .sorted(Comparator.comparing((AuditLog log) -> log.createTime).reversed())
                .map(AuditLog::toMap)
                .toList();
    }
}
