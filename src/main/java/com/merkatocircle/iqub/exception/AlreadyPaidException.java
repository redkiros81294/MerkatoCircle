package com.merkatocircle.iqub.exception;

/** Thrown on a second payment attempt for a Contribution that is already PAID or PAID_LATE. */
public class AlreadyPaidException extends RuntimeException {
    public AlreadyPaidException(int roundNumber) {
        super("This round (" + roundNumber + ") is already paid — no need to pay again");
    }
}
