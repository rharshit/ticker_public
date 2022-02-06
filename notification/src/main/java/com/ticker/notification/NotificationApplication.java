package com.ticker.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@ComponentScan({"com.ticker.notification"})
@EnableJpaRepositories(basePackages = {"com.ticker.common.entity.notification"})
@EntityScan(basePackages = {"com.ticker.common.entity.notification"})
@ComponentScan({"com.ticker.common.entity.notification", "com.ticker.notification"})
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }

}
