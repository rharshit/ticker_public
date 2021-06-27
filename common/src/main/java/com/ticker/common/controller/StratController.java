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

public abstract class StratController<S extends StratTickerService, TM extends StratThreadModel> {

    @Autowired
    private S service;

    public abstract String getThreadCompName();

    @GetMapping
    public ResponseEntity<Map<String, List<TM>>> getCurrentTickers() {
        return new ResponseEntity<>(service.getCurrentTickers(), HttpStatus.OK);
    }

    @GetMapping("state-value")
    public ResponseEntity<Map<Integer, String>> getStateValueMap() {
        return new ResponseEntity<>(service.getStateValueMap(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ResponseStatus> addTicker(@RequestParam String exchange, @RequestParam String symbol) {
        service.createThread(exchange, symbol, new String[]{getThreadCompName()});
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    @PutMapping("refresh")
    public ResponseEntity<ResponseStatus> refresh(@RequestParam String exchange, @RequestParam String symbol) {
        service.refreshBrowser(exchange, symbol);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<ResponseStatus> deleteTicker(@RequestParam String exchange, @RequestParam String symbol) {
        service.stopFetching(exchange, symbol);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    @DeleteMapping("all/")
    public ResponseEntity<ResponseStatus> deleteAllTickers() {
        service.stopFetchingAll();
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }
}