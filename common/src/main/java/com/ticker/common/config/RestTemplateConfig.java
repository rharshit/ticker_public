package com.ticker.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

import java.net.URI;
import java.util.Map;

@Slf4j
@Configuration
public class RestTemplateConfig extends RestTemplate {
    @Bean(name = "restTemplate")
    public RestTemplate getRestTemplate() {
        RestTemplate restTemplate = new RestTemplate() {
            @Override
            protected <T> T doExecute(URI url, HttpMethod method, RequestCallback requestCallback, ResponseExtractor<T> responseExtractor) throws RestClientException {
                long start = System.currentTimeMillis();
                T returnValue = super.doExecute(url, method, requestCallback, responseExtractor);
                long end = System.currentTimeMillis();
                log.debug("URI:  " + method + ":" + url);
                log.debug("Time: " + (end - start) + "ms");
                return returnValue;
            }
        };
        restTemplate.setUriTemplateHandler(new UriTemplateHandler() {
            @Override
            public URI expand(String s, Map<String, ?> map) {
                StringBuilder requestParams = new StringBuilder();
                for (Map.Entry<String, ?> param : map.entrySet()) {
                    requestParams.append(requestParams.length() == 0 ? "?" : "&")
                            .append(param.getKey())
                            .append("=")
                            .append(param.getValue() == null ? "" : param.getValue().toString());
                }
                return URI.create(s + requestParams.toString());
            }

            @Override
            public URI expand(String s, Object... objects) {
                StringBuilder url = new StringBuilder(s);
                for (Object object : objects) {
                    url.append("/").append(object == null ? "null" : object.toString());
                }
                return URI.create(url.toString());
            }


        });
        return restTemplate;
    }
}
