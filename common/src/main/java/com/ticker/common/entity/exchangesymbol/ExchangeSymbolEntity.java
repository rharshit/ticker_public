package com.ticker.common.entity.exchangesymbol;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * The type Exchange symbol entity.
 */
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
    private Integer minQty = 1;
    private Integer incQty = 1;
    private Integer lotSize = 1;
    private String tags;

    /**
     * Instantiates a new Exchange symbol entity.
     *
     * @param exchangeId      the exchange id
     * @param symbolId        the symbol id
     * @param tableName       the table name
     * @param tradingviewUrl  the tradingview url
     * @param zerodhaExchange the zerodha exchange
     * @param zerodhaSymbol   the zerodha symbol
     * @param tickerType      the ticker type
     */
    public ExchangeSymbolEntity(String exchangeId, String symbolId, String tableName, String tradingviewUrl, String zerodhaExchange, String zerodhaSymbol, String tickerType) {
        this.exchangeId = exchangeId;
        this.symbolId = symbolId;
        this.tableName = tableName;
        this.tradingviewUrl = tradingviewUrl;
        this.zerodhaExchange = zerodhaExchange;
        this.zerodhaSymbol = zerodhaSymbol;
        this.tickerType = tickerType;
    }

    /**
     * Instantiates a new Exchange symbol entity.
     *
     * @param exchangeId the exchange id
     * @param symbolId   the symbol id
     */
    public ExchangeSymbolEntity(String exchangeId, String symbolId) {
        this.exchangeId = exchangeId;
        this.symbolId = symbolId;
    }

    /**
     * Gets final table name.
     *
     * @return the final table name
     */
    public String getFinalTableName() {
        if (tableName != null && tableName.contains(":")) {
            String[] split = tableName.split(":", 2);
            return split[0] + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern(split[1]));
        } else {
            return tableName;
        }
    }
}
