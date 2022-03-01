package com.ticker.mockfetcher.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * The type Mock fetcher repo model.
 */
@Data
@NoArgsConstructor
public class MockFetcherRepoModel {
    private String tableName;
    private String timestamp;
    private double o;
    private double h;
    private double l;
    private double c;
    private double bbU;
    private double bbA;
    private double bbL;
    private double rsi;
    private double tema;
    private double dayO;
    private double dayH;
    private double dayL;
    private double dayC;
    private double prevClose;

    /**
     * Instantiates a new Mock fetcher repo model.
     *
     * @param tableName the table name
     * @param timestamp the timestamp
     * @param o         the o
     * @param h         the h
     * @param l         the l
     * @param c         the c
     * @param bbU       the bb u
     * @param bbA       the bb a
     * @param bbL       the bb l
     * @param rsi       the rsi
     * @param tema      the tema
     */
    public MockFetcherRepoModel(String tableName, long timestamp, double o, double h, double l, double c, double bbU, double bbA, double bbL, double rsi, double tema, double dayO, double dayH, double dayL, double dayC, double prevClose) {
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
        this.dayO = dayO;
        this.dayH = dayH;
        this.dayL = dayL;
        this.dayC = dayC;
        this.prevClose = prevClose;
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
