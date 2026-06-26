package com.ptagent.service;

import com.ptagent.repository.AppRepository;

import java.util.List;
import java.util.Map;

public class NotificationService {
    private final AppRepository repository;

    public NotificationService(AppRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> list(long userId) {
        return repository.notificationsFor(userId).stream().map(notification -> notification.toMap()).toList();
    }
}
