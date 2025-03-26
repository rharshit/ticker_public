package com.ticker.fetcher.model.websocket.response;

public class CurrentLp {
    public String s;
    public V v;
    public String n;

    public static class V {
        public int volume;
        public double lp;
        public double ch;
        public int lp_time;
        public double chp;
    }
}
