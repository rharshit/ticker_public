package com.ticker.brokerage.controller;

import com.ticker.brokerage.service.BrokerageService;
import com.ticker.common.controller.BaseController;
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
public class BrokerageController extends BaseController<BrokerageService> {

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
                                                   @RequestParam double buy,
                                                   @RequestParam double sell,
                                                   @RequestParam double quantity) {
        return service.getZerodhaBrokerageWrapper(type, exchange, buy, sell, quantity);
    }

    /**
     * Is busy response entity.
     *
     * @return the response entity
     */
    @GetMapping("/zerodha/busy")
    public ResponseEntity<Boolean> isBusy() {
        return new ResponseEntity<>(BrokerageService.isBusy(), HttpStatus.OK);
    }

    /**
     * Gets zerodha webdriver pool size.
     *
     * @return the zerodha webdriver pool size
     */
    @GetMapping("/zerodha/pool")
    public ResponseEntity<int[]> getZerodhaWebdriverPoolSize() {
        return new ResponseEntity<>(BrokerageService.getZerodhaWebdriverPoolSize(), HttpStatus.OK);
    }
}
