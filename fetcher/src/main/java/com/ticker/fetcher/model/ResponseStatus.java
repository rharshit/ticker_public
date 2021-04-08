package com.ticker.fetcher.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ResponseStatus {
    private boolean success;
    private String reason;

    public ResponseStatus() {
        this.success = true;
    }

    public ResponseStatus(boolean success, String reason) {
        this.success = success;
        this.reason = reason;
    }
}
