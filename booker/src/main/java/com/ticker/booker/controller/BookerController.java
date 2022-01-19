package com.ticker.booker.controller;

import com.ticker.booker.model.TradeMap;
import com.ticker.booker.service.BookerService;
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

@RestController
@RequestMapping("/")
public class BookerController {

    @Autowired
    private BookerService service;

    @GetMapping("/zerodha/login")
    public ResponseEntity<ResponseStatus> getZerodhaLoginURL() {
        return new ResponseEntity<>(new ResponseStatus(true, service.getZerodhaLoginURL()), HttpStatus.OK);
    }

    @PostMapping("/zerodha/requestToken")
    public ResponseEntity<ResponseStatus> setRequestToken(@RequestParam String requestToken) {
        service.setRequestToken(requestToken);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    @GetMapping("/zerodha/margins")
    public ResponseEntity<Map<String, Margin>> getZerodhaMargins() {
        return new ResponseEntity<>(service.getZerodhaMargins(), HttpStatus.OK);
    }

    @PostMapping("/zerodha/regular")
    public Integer bookRegularOrder(@RequestParam String tradingSymbol, @RequestParam String exchange,
                                    @RequestParam String transactionType, @RequestParam String orderType,
                                    @RequestParam Integer quantity, @RequestParam String product,
                                    @RequestParam Optional<Float> price, @RequestParam Optional<Float> triggerPrice,
                                    @RequestParam Optional<Integer> disclosedQuantity, @RequestParam String validity,
                                    @RequestParam Optional<String> tag) {
        return service.bookRegularOrder(tradingSymbol, exchange, transactionType, orderType, quantity, product,
                price.orElse(null), triggerPrice.orElse(null), disclosedQuantity.orElse(null),
                validity, tag.orElse("Ticker"));
    }

    @PostMapping("/logs")
    public ResponseEntity<ResponseStatus> populateLogs(@RequestBody String logs, @RequestParam String appName) {
        service.populateLogs(logs, appName);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<String>> getLogFiles() {
        return new ResponseEntity<>(service.getLogFiles(), HttpStatus.OK);
    }

    @PostMapping("/logs/add")
    public ResponseEntity<ResponseStatus> uploadLogFile(@RequestParam String file) {
        service.uploadLogFile(file);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    @PostMapping("/logs/all")
    public ResponseEntity<ResponseStatus> uploadAllLogFiles() {
        service.uploadAllLogFiles();
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

    @GetMapping("/trades")
    public ResponseEntity<Map<String, Map<String, List<TickerTrade>>>> getTrades() {
        return new ResponseEntity<>(BookerService.getTrades(), HttpStatus.OK);
    }

    @GetMapping("/totalTrade")
    public ResponseEntity<TradeMap> getTotalTrade() {
        return new ResponseEntity<>(service.getTotalTrade(), HttpStatus.OK);
    }

}
