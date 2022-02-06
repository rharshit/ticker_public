package com.ticker.notification.controller;

import com.ticker.common.entity.notification.NotificationEntity;
import com.ticker.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * The type Notification controller.
 */
@RestController
@RequestMapping("/")
public class NotificationController {

    @Autowired
    private NotificationService service;

    /**
     * Gets notifications.
     *
     * @return the notifications
     */
    @GetMapping
    public Iterable<NotificationEntity> getNotifications() {
        return service.getNotifications();
    }

    /**
     * Update notification notification entity.
     *
     * @param notification the notification
     * @return the notification entity
     */
    @PutMapping
    public NotificationEntity updateNotification(@RequestBody NotificationEntity notification) {
        return service.addUpdateNotification(notification);
    }

    /**
     * Add notification notification entity.
     *
     * @param notification the notification
     * @return the notification entity
     */
    @PostMapping
    public NotificationEntity addNotification(@RequestBody NotificationEntity notification) {
        return service.addUpdateNotification(notification);
    }
}
