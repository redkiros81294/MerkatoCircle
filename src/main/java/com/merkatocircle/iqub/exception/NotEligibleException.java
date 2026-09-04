package com.merkatocircle.iqub.exception;

/** Thrown when a member who fails the spec §3.2 eligibility check tries to bid on a round. */
public class NotEligibleException extends RuntimeException {
    public NotEligibleException(int roundNumber) {
        super("You're not eligible for round " + roundNumber
                + " yet — pay this round's contribution first, and make sure you haven't already had a turn this cycle");
    }
}
