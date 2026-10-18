package com.smarttaxi.exception;

/** Thrown when an illegal action is requested from the environment. */
public class InvalidMoveException extends TaxiException {
    private static final long serialVersionUID = 1L;

    public InvalidMoveException(String message) {
        super(message);
    }

    public InvalidMoveException(String message, Throwable cause) {
        super(message, cause);
    }
}
