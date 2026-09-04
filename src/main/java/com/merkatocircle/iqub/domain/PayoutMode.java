package com.merkatocircle.iqub.domain;

/**
 * How a group picks each round's winner. LOTTERY is the Tier 1 default and the only
 * mode Tier 1 exercises. AUCTION is a Tier 3 concept (spec §3.7) — defined now so
 * Iqub's schema never needs to change when Tier 3 is built.
 */
public enum PayoutMode {
    LOTTERY,
    AUCTION
}
