package com.ticker.fetcher.model.websocket.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.ticker.fetcher.rx.FetcherThread.STUDY_SERIES_CODE;

public class PrevBars {
    @JsonProperty(STUDY_SERIES_CODE)
    public Sds1 sds;

    public static class S {
        public List<Double> v;
        public int i;
    }

    public static class Lbs {
        public int bar_close_time;
    }

    public static class Ns {
        public String d;
        public List<Object> indexes;
    }

    public static class Sds1 {
        public String node;
        public List<S> s;
        public String t;
        public Ns ns;
        public Lbs lbs;
    }
}
