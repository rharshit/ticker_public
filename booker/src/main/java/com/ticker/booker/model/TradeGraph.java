package com.ticker.booker.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TradeGraph {
    List<String> labels = new ArrayList<>();
    List<Double> pnl = new ArrayList<>();
    List<Double> netPnl = new ArrayList<>();
    List<Double> taxes = new ArrayList<>();
    List<Integer> numTrades = new ArrayList<>();
}
