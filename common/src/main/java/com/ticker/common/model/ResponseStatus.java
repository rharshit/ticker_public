package com.ticker.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * The type Response status.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ResponseStatus {
    private boolean success;
    private String reason;

    /**
     * Instantiates a new Response status.
     */
    public ResponseStatus() {
        this.success = true;
    }

    /**
     * Instantiates a new Response status.
     *
     * @param success the success
     * @param reason  the reason
     */
    public ResponseStatus(boolean success, String reason) {
        this.success = success;
        this.reason = reason;
    }
}
