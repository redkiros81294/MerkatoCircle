package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Member;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;

/** The real draw: a uniform random pick among the eligible pool. */
@Component
public class RandomWinnerSelector implements WinnerSelector {

    private final Random random = new SecureRandom();

    @Override
    public Member select(List<Member> eligibleMembers) {
        if (eligibleMembers.isEmpty()) {
            throw new IllegalArgumentException("Cannot select a winner from an empty pool");
        }
        int index = random.nextInt(eligibleMembers.size());
        return eligibleMembers.get(index);
    }
}
