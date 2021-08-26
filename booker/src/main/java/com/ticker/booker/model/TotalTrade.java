package com.ticker.booker.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TotalTrade {
    private Map<String, Map<String, Map<String, List<CompleteTrade>>>> completeTradeMap;
    private Float totalPnl;
    private Float totalTaxes;
}
