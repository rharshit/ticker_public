package com.ticker.mwave.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ticker.mwave.rx.MWaveThread;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MWaveThreadModel {

    @JsonIgnore
    private MWaveThread thread;

    public String getExchange() {
        return this.thread.getExchange();
    }

    public String getSymbol() {
        return this.thread.getSymbol();
    }
}
