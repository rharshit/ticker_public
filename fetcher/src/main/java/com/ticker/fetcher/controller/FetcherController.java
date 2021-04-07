package com.ticker.fetcher.controller;

import com.ticker.fetcher.service.FetcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fetcher/")
public class FetcherController {

    @Autowired
    private FetcherService service;

    @PostMapping
    public ResponseEntity addTicker(@RequestParam String exchange, @RequestParam String symbol) {
        service.addTicker(exchange, symbol);
        return new ResponseEntity(HttpStatus.OK);
    }
}
