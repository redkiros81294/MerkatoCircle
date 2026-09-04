package com.merkatocircle.iqub.exception;

/** Thrown when a payment or bid is attempted against a Round that is already CLOSED. */
public class RoundClosedException extends RuntimeException {
    public RoundClosedException(String message) {
        super(message);
    }
}
