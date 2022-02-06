package com.ticker.fetcher.constants;

/**
 * The type Procedure constants.
 */
public abstract class ProcedureConstants {
    /**
     * The constant GET_EXCHANGE_SYMBOL_ID_PR.
     */
    public static final String GET_EXCHANGE_SYMBOL_ID_PR = "ticker.get_exchange_symbol_id_pr";
    /**
     * The constant ADD_TABLE.
     */
    public static final String ADD_TABLE = "fetcher.add_table";

    private ProcedureConstants() {
        throw new IllegalStateException("ProcedureConstants class");
    }
}
