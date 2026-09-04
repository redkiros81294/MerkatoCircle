package com.merkatocircle.iqub.exception;

/** Thrown when RoundService.runDraw is called but the eligible pool (spec §3.2) is empty. */
public class NoEligibleMembersException extends RuntimeException {
    public NoEligibleMembersException(int roundNumber) {
        super("No member is currently eligible for round " + roundNumber
                + " — someone still needs to pay, or everyone eligible has already had a turn this cycle");
    }
}
