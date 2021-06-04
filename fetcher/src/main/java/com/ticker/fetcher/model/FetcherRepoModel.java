package com.ticker.fetcher.model;

import lombok.Data;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

@Data
public class FetcherRepoModel {
    private final String tableName;
    private final String timestamp;
    private final float o;
    private final float h;
    private final float l;
    private final float c;
    private final float bbU;
    private final float bbA;
    private final float bbL;
    private final float extra;

    public FetcherRepoModel(String tableName, long timestamp, float o, float h, float l, float c, float bbU, float bbA, float bbL, float extra) {
        this.tableName = tableName;
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Timestamp(timestamp));
        this.o = o;
        this.h = h;
        this.l = l;
        this.c = c;
        this.bbU = bbU;
        this.bbA = bbA;
        this.bbL = bbL;
        this.extra = extra;
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
                ", extra=" + extra +
                '}';
    }
}
