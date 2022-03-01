package com.ticker.fetcher.model;

import com.ticker.fetcher.rx.FetcherThread;
import lombok.Data;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * The type Fetcher repo model.
 */
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
    private final float rsi;
    private final float tema;
    private final float dayH;
    private final float dayL;
    private final float dayC;
    private final float prevClose;
    private final float dayO;

    /**
     * Instantiates a new Fetcher repo model.
     *
     * @param tableName the table name
     * @param timestamp the timestamp
     * @param o         the open
     * @param h         the high
     * @param l         the low
     * @param c         the close
     * @param bbU       the bb upper
     * @param bbA       the bb average
     * @param bbL       the bb lower
     * @param rsi       the rsi
     * @param tema      the tema
     * @param dayH      the day high
     * @param dayL      the day low
     * @param dayC      the day close (current)
     * @param prevClose the prev close
     * @param dayO      the day open
     */
    public FetcherRepoModel(String tableName, long timestamp, float o, float h, float l, float c, float bbU, float bbA, float bbL, float rsi, float tema, float dayH, float dayL, float dayC, float prevClose, float dayO) {
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
        this.dayH = dayH;
        this.dayL = dayL;
        this.dayC = dayC;
        this.prevClose = prevClose;
        this.dayO = dayO;
    }

    /**
     * Instantiates a new Fetcher repo model.
     *
     * @param thread the thread
     */
    public FetcherRepoModel(FetcherThread thread) {
        this.tableName = thread.getTableName();
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Timestamp(System.currentTimeMillis()));
        this.o = thread.getO();
        this.h = thread.getH();
        this.l = thread.getL();
        this.c = thread.getC();
        this.bbU = thread.getBbU();
        this.bbA = thread.getBbA();
        this.bbL = thread.getBbL();
        this.rsi = thread.getRsi();
        this.tema = thread.getTema();
        this.dayH = thread.getDayH();
        this.dayL = thread.getDayL();
        this.dayC = thread.getDayC();
        this.prevClose = thread.getPrevClose();
        this.dayO = thread.getDayO();
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
