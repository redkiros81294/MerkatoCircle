package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Bid;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.domain.RoundStatus;
import com.merkatocircle.iqub.exception.InvalidBidException;
import com.merkatocircle.iqub.exception.NotEligibleException;
import com.merkatocircle.iqub.exception.RoundClosedException;
import com.merkatocircle.iqub.repository.BidRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Bidding for auction-mode groups (spec §3.7). Depends on {@link EligibilityChecker}, not
 * {@link RoundService} — RoundService depends on this class for the auction branch of
 * runDraw, so the dependency has to run one way only.
 */
@Service
public class BidService {

    private static final BigDecimal MIN_DISCOUNT = BigDecimal.ZERO;
    private static final BigDecimal MAX_DISCOUNT = new BigDecimal("30");

    private final BidRepository bidRepository;
    private final EligibilityChecker eligibilityChecker;
    private final Clock clock;

    public BidService(BidRepository bidRepository, EligibilityChecker eligibilityChecker, Clock clock) {
        this.bidRepository = bidRepository;
        this.eligibilityChecker = eligibilityChecker;
        this.clock = clock;
    }

    /**
     * Boundaries per spec §3.7: -1 and 0 either side of the floor, 30 and 31 either side of
     * the ceiling. A second submission from the same member in the same round revises their
     * existing offer rather than creating a duplicate row.
     */
    public Bid submitBid(Round round, Member member, BigDecimal discountPercent) {
        if (round.getStatus() == RoundStatus.CLOSED) {
            throw new RoundClosedException("Round " + round.getRoundNumber() + " is already closed");
        }
        if (discountPercent.compareTo(MIN_DISCOUNT) < 0 || discountPercent.compareTo(MAX_DISCOUNT) > 0) {
            throw new InvalidBidException("Discount must be between 0 and 30 percent");
        }
        if (!eligibilityChecker.isEligible(round, member)) {
            throw new NotEligibleException(round.getRoundNumber());
        }

        LocalDate today = LocalDate.now(clock);
        Optional<Bid> existing = bidRepository.findByRoundAndMember(round, member);
        if (existing.isPresent()) {
            Bid bid = existing.get();
            bid.updateOffer(discountPercent, today);
            return bidRepository.save(bid);
        }
        return bidRepository.save(new Bid(round, member, discountPercent, today));
    }

    public List<Bid> allBidsFor(Round round) {
        return bidRepository.findByRound(round);
    }

    public Optional<Bid> myBid(Round round, Member member) {
        return bidRepository.findByRoundAndMember(round, member);
    }

    /**
     * The winning bid, if any: highest discount among currently-eligible bidders, ties broken
     * by earliest submission. Empty means "fall back to lottery" — RoundService.runDraw
     * treats that as the signal to use {@link WinnerSelector} instead (spec §3.7).
     */
    public Optional<Bid> getTopBid(Round round) {
        return bidRepository.findByRound(round).stream()
                .filter(bid -> eligibilityChecker.isEligible(round, bid.getMember()))
                .min(Comparator
                        .comparing(Bid::getDiscountPercent, Comparator.reverseOrder())
                        .thenComparing(Bid::getSubmittedDate));
    }
}
