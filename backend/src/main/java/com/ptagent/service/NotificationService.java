package com.ptagent.service;

import com.ptagent.repository.Repository;

import java.util.List;
import java.util.Map;

public class NotificationService {
    private final Repository repository;

    public NotificationService(Repository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> list(long userId) {
        return repository.notificationsFor(userId).stream().map(notification -> notification.toMap()).toList();
    }
}
