package com.merkatocircle.iqub.exception;

/** Thrown when a bid's discount percentage falls outside spec §3.7's 0-30 range. */
public class InvalidBidException extends RuntimeException {
    public InvalidBidException(String message) {
        super(message);
    }
}
