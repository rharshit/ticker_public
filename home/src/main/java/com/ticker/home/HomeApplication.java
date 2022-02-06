package com.ticker.home;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;


/**
 * The type Home application.
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class HomeApplication {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(HomeApplication.class, args);
    }

}
