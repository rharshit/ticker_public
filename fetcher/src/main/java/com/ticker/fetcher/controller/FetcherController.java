package com.ticker.fetcher.controller;

import com.ticker.common.controller.BaseController;
import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.model.ResponseStatus;
import com.ticker.fetcher.model.FetcherThreadModel;
import com.ticker.fetcher.service.FetcherService;
import com.ticker.fetcher.service.TickerService;
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
public class FetcherController extends BaseController<FetcherService> {

    @Autowired
    private TickerService service;

    /**
     * Check if the instance is mock
     *
     * @return {@code false}
     */
    @GetMapping("mock")
    public ResponseEntity<Boolean> isMock() {
        return new ResponseEntity<>(false, HttpStatus.OK);
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
    public ResponseEntity<FetcherThreadModel> getCurrentValue(@RequestParam String exchange, @RequestParam String symbol) {
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
    public ResponseEntity<Map<String, List<FetcherThreadModel>>> getCurrentTickers() {
        return new ResponseEntity<>(service.getCurrentTickers(), HttpStatus.OK);
    }

    /**
     * Add tracking for the ticker, given exchange and symbol for app
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     * @param appName  the app name
     * @return response entity
     */
    @PostMapping
    public ResponseEntity<ResponseStatus> addTicker(@RequestParam String exchange, @RequestParam String symbol,
                                                    @RequestParam String appName) {
        service.createThread(exchange, symbol, new String[]{appName});
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
        service.removeAppFromThread(exchange, symbol, appName);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
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
}
