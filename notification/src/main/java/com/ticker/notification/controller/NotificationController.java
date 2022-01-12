package com.ticker.notification.controller;

import com.ticker.common.entity.notification.NotificationEntity;
import com.ticker.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
public class NotificationController {

    @Autowired
    private NotificationService service;

    @GetMapping
    public Iterable<NotificationEntity> getNotifications() {
        return service.getNotifications();
    }

    @PutMapping
    public NotificationEntity updateNotification(@RequestBody NotificationEntity notification) {
        return service.addUpdateNotification(notification);
    }

    @PostMapping
    public NotificationEntity addNotification(@RequestBody NotificationEntity notification) {
        return service.addUpdateNotification(notification);
    }
}
