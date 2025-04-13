package com.ticker.common.entity.notification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * The type Notification entity.
 */
@Data
@Entity
@Table(name = "notifications")
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer notificationId;
    private String title;
    private String text;
    private Boolean readFlag;
    private Boolean deleteFlag;
    private Timestamp timestamp;
}
