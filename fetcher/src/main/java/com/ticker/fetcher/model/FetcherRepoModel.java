package com.ticker.fetcher.model;

import lombok.AllArgsConstructor;

import java.sql.Timestamp;

@AllArgsConstructor
public class FetcherRepoModel {
    private final String tableName;
    private final long timestamp;
    private final float o;
    private final float h;
    private final float l;
    private final float c;
    private final float bbU;
    private final float bbA;
    private final float bbL;
    private final float extra;

    @Override
    public String toString() {
        return "FetcherRepoModel{" +
                "tableName='" + tableName + '\'' +
                ",\ttimestamp=" + new Timestamp(timestamp) +
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
