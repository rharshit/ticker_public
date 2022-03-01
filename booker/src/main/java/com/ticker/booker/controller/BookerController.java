package com.ticker.booker.controller;

import com.ticker.booker.model.TotalTrade;
import com.ticker.booker.service.BookerService;
import com.ticker.common.controller.BaseController;
import com.ticker.common.model.ResponseStatus;
import com.ticker.common.model.TickerTrade;
import com.zerodhatech.models.Margin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The type Booker controller.
 */
@RestController
@RequestMapping("/")
public class BookerController extends BaseController<BookerService> {

    @Autowired
    private BookerService service;

    /**
     * Gets zerodha login url.
     *
     * @return the zerodha login url
     */
    @GetMapping("/zerodha/login")
    public ResponseEntity<ResponseStatus> getZerodhaLoginURL() {
        return new ResponseEntity<>(new ResponseStatus(true, service.getZerodhaLoginURL()), HttpStatus.OK);
    }

    /**
     * Sets request token.
     *
     * @param requestToken the request token
     * @return the request token
     */
    @PostMapping("/zerodha/requestToken")
    public ResponseEntity<ResponseStatus> setRequestToken(@RequestParam String requestToken) {
        service.setRequestToken(requestToken);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    /**
     * Gets zerodha margins.
     *
     * @return the zerodha margins
     */
    @GetMapping("/zerodha/margins")
    public ResponseEntity<Map<String, Margin>> getZerodhaMargins() {
        return new ResponseEntity<>(service.getZerodhaMargins(), HttpStatus.OK);
    }

    /**
     * Book regular order integer.
     *
     * @param tradingSymbol     the trading symbol
     * @param exchange          the exchange
     * @param transactionType   the transaction type
     * @param orderType         the order type
     * @param quantity          the quantity
     * @param product           the product
     * @param price             the price
     * @param triggerPrice      the trigger price
     * @param disclosedQuantity the disclosed quantity
     * @param validity          the validity
     * @param tag               the tag
     * @return the integer
     */
    @PostMapping("/zerodha/regular")
    public Integer bookRegularOrder(@RequestParam String tradingSymbol, @RequestParam String exchange,
                                    @RequestParam String transactionType, @RequestParam String orderType,
                                    @RequestParam Integer quantity, @RequestParam String product,
                                    @RequestParam Optional<Double> price, @RequestParam Optional<Double> triggerPrice,
                                    @RequestParam Optional<Integer> disclosedQuantity, @RequestParam String validity,
                                    @RequestParam Optional<String> tag) {
        return service.bookRegularOrder(tradingSymbol, exchange, transactionType, orderType, quantity, product,
                price.orElse(null), triggerPrice.orElse(null), disclosedQuantity.orElse(null),
                validity, tag.orElse("Ticker"));
    }

    /**
     * Populate logs response entity.
     *
     * @param logs    the logs
     * @param appName the app name
     * @return the response entity
     */
    @PostMapping("/logs")
    public ResponseEntity<ResponseStatus> populateLogs(@RequestBody String logs, @RequestParam String appName) {
        service.populateLogs(logs, appName);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    /**
     * Gets log files.
     *
     * @return the log files
     */
    @GetMapping("/logs")
    public ResponseEntity<List<String>> getLogFiles() {
        return new ResponseEntity<>(service.getLogFiles(), HttpStatus.OK);
    }

    /**
     * Upload log file response entity.
     *
     * @param file the file
     * @return the response entity
     */
    @PostMapping("/logs/add")
    public ResponseEntity<ResponseStatus> uploadLogFile(@RequestParam String file) {
        service.uploadLogFile(file);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    /**
     * Upload all log files response entity.
     *
     * @return the response entity
     */
    @PostMapping("/logs/all")
    public ResponseEntity<ResponseStatus> uploadAllLogFiles() {
        service.uploadAllLogFiles();
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    /**
     * Populate logs response entity.
     *
     * @return the response entity
     */
    @DeleteMapping("/logs")
    public ResponseEntity<ResponseStatus> populateLogs() {
        service.deleteLogs();
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    /**
     * Gets trades.
     *
     * @return the trades
     */
    @GetMapping("/trades")
    public ResponseEntity<Map<String, Map<String, List<TickerTrade>>>> getTrades() {
        return new ResponseEntity<>(BookerService.getTrades(), HttpStatus.OK);
    }

    /**
     * Gets total trade.
     *
     * @return the total trade
     */
    @GetMapping("/totalTrade")
    public ResponseEntity<TotalTrade> getTotalTrade() {
        return new ResponseEntity<>(service.getTotalTrade(), HttpStatus.OK);
    }

}
