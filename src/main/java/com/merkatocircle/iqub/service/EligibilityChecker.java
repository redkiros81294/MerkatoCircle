package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Membership;
import com.merkatocircle.iqub.domain.MembershipStatus;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.repository.ContributionRepository;
import com.merkatocircle.iqub.repository.MembershipRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The spec §3.2 decision table, on its own so both {@link RoundService} (the lottery draw)
 * and {@link BidService} (checking who's even allowed to bid) can depend on it without
 * depending on each other — RoundService also depends on BidService for auction rounds
 * (spec §3.7), and a service can't depend on something that depends back on it.
 */
@Component
public class EligibilityChecker {

    private final ContributionRepository contributionRepository;
    private final MembershipRepository membershipRepository;

    public EligibilityChecker(ContributionRepository contributionRepository, MembershipRepository membershipRepository) {
        this.contributionRepository = contributionRepository;
        this.membershipRepository = membershipRepository;
    }

    /** C1: paid this round. C2: hasn't already won this cycle. C3: membership still ACTIVE. */
    public List<Member> getEligibleMembers(Round round) {
        List<Member> eligible = new ArrayList<>();
        for (Contribution contribution : contributionRepository.findByRound(round)) {
            if (!contribution.isSettled()) {
                continue;
            }
            Optional<Membership> membershipOpt =
                    membershipRepository.findByMemberAndIqub(contribution.getMember(), round.getIqub());
            if (membershipOpt.isEmpty()) {
                continue;
            }
            Membership membership = membershipOpt.get();
            if (membership.isHasReceivedPayoutThisCycle()) {
                continue;
            }
            if (membership.getStatus() != MembershipStatus.ACTIVE) {
                continue;
            }
            eligible.add(contribution.getMember());
        }
        return eligible;
    }

    public boolean isEligible(Round round, Member member) {
        return getEligibleMembers(round).stream().anyMatch(m -> m.getId().equals(member.getId()));
    }
}
