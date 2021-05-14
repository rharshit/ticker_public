package com.ticker.brokerage.controller;

import com.ticker.brokerage.service.BrokerageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/brokerage/")
public class BrokerageController {

    @Autowired
    private BrokerageService service;

    @GetMapping("/zerodha/{type}/{exchange}")
    public Map<String, Double> getZerodhaBrokerage(@PathVariable String type, @PathVariable String exchange) {
        return service.getZerodhaBrokerage(type, exchange, 0);
    }
}
