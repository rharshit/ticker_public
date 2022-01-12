package com.ticker.common.entity.exchangesymbol;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
public class ExchangeSymbolEntityPK implements Serializable {
    private String exchangeId;
    private String symbolId;
}
