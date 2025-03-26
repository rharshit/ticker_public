package com.ticker.fetcher.model.websocket.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TickerDetails {

    public String s;
    public V v;
    public String n;

    public static class BrokerNames {
        public String motilaloswal;
        public String angelone;
        public String ibkr;
        public String aliceblue;
        public String bajaj;
        public String fyers;
        public String interactiveil;
        public String mexem;
        public String tradesmart;
        public String paytm;
        public String cobratrading;
        public String dhan;
    }

    public static class Family {
        public String prefix;
        public List<Series> series;
        public String name;
        public String description;
        public String exercise;
    }

    public static class Figi {
        @JsonProperty("country-composite")
        public String country_composite;
        @JsonProperty("exchange-level")
        public String exchange_level;
    }

    public static class OptionsInfo {
        public List<Family> families;
    }

    public static class RatesFy {
        public double to_cny;
        public int to_market;
        public int to_symbol;
        public double to_jpy;
        public double to_eur;
        public double to_chf;
        public double to_aud;
        public double to_cad;
        public double to_usd;
        public double to_gbp;
        public int time;
    }

    public static class RatesMc {
        public double to_cny;
        public int to_market;
        public int to_symbol;
        public double to_jpy;
        public double to_eur;
        public double to_chf;
        public double to_aud;
        public double to_cad;
        public double to_usd;
        public double to_gbp;
        public int time;
    }

    public static class RatesTtm {
        public double to_cny;
        public int to_market;
        public int to_symbol;
        public double to_jpy;
        public double to_eur;
        public double to_chf;
        public double to_aud;
        public double to_cad;
        public double to_usd;
        public double to_gbp;
        public int time;
    }

    public static class Series {
        public String root;
        public List<Double> strikes;
        public String id;
        public String underlying;
        public int exp;
    }

    public static class Source2 {
        public String country;
        public String name;
        public String description;
        public String id;
        @JsonProperty("exchange-type")
        public String exchange_type;
        public String url;
    }

    public static class Subsession {
        @JsonProperty("private")
        public boolean myprivate;
        @JsonProperty("session-correction")
        public String session_correction;
        public String session;
        public String description;
        @JsonProperty("session-display")
        public String session_display;
        public String id;
    }

    public static class V {
        public List<String> base_name;
        @JsonProperty("source-logoid")
        public String source_logoid;
        public String subsession_id;
        public boolean is_tradable;
        public double lp;
        public double earnings_per_share_basic_ttm;
        public String current_session;
        public int open_time;
        public String language;
        public Source2 source2;
        public double market_cap_calc;
        public int earnings_release_next_date;
        public String type;
        public double dividends_yield;
        public Object rtc_time;
        public List<String> typespecs;
        public String update_mode;
        public int lp_time;
        public String fundamental_currency_code;
        @JsonProperty("options-info")
        public OptionsInfo options_info;
        public double open_price;
        public double low_price;
        public String logoid;
        public double all_time_low;
        public Object days_to_maturity;
        public double price_earnings_ttm;
        public String pro_perm;
        public int dps_common_stock_prim_issue_fy;
        public boolean fractional;
        @JsonProperty("currency-logoid")
        public String currency_logoid;
        public String listed_exchange;
        @JsonProperty("visible-plots-set")
        public String visible_plots_set;
        public double chp;
        public double earnings_per_share_fq;
        public int volume;
        public String country_code;
        public String measure;
        @JsonProperty("symbol-primaryname")
        public String symbol_primaryname;
        public String original_name;
        public RatesTtm rates_ttm;
        public String provider_id;
        public String exchange;
        public String short_name;
        public String session_holidays;
        public long total_shares_outstanding_current;
        public String pro_name;
        public double last_annual_eps;
        public int all_time_high_day;
        public String short_description;
        @JsonProperty("isin-displayed")
        public String isin_displayed;
        public RatesFy rates_fy;
        public String timezone;
        public String description;
        public Figi figi;
        public double all_time_high;
        public Object rchp;
        public String currency_code;
        public double market_cap_basic;
        public double total_shares_outstanding_calculated;
        public int earnings_release_date;
        public int pointvalue;
        public Object rch;
        public double average_volume;
        public double price_52_week_high;
        public int minmove2;
        public int price_52_week_low;
        public double prev_close_price;
        public Object rtc;
        public double beta_1_year;
        public BrokerNames broker_names;
        public List<Subsession> subsessions;
        public int first_bar_time_1s;
        public double ch;
        public String allowed_adjustment;
        public boolean hub_rt_loaded;
        public double high_price;
        public double regular_close;
        public int first_bar_time_1m;
        public int regular_close_time;
        public long total_revenue;
        public int all_time_low_day;
        public int first_bar_time_1d;
        public double earnings_per_share_forecast_next_fq;
        public int minmov;
        public int pricescale;
        public double price_earnings_current;
        public String variable_tick_size;
        public String source_id;
        public RatesMc rates_mc;
        public String currency_id;
        public String isin;

        /*Futures*/
        public String front_contract;
        @JsonProperty("underlying-symbol")
        public String underlying_symbol;
        public boolean has_backadjustment;
        public boolean rt_loaded;
        public boolean has_settlement;
        public int continuous_order;
        public String root;
    }
}
