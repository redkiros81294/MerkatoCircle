package com.merkatocircle.iqub.domain;

/**
 * A member's platform-wide role. MEMBER is the default for everyone.
 * ADMIN is a Tier 2 concept — nothing in Tier 1 checks this field yet,
 * it exists so Tier 2's authorization rule (spec §3.6) doesn't need a schema change.
 */
public enum PlatformRole {
    MEMBER,
    ADMIN
}
