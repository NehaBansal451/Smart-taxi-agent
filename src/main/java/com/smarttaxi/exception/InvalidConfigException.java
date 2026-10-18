package com.smarttaxi.exception;

/** Thrown when a configuration value is missing or out of range. */
public class InvalidConfigException extends TaxiException {
    private static final long serialVersionUID = 1L;

    public InvalidConfigException(String message) {
        super(message);
    }

    public InvalidConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
