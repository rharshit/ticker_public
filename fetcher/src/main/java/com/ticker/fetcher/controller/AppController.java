package com.ticker.fetcher.controller;

import com.ticker.fetcher.model.FetcherThreadModel;
import com.ticker.fetcher.model.ResponseStatus;
import com.ticker.fetcher.service.TickerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
public class AppController {

    @Autowired
    private TickerService service;

    /**
     * Get a map of all the tickers currently being tracked
     *
     * @return
     */
    @GetMapping
    public ResponseEntity<Map<String, List<FetcherThreadModel>>> getCurrentTickers() {
        return new ResponseEntity<>(service.getCurrentTickers(), HttpStatus.OK);
    }

    /**
     * Add tracking for the ticker, given exchange and symbol for app
     *
     * @param exchange
     * @param symbol
     * @param appName
     * @return
     */
    @PostMapping
    public ResponseEntity<ResponseStatus> addTicker(@RequestParam String exchange, @RequestParam String symbol,
                                                    @RequestParam String appName) {
        service.addTicker(exchange, symbol, appName);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    @PutMapping("refresh")
    public ResponseEntity<ResponseStatus> refresh(@RequestParam String exchange, @RequestParam String symbol) {
        service.refreshBrowser(exchange, symbol);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    /**
     * Remove tracking for the ticker, given exchange and symbol for app
     *
     * @param exchange
     * @param symbol
     * @param appName
     * @return
     */
    @DeleteMapping
    public ResponseEntity<ResponseStatus> deleteTicker(@RequestParam String exchange, @RequestParam String symbol,
                                                       @RequestParam String appName) {
        service.deleteTicker(exchange, symbol, appName);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    /**
     * Remove tracking for the ticker, given exchange and symbol for all apps
     *
     * @param exchange
     * @param symbol
     * @return
     */
    @DeleteMapping("/ticker/")
    public ResponseEntity<ResponseStatus> deleteTicker(@RequestParam String exchange, @RequestParam String symbol) {
        service.deleteTicker(exchange, symbol);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    /**
     * Delete all tickers
     *
     * @return
     */
    @DeleteMapping(path = "/all/")
    public ResponseEntity<ResponseStatus> deleteAllTickers() {
        service.deleteAllTickers();
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }
}
