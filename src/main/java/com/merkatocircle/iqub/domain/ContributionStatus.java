package com.merkatocircle.iqub.domain;

/**
 * The payment state machine for a single Contribution (spec §3.4):
 *
 *   PENDING --initiate()--&gt; AWAITING_PAYMENT --verify()=success--&gt; PAID or PAID_LATE
 *                                |--verify()=failed--&gt; PAYMENT_FAILED --initiate() again--&gt; AWAITING_PAYMENT
 *                                |--verify()=pending--&gt; stays AWAITING_PAYMENT
 *
 * PAID vs PAID_LATE is decided by the lateness bands in spec §3.1.
 */
public enum ContributionStatus {
    PENDING,
    AWAITING_PAYMENT,
    PAID,
    PAID_LATE,
    PAYMENT_FAILED
}
