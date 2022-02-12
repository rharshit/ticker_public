package com.ticker.fetcher;

public class FetcherUtil {

    public static String[] decodeMessage(String data) {
        try {
            return data.split("~m~\\d*~m~");
        } catch (Exception ignored) {

        }
        return new String[]{""};
    }

    public static String encodeMessage(String message) {
        return "~m~" + message.length() + "~m~" + message;
    }

}
