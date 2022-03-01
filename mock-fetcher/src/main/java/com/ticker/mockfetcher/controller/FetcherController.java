package com.ticker.mockfetcher.controller;

import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.model.ResponseStatus;
import com.ticker.mockfetcher.model.MockFetcherThreadModel;
import com.ticker.mockfetcher.service.MockFetcherService;
import com.ticker.mockfetcher.service.TickerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The type Fetcher controller.
 */
@RestController
@RequestMapping("/")
public class FetcherController {

    @Autowired
    private TickerService service;


    @Autowired
    private MockFetcherService mockFetcherService;

    /**
     * Check if the instance is mock
     *
     * @return {@code true}
     */
    @GetMapping("mock")
    public ResponseEntity<Boolean> isMock() {
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    /**
     * Get all tickers saved in DB
     *
     * @return all tickers
     */
    @GetMapping("tickers")
    public Iterable<ExchangeSymbolEntity> getAllTickers() {
        return service.getAllTickers();
    }

    /**
     * Get current value of the ticker
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     * @return current value
     */
    @GetMapping("current")
    public ResponseEntity<MockFetcherThreadModel> getCurrentValue(@RequestParam String exchange, @RequestParam String symbol) {
        return new ResponseEntity<>(service.getCurrent(exchange, symbol), HttpStatus.OK);
    }

    /**
     * Save a new ticker to DB
     *
     * @param exchange   the exchange
     * @param symbol     the symbol
     * @param tickerType the ticker type
     * @param minQty     the min qty
     * @param incQty     the inc qty
     * @param lotSize    the lot size
     * @return exchange symbol entity
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
     * @return current tickers
     */
    @GetMapping
    public ResponseEntity<Map<String, List<MockFetcherThreadModel>>> getCurrentTickers() {
        return new ResponseEntity<>(service.getCurrentTickers(), HttpStatus.OK);
    }

    /**
     * Add tracking for the ticker, given exchange and symbol for app
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     * @param time     the time
     * @return response entity
     */
    @PostMapping
    public ResponseEntity<ResponseStatus> addTicker(@RequestParam String exchange, @RequestParam String symbol,
                                                    @RequestParam String time) {
        service.createThread(exchange, symbol, new String[]{time});
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
     * Remove tracking for the ticker, given exchange and symbol for app
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     * @param appName  the app name
     * @return response entity
     */
    @DeleteMapping
    public ResponseEntity<ResponseStatus> deleteTicker(@RequestParam String exchange, @RequestParam String symbol,
                                                       @RequestParam String appName) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Remove tracking for the ticker, given exchange and symbol for all apps
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     * @return response entity
     */
    @DeleteMapping("/ticker/")
    public ResponseEntity<ResponseStatus> deleteTicker(@RequestParam String exchange, @RequestParam String symbol) {
        service.deleteTicker(exchange, symbol);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    /**
     * Delete all tickers
     *
     * @return response entity
     */
    @DeleteMapping(path = "/all/")
    public ResponseEntity<ResponseStatus> deleteAllTickers() {
        service.deleteAllTickers();
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }


    /**
     * Gets executor details.
     *
     * @return the executor details
     */
    @GetMapping("/executors")
    public ResponseEntity<Map<String, Map<String, Integer>>> getExecutorDetails() {
        return new ResponseEntity<>(mockFetcherService.getExecutorDetails(), HttpStatus.OK);
    }
}
