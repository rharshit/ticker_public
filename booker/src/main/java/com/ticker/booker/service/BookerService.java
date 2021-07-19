package com.ticker.booker.service;

import org.springframework.stereotype.Service;

@Service
public class BookerService {
    public Integer bookRegularOrder(String tradingSymbol, String exchange, String transactionType, String orderType,
                                    Integer quantity, String product, Float price, Float triggerPrice,
                                    Integer disclosedQuantity, String validity, String tag) {
        return null;
    }
}
