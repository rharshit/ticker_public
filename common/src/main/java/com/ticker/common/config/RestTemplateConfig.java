package com.ticker.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig extends RestTemplate {
    @Bean(name = "restTemplate")
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }
}
