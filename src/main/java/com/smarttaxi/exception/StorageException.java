package com.smarttaxi.exception;

/** Thrown when saving or loading a file fails. */
public class StorageException extends TaxiException {
    private static final long serialVersionUID = 1L;

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
