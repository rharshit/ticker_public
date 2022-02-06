package com.ticker.fetcher.constants;

public abstract class ProcedureConstants {
    private ProcedureConstants() {
        throw new IllegalStateException("ProcedureConstants class");
    }

    public static final String GET_EXCHANGE_SYMBOL_ID_PR = "ticker.get_exchange_symbol_id_pr";
    public static final String ADD_TABLE = "fetcher.add_table";
}
