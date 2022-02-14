package com.ticker.booker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ticker.booker.service.BookerService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type Trade map.
 */
@Data
public class TradeMap extends HashMap<String, Object> {

    @JsonIgnore
    @Autowired
    private BookerService bookerService;

    private Float pnl;
    private Float taxes;

    /**
     * Instantiates a new Trade map.
     *
     * @param values the values
     */
    public TradeMap(Map<String, ?> values) {
        super(values);
        for (Entry<String, Object> entry : entrySet()) {
            if ((entry.getValue() instanceof Map) && !(entry.getValue() instanceof TradeMap)) {
                put(entry.getKey(), new TradeMap((Map) entry.getValue()));
            }
        }

        put("pnl", getPnl());
        put("taxes", getTaxes());
    }

    /**
     * Gets pnl.
     *
     * @return the pnl
     */
    public Float getPnl() {
        if (pnl == null) {
            float pnlTmp = 0;
            for (Entry<String, Object> entry : entrySet()) {
                if (entry.getValue() instanceof TradeMap) {
                    pnlTmp += ((TradeMap) entry.getValue()).getPnl();
                } else if (entry.getValue() instanceof List) {
                    for (Object obj : (List) entry.getValue()) {
                        if (obj instanceof CompleteTrade) {
                            pnlTmp += ((CompleteTrade) obj).getPnl();
                        }
                    }
                }
            }
            pnl = pnlTmp;
        }
        return pnl;
    }

    /**
     * Gets taxes.
     *
     * @return the taxes
     */
    public Float getTaxes() {
        if (taxes == null) {
            float taxTmp = 0;
            for (Entry<String, Object> entry : entrySet()) {
                if (entry.getValue() instanceof TradeMap) {
                    taxTmp += ((TradeMap) entry.getValue()).getTaxes();
                } else if (entry.getValue() instanceof List) {
                    for (Object obj : (List) entry.getValue()) {
                        if (obj instanceof CompleteTrade) {
                            taxTmp += ((CompleteTrade) obj).getTaxes();
                        }
                    }
                }
            }
            taxes = taxTmp;
        }
        return taxes;
    }
}
