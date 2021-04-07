package com.ticker.fetcher.service;

import com.ticker.fetcher.repository.FetcherRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FetcherService {

    @Autowired
    FetcherRepository repository;

    public void addTicker(String exchange, String symbol) {
        exchange = exchange.toUpperCase();
        symbol = symbol.toUpperCase();

        int esID = repository.getExchangeSymbolId(exchange, symbol);
        log.info(String.valueOf(esID));
    }
}
