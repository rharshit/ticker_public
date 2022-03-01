package com.ticker.fetcher;

/**
 * The type Fetcher util.
 */
public class FetcherUtil {

    /**
     * Decode message string [ ].
     *
     * @param data the data
     * @return the string [ ]
     */
    public static String[] decodeMessage(String data) {
        try {
            return data.split("~m~\\d*~m~");
        } catch (Exception ignored) {

        }
        return new String[]{""};
    }

    /**
     * Encode message string.
     *
     * @param message the message
     * @return the string
     */
    public static String encodeMessage(String message) {
        return "~m~" + message.length() + "~m~" + message;
    }

}
