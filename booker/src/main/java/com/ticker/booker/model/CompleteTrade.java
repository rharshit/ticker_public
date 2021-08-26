package com.ticker.booker.model;

import com.ticker.common.model.TickerTrade;
import lombok.Data;

@Data
public class CompleteTrade {
    private TickerTrade buy;
    private TickerTrade sell;
    private Integer quantity;
    private Float pnl;
    private Float taxes;
    private boolean completed = false;
}
