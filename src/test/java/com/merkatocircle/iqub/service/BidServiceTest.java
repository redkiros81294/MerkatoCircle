package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Bid;
import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.PayoutMode;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.domain.RoundStatus;
import com.merkatocircle.iqub.exception.InvalidBidException;
import com.merkatocircle.iqub.exception.NotEligibleException;
import com.merkatocircle.iqub.exception.RoundClosedException;
import com.merkatocircle.iqub.repository.BidRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

class BidServiceTest {

    private BidRepository bidRepository;
    private EligibilityChecker eligibilityChecker;
    private Clock clock;
    private BidService service;

    @BeforeEach
    void setUp() {
        bidRepository = mock(BidRepository.class);
        eligibilityChecker = mock(EligibilityChecker.class);
        clock = Clock.fixed(LocalDate.of(2026, 9, 3).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new BidService(bidRepository, eligibilityChecker, clock);
    }

    private Iqub iqub(PayoutMode mode) {
        return new Iqub("Group", new BigDecimal("500.00"), 7, 10, LocalDate.now(clock));
    }

    private Round round(Iqub iqub, int number, RoundStatus status) {
        Round r = new Round(iqub, number, LocalDate.now(clock).plusDays(3));
        r.setStatus(status);
        return r;
    }

    private Member member(String name) {
        return new Member(name, name.toLowerCase().replace(" ", ".") + "@example.com", "0712345678", "hash", LocalDate.now(clock));
    }

    @Test
    @DisplayName("BVA: discount -1 rejected, 0 accepted, 30 accepted, 31 rejected")
    void bidDiscountBoundaries() {
        Iqub iqub = iqub(PayoutMode.AUCTION);
        Round r = round(iqub, 1, RoundStatus.OPEN);
        Member m = member("Alice");

        when(eligibilityChecker.isEligible(r, m)).thenReturn(true);
        when(bidRepository.findByRoundAndMember(r, m)).thenReturn(Optional.empty());
        when(bidRepository.save(any(Bid.class))).thenAnswer(i -> i.getArgument(0));

        // BVA: -1 (below floor)
        assertThatThrownBy(() -> service.submitBid(r, m, new BigDecimal("-1")))
                .isInstanceOf(InvalidBidException.class);

        // BVA: 0 (lower boundary)
        Bid bid0 = service.submitBid(r, m, BigDecimal.ZERO);
        assertThat(bid0.getDiscountPercent()).isEqualByComparingTo(BigDecimal.ZERO);

        // BVA: 30 (upper boundary)
        Bid bid30 = service.submitBid(r, m, new BigDecimal("30"));
        assertThat(bid30.getDiscountPercent()).isEqualByComparingTo(new BigDecimal("30"));

        // BVA: 31 (above ceiling)
        assertThatThrownBy(() -> service.submitBid(r, m, new BigDecimal("31")))
                .isInstanceOf(InvalidBidException.class);
    }

    @Test
    @DisplayName("Cannot bid on closed round")
    void rejectsClosedRound() {
        Iqub iqub = iqub(PayoutMode.AUCTION);
        Round r = round(iqub, 1, RoundStatus.CLOSED);
        Member m = member("Alice");

        assertThatThrownBy(() -> service.submitBid(r, m, new BigDecimal("10")))
                .isInstanceOf(RoundClosedException.class);
    }

    @Test
    @DisplayName("Cannot bid if not eligible")
    void rejectsIneligibleMember() {
        Iqub iqub = iqub(PayoutMode.AUCTION);
        Round r = round(iqub, 1, RoundStatus.OPEN);
        Member m = member("Alice");

        when(eligibilityChecker.isEligible(r, m)).thenReturn(false);

        assertThatThrownBy(() -> service.submitBid(r, m, new BigDecimal("10")))
                .isInstanceOf(NotEligibleException.class);
    }

    @Test
    @DisplayName("Second bid revises existing offer")
    void revisesExistingBid() {
        Iqub iqub = iqub(PayoutMode.AUCTION);
        Round r = round(iqub, 1, RoundStatus.OPEN);
        Member m = member("Alice");
        Bid existing = new Bid(r, m, new BigDecimal("5"), LocalDate.now(clock).minusDays(1));

        when(eligibilityChecker.isEligible(r, m)).thenReturn(true);
        when(bidRepository.findByRoundAndMember(r, m)).thenReturn(Optional.of(existing));
        when(bidRepository.save(any(Bid.class))).thenAnswer(i -> i.getArgument(0));

        Bid revised = service.submitBid(r, m, new BigDecimal("15"));

        assertThat(revised.getDiscountPercent()).isEqualByComparingTo(new BigDecimal("15"));
        assertThat(revised).isSameAs(existing);
        verify(bidRepository).save(existing);
    }
}
