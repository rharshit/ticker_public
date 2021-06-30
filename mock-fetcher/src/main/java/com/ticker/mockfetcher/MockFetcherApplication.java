package com.ticker.mockfetcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@ComponentScan({"com.ticker.mockfetcher"})
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MockFetcherApplication {

	public static void main(String[] args) {
		SpringApplication.run(MockFetcherApplication.class, args);
	}

}
