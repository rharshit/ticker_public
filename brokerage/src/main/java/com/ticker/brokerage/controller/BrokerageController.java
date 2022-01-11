package com.ticker.brokerage.controller;

import com.ticker.brokerage.service.BrokerageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/")
public class BrokerageController {

    @Autowired
    private BrokerageService service;

    @GetMapping("/zerodha/{type}/{exchange}")
    public Map<String, Double> getZerodhaBrokerage(@PathVariable String type,
                                                   @PathVariable String exchange,
                                                   @RequestParam float buy,
                                                   @RequestParam float sell,
                                                   @RequestParam float quantity) {
        return service.getZerodhaBrokerage(type, exchange, buy, sell, quantity, 0);
    }

    @GetMapping("/zerodha/busy")
    public ResponseEntity<Boolean> isBusy() {
        return new ResponseEntity<>(service.isBusy(), HttpStatus.OK);
    }
}
