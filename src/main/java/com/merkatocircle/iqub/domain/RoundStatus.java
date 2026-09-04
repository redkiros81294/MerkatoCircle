package com.merkatocircle.iqub.domain;

/**
 * A round's lifecycle (spec §3.3). Valid transitions: OPEN -> OVERDUE, OPEN -> CLOSED,
 * OVERDUE -> CLOSED. CLOSED is terminal. OVERDUE -> OPEN is never valid.
 */
public enum RoundStatus {
    OPEN,
    OVERDUE,
    CLOSED
}
