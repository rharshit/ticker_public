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
    private float o;
    private float h;
    private float l;
    private float c;
    private float bbU;
    private float bbA;
    private float bbL;
    private float rsi;
    private float tema;
    private float dayO;
    private float dayH;
    private float dayL;
    private float dayC;
    private float prevClose;

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
    public MockFetcherRepoModel(String tableName, long timestamp, float o, float h, float l, float c, float bbU, float bbA, float bbL, float rsi, float tema, float dayO, float dayH, float dayL, float dayC, float prevClose) {
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
