package com.ticker.notification.service;

import com.ticker.common.entity.notification.NotificationEntity;
import com.ticker.common.entity.notification.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository repository;

    public Iterable<NotificationEntity> getNotifications() {
        return repository.findAll();
    }

    public NotificationEntity addUpdateNotification(NotificationEntity notification) {
        return repository.save(notification);
    }
}
