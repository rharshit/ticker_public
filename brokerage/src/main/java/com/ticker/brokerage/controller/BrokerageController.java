package com.ticker.brokerage.controller;

import com.ticker.brokerage.service.BrokerageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * The type Brokerage controller.
 */
@RestController
@RequestMapping("/")
public class BrokerageController {

    @Autowired
    private BrokerageService service;

    /**
     * Gets zerodha brokerage.
     *
     * @param type     the type
     * @param exchange the exchange
     * @param buy      the buy
     * @param sell     the sell
     * @param quantity the quantity
     * @return the zerodha brokerage
     */
    @GetMapping("/zerodha/{type}/{exchange}")
    public Map<String, Double> getZerodhaBrokerage(@PathVariable String type,
                                                   @PathVariable String exchange,
                                                   @RequestParam float buy,
                                                   @RequestParam float sell,
                                                   @RequestParam float quantity) {
        return service.getZerodhaBrokerage(type, exchange, buy, sell, quantity, 0);
    }

    /**
     * Is busy response entity.
     *
     * @return the response entity
     */
    @GetMapping("/zerodha/busy")
    public ResponseEntity<Boolean> isBusy() {
        return new ResponseEntity<>(service.isBusy(), HttpStatus.OK);
    }

    /**
     * Gets zerodha webdriver pool size.
     *
     * @return the zerodha webdriver pool size
     */
    @GetMapping("/zerodha/pool")
    public ResponseEntity<int[]> getZerodhaWebdriverPoolSize() {
        return new ResponseEntity<>(service.getZerodhaWebdriverPoolSize(), HttpStatus.OK);
    }
}
