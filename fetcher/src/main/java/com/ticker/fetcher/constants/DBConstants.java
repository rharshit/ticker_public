package com.ticker.fetcher.constants;

/**
 * The type Db constants.
 */
public abstract class DBConstants {
    /**
     * The constant I_EXCHANGE_ID.
     */
    public static final String I_EXCHANGE_ID = "i_exchange_id";
    /**
     * The constant I_SYMBOL_ID.
     */
    public static final String I_SYMBOL_ID = "i_symbol_id";
    /**
     * The constant O_EXCHANGE_SYMBOL_ID.
     */
    public static final String O_EXCHANGE_SYMBOL_ID = "o_exchange_symbol_id";
    /**
     * The constant TABLE_NAME.
     */
    public static final String TABLE_NAME = "tableName";

    private DBConstants() {
        throw new IllegalStateException("DBConstants class");
    }
}
