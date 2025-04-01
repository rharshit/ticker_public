package com.ticker.fetcher.model.websocket.response;

public class DayOhlc {
    public String s;
    public V v;
    public String n;

    public static class V {
        public double volume;
        public boolean rt_loaded;
        public double lp;
        public int regular_close_time;
        public double ch;
        public double regular_close;
        public double high_price;
        public int open_time;
        public double chp;
        public double open_price;
        public double low_price;
        public double prev_close_price;
    }
}
