package com.ticker.fetcher.repository.exchangesymbol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@Table(name = "exchange_symbol")
@IdClass(ExchangeSymbolEntityPK.class)
@AllArgsConstructor
@NoArgsConstructor
public class ExchangeSymbolEntity implements Serializable {
    @Id
    private String exchangeId;
    @Id
    private String symbolId;
    private String tableName;
    private String tradingviewUrl;
    private String zerodhaExchange;
    private String zerodhaSymbol;
    private String tickerType;
    private Integer minQty = 0;
    private Integer incQty = 1;
    private Integer lotSize = 1;

    public ExchangeSymbolEntity(String exchangeId, String symbolId, String tableName, String tradingviewUrl, String zerodhaExchange, String zerodhaSymbol, String tickerType) {
        this.exchangeId = exchangeId;
        this.symbolId = symbolId;
        this.tableName = tableName;
        this.tradingviewUrl = tradingviewUrl;
        this.zerodhaExchange = zerodhaExchange;
        this.zerodhaSymbol = zerodhaSymbol;
        this.tickerType = tickerType;
    }
}
