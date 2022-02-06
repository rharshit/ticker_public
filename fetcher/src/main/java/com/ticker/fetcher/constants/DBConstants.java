package com.ticker.fetcher.constants;

public abstract class DBConstants {
    public static final String I_EXCHANGE_ID = "i_exchange_id";
    public static final String I_SYMBOL_ID = "i_symbol_id";
    public static final String O_EXCHANGE_SYMBOL_ID = "o_exchange_symbol_id";
    public static final String TABLE_NAME = "tableName";

    private DBConstants() {
        throw new IllegalStateException("DBConstants class");
    }
}
