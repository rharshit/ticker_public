package com.ticker.fetcher.controller;

import com.ticker.fetcher.model.FetcherThreadModel;
import com.ticker.fetcher.model.ResponseStatus;
import com.ticker.fetcher.repository.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.fetcher.service.TickerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/")
public class AppController {

    @Autowired
    private TickerService service;

    /**
     * Get all tickers saved in DB
     *
     * @return
     */
    @GetMapping("tickers")
    public Iterable<ExchangeSymbolEntity> getAllTickers() {
        return service.getAllTickers();
    }

    /**
     * Save a new ticker to DB
     *
     * @param exchange
     * @param symbol
     * @param tickerType
     * @param minQty
     * @param incQty
     * @param lotSize
     * @return
     */
    @PostMapping("ticker")
    public ExchangeSymbolEntity addTickerToDB(@RequestParam String exchange, @RequestParam String symbol,
                                              @RequestParam String tickerType, @RequestParam Optional<Integer> minQty
            , @RequestParam Optional<Integer> incQty, @RequestParam Optional<Integer> lotSize) {
        return service.addTickerToDB(exchange, symbol, tickerType, minQty.orElse(null), incQty.orElse(null), lotSize.orElse(null));
    }


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
