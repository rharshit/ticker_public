package com.ticker.common.controller;

import com.ticker.common.model.ResponseStatus;
import com.ticker.common.model.StratThreadModel;
import com.ticker.common.service.StratTickerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * The type Strat controller.
 *
 * @param <S>  the type parameter
 * @param <TM> the type parameter
 */
public abstract class StratController<S extends StratTickerService, TM extends StratThreadModel> {

    @Autowired
    private S service;

    /**
     * Gets thread comp name.
     *
     * @return the thread comp name
     */
    public abstract String getThreadCompName();

    /**
     * Gets current tickers.
     *
     * @return the current tickers
     */
    @GetMapping
    public ResponseEntity<Map<String, List<TM>>> getCurrentTickers() {
        return new ResponseEntity<>(service.getCurrentTickers(), HttpStatus.OK);
    }

    /**
     * Gets state value map.
     *
     * @return the state value map
     */
    @GetMapping("state-value")
    public ResponseEntity<Map<Integer, String>> getStateValueMap() {
        return new ResponseEntity<>(service.getStateValueMap(), HttpStatus.OK);
    }

    /**
     * Add ticker response entity.
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     * @return the response entity
     */
    @PostMapping
    public ResponseEntity<ResponseStatus> addTicker(@RequestParam String exchange, @RequestParam String symbol) {
        service.createThread(exchange, symbol, new String[]{getThreadCompName()});
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    /**
     * Refresh response entity.
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     * @return the response entity
     */
    @PutMapping("refresh")
    public ResponseEntity<ResponseStatus> refresh(@RequestParam String exchange, @RequestParam String symbol) {
        service.refreshBrowser(exchange, symbol);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    /**
     * Delete ticker response entity.
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     * @return the response entity
     */
    @DeleteMapping
    public ResponseEntity<ResponseStatus> deleteTicker(@RequestParam String exchange, @RequestParam String symbol) {
        service.stopFetching(exchange, symbol);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    /**
     * Delete all tickers response entity.
     *
     * @return the response entity
     */
    @DeleteMapping("all/")
    public ResponseEntity<ResponseStatus> deleteAllTickers() {
        service.stopFetchingAll();
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }
}