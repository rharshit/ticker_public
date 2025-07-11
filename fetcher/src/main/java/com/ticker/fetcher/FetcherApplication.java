package com.ticker.fetcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The type Fetcher application.
 */
@EnableScheduling
@EnableJpaRepositories(basePackages = {"com.ticker.common.entity.exchangesymbol"})
@EntityScan(basePackages = {"com.ticker.common.entity.exchangesymbol"})
@ComponentScan({"com.ticker.common.entity.exchangesymbol", "com.ticker.fetcher"})
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class FetcherApplication {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(FetcherApplication.class, args);
    }

}
