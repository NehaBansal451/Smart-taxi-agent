package com.smarttaxi.exception;

/** Thrown when a database operation fails. */
public class DataAccessException extends TaxiException {
    private static final long serialVersionUID = 1L;

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
