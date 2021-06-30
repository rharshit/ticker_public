package com.ticker.mockfetcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaRepositories(basePackages = {"com.ticker.common.fetcher.repository.exchangesymbol"})
@EntityScan(basePackages = {"com.ticker.common.fetcher.repository.exchangesymbol"})
@ComponentScan({"com.ticker.common.fetcher.repository.exchangesymbol", "com.ticker.mockfetcher"})
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MockFetcherApplication {

	public static void main(String[] args) {
		SpringApplication.run(MockFetcherApplication.class, args);
	}

}
