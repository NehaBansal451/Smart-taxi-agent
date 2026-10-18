package com.smarttaxi.exception;

/** Base class of all checked exceptions in the Smart Taxi Agent application. */
public class TaxiException extends Exception {
    private static final long serialVersionUID = 1L;

    public TaxiException(String message) {
        super(message);
    }

    public TaxiException(String message, Throwable cause) {
        super(message, cause);
    }
}
