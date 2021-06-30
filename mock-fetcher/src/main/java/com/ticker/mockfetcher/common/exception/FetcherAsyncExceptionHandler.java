package com.ticker.mockfetcher.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

@Slf4j
public class FetcherAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(
            Throwable throwable, Method method, Object... obj) {

        log.error("Error in fetcher");
        log.error(throwable.getMessage());
    }

}
