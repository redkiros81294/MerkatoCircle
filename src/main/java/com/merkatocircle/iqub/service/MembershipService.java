package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Membership;
import com.merkatocircle.iqub.domain.MembershipStatus;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.domain.RoundStatus;
import com.merkatocircle.iqub.repository.ContributionRepository;
import com.merkatocircle.iqub.repository.MembershipRepository;
import com.merkatocircle.iqub.repository.RoundRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final RoundRepository roundRepository;
    private final ContributionRepository contributionRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    public MembershipService(MembershipRepository membershipRepository,
                              RoundRepository roundRepository,
                              ContributionRepository contributionRepository,
                              NotificationService notificationService,
                              Clock clock) {
        this.membershipRepository = membershipRepository;
        this.roundRepository = roundRepository;
        this.contributionRepository = contributionRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /**
     * ACTIVE if the group has room, otherwise WAITLISTED (spec §3.5). If the group already
     * has a round in progress, backfills a PENDING Contribution so a member who joins
     * mid-round is immediately part of it rather than invisibly skipped.
     */
    public Membership join(Iqub iqub, Member member, LocalDate joinedDate) {
        long activeCount = membershipRepository.countByIqubAndStatus(iqub, MembershipStatus.ACTIVE);
        MembershipStatus status = activeCount < iqub.getMaxMembers()
                ? MembershipStatus.ACTIVE
                : MembershipStatus.WAITLISTED;

        Membership membership = new Membership(member, iqub, joinedDate, status);
        membership = membershipRepository.save(membership);

        if (status == MembershipStatus.ACTIVE) {
            backfillCurrentRoundContribution(iqub, member);
        }
        return membership;
    }

    public boolean alreadyMember(Iqub iqub, Member member) {
        return membershipRepository.findByMemberAndIqub(member, iqub).isPresent();
    }

    public List<Membership> membersOf(Iqub iqub) {
        return membershipRepository.findByIqub(iqub);
    }

    public Membership getById(Long membershipId) {
        return membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("No membership with id " + membershipId));
    }

    /**
     * An organizer removing an ACTIVE member (spec §3.5): the membership is deleted and, if
     * that freed a seat, the longest-waiting WAITLISTED member is promoted to ACTIVE.
     * Removing a WAITLISTED or DEFAULTED membership just deletes it — there's no seat to free.
     */
    public void removeMember(Membership membership) {
        boolean wasActive = membership.getStatus() == MembershipStatus.ACTIVE;
        Iqub iqub = membership.getIqub();
        membershipRepository.delete(membership);

        if (wasActive) {
            promoteNextWaitlisted(iqub);
        }
    }

    /** Promotes the earliest-joined WAITLISTED membership to ACTIVE, if any (spec §3.5). */
    public Optional<Membership> promoteNextWaitlisted(Iqub iqub) {
        List<Membership> waitlisted = membershipRepository.findByIqubAndStatus(iqub, MembershipStatus.WAITLISTED);
        Optional<Membership> next = waitlisted.stream().min(Comparator.comparing(Membership::getJoinedDate));

        next.ifPresent(membership -> {
            membership.setStatus(MembershipStatus.ACTIVE);
            membershipRepository.save(membership);
            backfillCurrentRoundContribution(iqub, membership.getMember());
            notificationService.notify(membership.getMember(),
                    "A seat opened up in " + iqub.getName() + " — you're off the waitlist and active from this round.");
        });
        return next;
    }

    private void backfillCurrentRoundContribution(Iqub iqub, Member member) {
        roundRepository.findTopByIqubOrderByRoundNumberDesc(iqub)
                .filter(round -> round.getStatus() != RoundStatus.CLOSED)
                .ifPresent(round -> ensureContribution(round, iqub, member));
    }

    private void ensureContribution(Round round, Iqub iqub, Member member) {
        if (contributionRepository.findByRoundAndMember(round, member).isEmpty()) {
            contributionRepository.save(new Contribution(round, member, iqub.getContributionAmount()));
        }
    }
}
