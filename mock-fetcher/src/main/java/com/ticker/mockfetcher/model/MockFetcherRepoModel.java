package com.ticker.mockfetcher.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

@Data
@NoArgsConstructor
public class MockFetcherRepoModel {
    private String tableName;
    private String timestamp;
    private float o;
    private float h;
    private float l;
    private float c;
    private float bbU;
    private float bbA;
    private float bbL;
    private float rsi;
    private float tema;

    public MockFetcherRepoModel(String tableName, long timestamp, float o, float h, float l, float c, float bbU, float bbA, float bbL, float rsi, float tema) {
        this.tableName = tableName;
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Timestamp(timestamp));
        this.o = o;
        this.h = h;
        this.l = l;
        this.c = c;
        this.bbU = bbU;
        this.bbA = bbA;
        this.bbL = bbL;
        this.rsi = rsi;
        this.tema = tema;
    }

    @Override
    public String toString() {
        return "FetcherRepoModel{" +
                "tableName='" + tableName + '\'' +
                ",\ttimestamp=" + timestamp +
                ", o=" + o +
                ", h=" + h +
                ", l=" + l +
                ", c=" + c +
                ", bbU=" + bbU +
                ", bbA=" + bbA +
                ", bbL=" + bbL +
                ", rsi=" + rsi +
                ", tema=" + tema +
                '}';
    }
}
