package com.ticker.bbrsi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The type Bb rsi application.
 */
@EnableScheduling
@EnableJpaRepositories(basePackages = {"com.ticker.common.entity.exchangesymbol"})
@EntityScan(basePackages = {"com.ticker.common.entity.exchangesymbol"})
@ComponentScan({
        "com.ticker.common.entity.exchangesymbol",
        "com.ticker.common.config",
        "com.ticker.bbrsi"
})
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class BbRsiApplication {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(BbRsiApplication.class, args);
    }

}
