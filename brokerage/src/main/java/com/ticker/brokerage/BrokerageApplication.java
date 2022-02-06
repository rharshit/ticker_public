package com.ticker.brokerage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;

/**
 * The type Brokerage application.
 */
@EnableCaching
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class BrokerageApplication {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(BrokerageApplication.class, args);
    }

}
