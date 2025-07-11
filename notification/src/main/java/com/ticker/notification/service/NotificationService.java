package com.ticker.notification.service;

import com.ticker.common.entity.notification.NotificationEntity;
import com.ticker.common.entity.notification.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The type Notification service.
 */
@Service
public class NotificationService {

    @Autowired
    private NotificationRepository repository;

    /**
     * Gets notifications.
     *
     * @return the notifications
     */
    public Iterable<NotificationEntity> getNotifications() {
        return repository.findAll();
    }

    /**
     * Add update notification notification entity.
     *
     * @param notification the notification
     * @return the notification entity
     */
    public NotificationEntity addUpdateNotification(NotificationEntity notification) {
        return repository.save(notification);
    }
}
