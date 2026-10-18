package com.smarttaxi.exception;

/** Thrown when a city map is corrupt or unsolvable. */
public class InvalidMapException extends TaxiException {
    private static final long serialVersionUID = 1L;

    public InvalidMapException(String message) {
        super(message);
    }

    public InvalidMapException(String message, Throwable cause) {
        super(message, cause);
    }
}
