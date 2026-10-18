package com.smarttaxi.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Creates loggers and writes everything under the com.smarttaxi package to a log file.
 * Call configure() once at app start; getLogger() can be used from any class.
 */
public final class LoggerUtil {

    private static final String ROOT_NAME = "com.smarttaxi";
    private static final Logger ROOT = Logger.getLogger(ROOT_NAME);
    private static FileHandler fileHandler;

    private LoggerUtil() {
    }

    /** Starts writing log records to the given file at the given level. */
    public static synchronized void configure(String logFile, String level) {
        reset();
        try {
            Path parent = Paths.get(logFile).toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Level parsed = Level.parse(level.trim().toUpperCase());
            fileHandler = new FileHandler(logFile, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(parsed);
            ROOT.setLevel(parsed);
            ROOT.addHandler(fileHandler);
        } catch (IOException | IllegalArgumentException e) {
            ROOT.log(Level.WARNING, "File logging disabled: " + e.getMessage());
        }
    }

    /** Returns a logger named after the class. */
    public static Logger getLogger(Class<?> type) {
        return Logger.getLogger(type.getName());
    }

    /** Stops file logging and closes the file (flushes pending records). */
    public static synchronized void reset() {
        if (fileHandler != null) {
            ROOT.removeHandler(fileHandler);
            fileHandler.close();
            fileHandler = null;
        }
    }
}
