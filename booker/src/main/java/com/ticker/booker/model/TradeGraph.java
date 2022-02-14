package com.ticker.booker.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TradeGraph {
    List<String> labels = new ArrayList<>();
    List<Float> pnl = new ArrayList<>();
    List<Float> netPnl = new ArrayList<>();
    List<Float> taxes = new ArrayList<>();
}
