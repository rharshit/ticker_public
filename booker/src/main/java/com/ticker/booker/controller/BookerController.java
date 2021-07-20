package com.ticker.booker.controller;

import com.ticker.booker.service.BookerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/")
public class BookerController {

    @Autowired
    private BookerService service;

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

}
