package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Bid;
import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Membership;
import com.merkatocircle.iqub.domain.MembershipStatus;
import com.merkatocircle.iqub.domain.PayoutMode;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.domain.RoundStatus;
import com.merkatocircle.iqub.exception.NoEligibleMembersException;
import com.merkatocircle.iqub.exception.RoundAlreadyClosedException;
import com.merkatocircle.iqub.repository.ContributionRepository;
import com.merkatocircle.iqub.repository.MembershipRepository;
import com.merkatocircle.iqub.repository.RoundRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RoundService {

    private final RoundRepository roundRepository;
    private final ContributionRepository contributionRepository;
    private final MembershipRepository membershipRepository;
    private final EligibilityChecker eligibilityChecker;
    private final WinnerSelector winnerSelector;
    private final BidService bidService;
    private final NotificationService notificationService;
    private final Clock clock;

    public RoundService(RoundRepository roundRepository,
                         ContributionRepository contributionRepository,
                         MembershipRepository membershipRepository,
                         EligibilityChecker eligibilityChecker,
                         WinnerSelector winnerSelector,
                         BidService bidService,
                         NotificationService notificationService,
                         Clock clock) {
        this.roundRepository = roundRepository;
        this.contributionRepository = contributionRepository;
        this.membershipRepository = membershipRepository;
        this.eligibilityChecker = eligibilityChecker;
        this.winnerSelector = winnerSelector;
        this.bidService = bidService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /** The latest round for this group, with OPEN -&gt; OVERDUE (spec §3.3) applied and persisted if due. */
    public Round getCurrentRound(Iqub iqub) {
        Round round = roundRepository.findTopByIqubOrderByRoundNumberDesc(iqub)
                .orElseThrow(() -> new IllegalStateException("Iqub '" + iqub.getName() + "' has no rounds yet"));
        refreshOverdueStatus(round);
        return round;
    }

    public List<Round> getAllRounds(Iqub iqub) {
        return roundRepository.findByIqubOrderByRoundNumberAsc(iqub);
    }

    public Round getById(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("No round with id " + roundId));
        refreshOverdueStatus(round);
        return round;
    }

    public List<Contribution> getContributions(Round round) {
        return contributionRepository.findByRound(round);
    }

    /** Opens Round 1 for a freshly created Iqub, with a Contribution for every ACTIVE member. */
    public Round createFirstRound(Iqub iqub) {
        LocalDate deadline = LocalDate.now(clock).plusDays(iqub.getRoundIntervalDays());
        Round round = roundRepository.save(new Round(iqub, 1, deadline));
        for (Membership membership : membershipRepository.findByIqubAndStatus(iqub, MembershipStatus.ACTIVE)) {
            contributionRepository.save(new Contribution(round, membership.getMember(), iqub.getContributionAmount()));
        }
        return round;
    }

    private void refreshOverdueStatus(Round round) {
        if (round.getStatus() != RoundStatus.OPEN) {
            return;
        }
        boolean pastDeadline = LocalDate.now(clock).isAfter(round.getDeadline());
        boolean everyonePaid = contributionRepository.findByRound(round).stream()
                .allMatch(Contribution::isSettled);
        if (pastDeadline && !everyonePaid) {
            round.setStatus(RoundStatus.OVERDUE);
            roundRepository.save(round);
        }
    }

    /** The spec §3.2 decision table — delegated so BidService can use the exact same rule. */
    public List<Member> getEligibleMembers(Round round) {
        return eligibilityChecker.getEligibleMembers(round);
    }

    /**
     * Draws a winner. Rejects an already-CLOSED round outright and an empty eligible pool
     * (spec §3.3). For an AUCTION-mode group (spec §3.7) with at least one bid from a
     * currently-eligible member, the highest bidder wins at their discounted payout; with
     * no bids, or in a plain LOTTERY group, {@link WinnerSelector} decides as before. Either
     * way the round closes and the next one opens immediately.
     */
    public Round runDraw(Round round) {
        if (round.getStatus() == RoundStatus.CLOSED) {
            throw new RoundAlreadyClosedException(round.getRoundNumber());
        }
        List<Member> eligible = eligibilityChecker.getEligibleMembers(round);
        if (eligible.isEmpty()) {
            throw new NoEligibleMembersException(round.getRoundNumber());
        }

        BigDecimal totalPool = totalPaidIn(round);
        Optional<Bid> topBid = round.getIqub().getPayoutMode() == PayoutMode.AUCTION
                ? bidService.getTopBid(round)
                : Optional.empty();

        Member winner;
        BigDecimal payoutAmount;
        if (topBid.isPresent()) {
            winner = topBid.get().getMember();
            BigDecimal discountFraction = topBid.get().getDiscountPercent()
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            payoutAmount = totalPool.multiply(BigDecimal.ONE.subtract(discountFraction))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            winner = winnerSelector.select(eligible);
            payoutAmount = totalPool;
        }

        LocalDate today = LocalDate.now(clock);
        round.closeWithWinner(winner, payoutAmount, today);
        roundRepository.save(round);

        membershipRepository.findByMemberAndIqub(winner, round.getIqub()).ifPresent(membership -> {
            membership.setHasReceivedPayoutThisCycle(true);
            membershipRepository.save(membership);
        });

        notificationService.notify(winner, "You won round " + round.getRoundNumber() + " of "
                + round.getIqub().getName() + " — " + payoutAmount + " ETB paid out.");

        openNextRound(round);
        return round;
    }

    private BigDecimal totalPaidIn(Round round) {
        return contributionRepository.findByRound(round).stream()
                .filter(Contribution::isSettled)
                .map(Contribution::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void openNextRound(Round closedRound) {
        Iqub iqub = closedRound.getIqub();
        LocalDate nextDeadline = LocalDate.now(clock).plusDays(iqub.getRoundIntervalDays());
        Round next = new Round(iqub, closedRound.getRoundNumber() + 1, nextDeadline);
        next = roundRepository.save(next);

        for (Membership membership : membershipRepository.findByIqubAndStatus(iqub, MembershipStatus.ACTIVE)) {
            contributionRepository.save(new Contribution(next, membership.getMember(), iqub.getContributionAmount()));
        }
    }
}
