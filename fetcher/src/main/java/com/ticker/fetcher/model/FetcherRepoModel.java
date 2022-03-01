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
    private final double o;
    private final double h;
    private final double l;
    private final double c;
    private final double bbU;
    private final double bbA;
    private final double bbL;
    private final double rsi;
    private final double tema;
    private final double dayH;
    private final double dayL;
    private final double dayC;
    private final double prevClose;
    private final double dayO;

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
    public FetcherRepoModel(String tableName, long timestamp, double o, double h, double l, double c, double bbU, double bbA, double bbL, double rsi, double tema, double dayH, double dayL, double dayC, double prevClose, double dayO) {
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
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Timestamp(thread.getUpdatedAt()));
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
