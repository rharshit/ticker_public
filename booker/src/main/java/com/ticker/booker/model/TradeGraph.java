package com.ticker.booker.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Trade graph.
 */
@Data
public class TradeGraph {
    /**
     * The Labels.
     */
    List<String> labels = new ArrayList<>();
    /**
     * The Pnl.
     */
    List<Double> pnl = new ArrayList<>();
    /**
     * The Net pnl.
     */
    List<Double> netPnl = new ArrayList<>();
    /**
     * The Taxes.
     */
    List<Double> taxes = new ArrayList<>();
    /**
     * The Num trades.
     */
    List<Integer> numTrades = new ArrayList<>();
}
