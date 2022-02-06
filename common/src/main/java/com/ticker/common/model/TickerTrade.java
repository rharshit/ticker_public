package com.ticker.common.model;

import com.zerodhatech.models.Trade;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Ticker trade.
 */
@Data
@NoArgsConstructor
public class TickerTrade extends Trade {
    private String appName;

    /**
     * Instantiates a new Ticker trade.
     *
     * @param other the other
     */
    public TickerTrade(TickerTrade other) {
        this.appName = other.appName;
        this.tradeId = other.tradeId;
        this.orderId = other.orderId;
        this.exchangeOrderId = other.exchangeOrderId;
        this.tradingSymbol = other.tradingSymbol;
        this.exchange = other.exchange;
        this.instrumentToken = other.instrumentToken;
        this.product = other.product;
        this.averagePrice = other.averagePrice;
        this.quantity = other.quantity;
        this.fillTimestamp = other.fillTimestamp;
        this.exchangeTimestamp = other.exchangeTimestamp;
        this.transactionType = other.transactionType;
    }
}
