package com.ticker.booker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * The type Booker application.
 */
@ComponentScan({"com.ticker.common.exception", "com.ticker.common.config", "com.ticker.booker"})
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class BookerApplication {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(BookerApplication.class, args);
    }

}
