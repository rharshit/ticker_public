package com.ticker.brokerage.controller;

import com.ticker.brokerage.model.ResponseStatus;
import com.ticker.brokerage.service.BrokerageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/brokerage/")
public class BrokerageController {

    @Autowired
    private BrokerageService service;

    @GetMapping("/zerodha/{type}")
    public ResponseStatus getZerodhaBrokerage(@PathVariable String type) {
        service.getZerodhaBrokerage(type);
        return new ResponseStatus();
    }
}
