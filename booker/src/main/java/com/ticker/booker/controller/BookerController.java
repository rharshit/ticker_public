package com.ticker.booker.controller;

import com.ticker.booker.service.BookerService;
import com.ticker.common.model.ResponseStatus;
import com.zerodhatech.models.Margin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ResponseStatus> populateLogs(@RequestBody String logs) {
        service.populateLogs(logs);
        return new ResponseEntity<>(new ResponseStatus(), HttpStatus.OK);
    }

}
