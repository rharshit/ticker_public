package com.ticker.brokerage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class BrokerageService {

    public void getZerodhaBrokerage(String type) {
        log.info(type);
    }
}
