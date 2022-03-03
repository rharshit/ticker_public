package com.ticker.common.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static com.ticker.common.contants.TickerConstants.*;

/**
 * The type Util.
 */
@Slf4j
public abstract class Util {
    /**
     * The constant WAIT_QUICK.
     */
    public static final long WAIT_QUICK = 25;
    /**
     * The constant WAIT_SHORT.
     */
    public static final long WAIT_SHORT = 250;
    /**
     * The constant WAIT_MEDIUM.
     */
    public static final long WAIT_MEDIUM = 750;
    /**
     * The constant WAIT_LONG.
     */
    public static final long WAIT_LONG = 2000;

    /**
     * Wait for.
     *
     * @param time the time
     */
    public static void waitFor(long time) {
        log.debug("Waiting for " + time + "ms");
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            log.error("Error while waiting", e);
        }
        log.debug("Resume");
    }

    /**
     * Gets application url.
     *
     * @param application the application
     * @return the application url
     */
    public static String getApplicationUrl(String application) {
        switch (application) {
            case APPLICATION_HOME:
                return "http://localhost:8080/";
            case APPLICATION_FETCHER:
                return "http://localhost:8081/";
            case APPLICATION_BROKERAGE:
                return "http://localhost:8082/";
            case APPLICATION_BB_RSI:
                return "http://localhost:8181/";
            case APPLICATION_BB_RSI_SAFE:
                return "http://localhost:8183/";
            case APPLICATION_MWAVE:
                return "http://localhost:8182/";
            default:
                return null;
        }
    }

    public static void writeToFile(String path, String message, boolean append) {
        try {
            createFile(path);
            write(path, message, append);
        } catch (Exception e) {
            log.debug("Error occurred while writing to file at path: " + path);
        }
    }

    private static synchronized void write(String path, String message, boolean append) {
        try (FileWriter myWriter = new FileWriter(path, append)) {
            myWriter.write(message);
            if (append) {
                myWriter.write("\n");
            }
        } catch (IOException e) {
            log.debug("Error while writing data");
        }
    }

    private static void write(File file, String message, boolean append) {
        write(file.getPath(), message, append);
    }

    private static void createFile(String path) {
        try {
            File file = new File(path);
            createFile(file);
        } catch (Exception e) {
            log.debug("Error occurred while creating file at path: " + path);
        }
    }

    private static void createFile(File file) {
        try {
            if (file.createNewFile()) {
                log.debug("File created: " + file.getName());
            } else {
                log.debug("File already exists " + file.getName());
            }
        } catch (IOException e) {
            log.debug("An error occurred while creating file");
        }
    }
}
