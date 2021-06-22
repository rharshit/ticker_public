package com.ticker.mwave.controller;

import com.ticker.common.model.ResponseStatus;
import com.ticker.mwave.model.MWaveThreadModel;
import com.ticker.mwave.service.MWaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
public class MWaveController {

    @Autowired
    private MWaveService service;

    @GetMapping
    public ResponseEntity<Map<String, List<MWaveThreadModel>>> getCurrentTickers() {
        return new ResponseEntity<>(service.getCurrentTickers(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ResponseStatus> addTicker(@RequestParam String exchange, @RequestParam String symbol) {
        service.createThread(exchange, symbol);
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
