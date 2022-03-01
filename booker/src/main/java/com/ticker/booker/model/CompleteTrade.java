package com.ticker.booker.model;

import com.ticker.common.model.TickerTrade;
import lombok.Data;

import java.util.Date;

/**
 * The type Complete trade.
 */
@Data
public class CompleteTrade {
    private String appName;
    private TickerTrade buy;
    private TickerTrade sell;
    private String symbol;
    private String exchange;
    private String product;
    private String tickerType;
    private Integer quantity;
    private Double pnl;
    private Double taxes;
    private boolean completed = false;

    public Date getTimestamp() {
        Date d1 = buy.exchangeTimestamp;
        Date d2 = buy.exchangeTimestamp;
        return d1.compareTo(d2) < 0 ? d1 : d2;
    }
}
