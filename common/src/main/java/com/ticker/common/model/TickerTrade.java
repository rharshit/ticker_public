package com.ticker.common.model;

import com.zerodhatech.models.Trade;
import lombok.Data;

@Data
public class TickerTrade extends Trade {
    private String appName;

}
