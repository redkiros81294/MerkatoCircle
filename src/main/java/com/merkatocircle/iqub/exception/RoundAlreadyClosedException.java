package com.merkatocircle.iqub.exception;

/** Thrown when RoundService.runDraw is called on a Round that is already CLOSED (spec §3.3). */
public class RoundAlreadyClosedException extends RuntimeException {
    public RoundAlreadyClosedException(int roundNumber) {
        super("Round " + roundNumber + " is already closed and cannot be drawn again");
    }
}
