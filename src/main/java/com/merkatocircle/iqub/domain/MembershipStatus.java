package com.merkatocircle.iqub.domain;

/**
 * A member's standing within one specific Iqub group.
 * WAITLISTED is a Tier 2 value (spec §3.5) — Tier 1 only ever produces ACTIVE or DEFAULTED.
 */
public enum MembershipStatus {
    ACTIVE,
    WAITLISTED,
    DEFAULTED
}
