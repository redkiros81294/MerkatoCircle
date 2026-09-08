package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Member;
import java.util.List;

/**
 * The one seam between RoundService and randomness. Nothing in this app should call
 * Math.random() or `new Random()` directly — go through this interface instead, so a
 * unit test can inject a selector that always returns a chosen member (see spec §2).
 */
public interface WinnerSelector {

    /**
     * @param eligibleMembers never empty — RoundService checks that before calling this
     */
    Member select(List<Member> eligibleMembers);
}
