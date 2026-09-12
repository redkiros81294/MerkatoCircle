package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Bid;
import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.ContributionStatus;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

class RoundServiceTest {

    private RoundRepository roundRepository;
    private ContributionRepository contributionRepository;
    private MembershipRepository membershipRepository;
    private EligibilityChecker eligibilityChecker;
    private WinnerSelector winnerSelector;
    private BidService bidService;
    private NotificationService notificationService;
    private Clock clock;
    private RoundService service;

    @BeforeEach
    void setUp() {
        roundRepository = mock(RoundRepository.class);
        contributionRepository = mock(ContributionRepository.class);
        membershipRepository = mock(MembershipRepository.class);
        eligibilityChecker = mock(EligibilityChecker.class);
        winnerSelector = mock(WinnerSelector.class);
        bidService = mock(BidService.class);
        notificationService = mock(NotificationService.class);
        clock = Clock.fixed(LocalDate.of(2026, 9, 3).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new RoundService(roundRepository, contributionRepository, membershipRepository,
                eligibilityChecker, winnerSelector, bidService, notificationService, clock);
    }

    private Iqub iqub(PayoutMode mode) {
        Iqub i = new Iqub("Group", new BigDecimal("500.00"), 7, 10, LocalDate.now(clock));
        i.setPayoutMode(mode);
        return i;
    }

    private Round round(Iqub iqub, int number, RoundStatus status) {
        return round(iqub, number, status, LocalDate.now(clock).plusDays(3));
    }

    private Round round(Iqub iqub, int number, RoundStatus status, LocalDate deadline) {
        Round r = new Round(iqub, number, deadline);
        r.setStatus(status);
        return r;
    }

    private Member member(String name) {
        return new Member(name, name.toLowerCase().replace(" ", ".") + "@example.com", "0712345678", "hash", LocalDate.now(clock));
    }

    private Contribution contribution(Round r, Member m, ContributionStatus status) {
        Contribution c = new Contribution(r, m, new BigDecimal("500"));
        if (status == ContributionStatus.PAID || status == ContributionStatus.PAID_LATE) {
            c.markPaid(status, new BigDecimal("500"), LocalDate.now(clock), BigDecimal.ZERO);
        }
        return c;
    }

    @Test
    @DisplayName("OPEN → CLOSED when draw is run")
    void openRoundClosesOnDraw() {
        Iqub iqub = iqub(PayoutMode.LOTTERY);
        Round r = round(iqub, 1, RoundStatus.OPEN);
        Member winner = member("Alice");

        when(roundRepository.findTopByIqubOrderByRoundNumberDesc(iqub)).thenReturn(Optional.of(r));
        when(roundRepository.findById(1L)).thenReturn(Optional.of(r));
        when(eligibilityChecker.getEligibleMembers(r)).thenReturn(List.of(winner));
        when(winnerSelector.select(List.of(winner))).thenReturn(winner);
        when(contributionRepository.findByRound(r)).thenReturn(Collections.emptyList());
        when(membershipRepository.findByMemberAndIqub(winner, iqub)).thenReturn(Optional.of(new Membership(winner, iqub, LocalDate.now(clock), MembershipStatus.ACTIVE)));
        when(membershipRepository.findByIqubAndStatus(any(Iqub.class), any(MembershipStatus.class))).thenReturn(Collections.emptyList());
        when(roundRepository.save(any(Round.class))).thenAnswer(i -> i.getArgument(0));

        Round closed = service.runDraw(r);

        assertThat(closed.getStatus()).isEqualTo(RoundStatus.CLOSED);
        assertThat(closed.getWinner()).isEqualTo(winner);
        assertThat(closed.getPayoutAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        verify(notificationService).notify(any(), any(String.class));
    }

    @Test
    @DisplayName("Cannot draw on already closed round")
    void cannotDrawClosedRound() {
        Iqub iqub = iqub(PayoutMode.LOTTERY);
        Round r = round(iqub, 1, RoundStatus.CLOSED);

        assertThatThrownBy(() -> service.runDraw(r))
                .isInstanceOf(RoundAlreadyClosedException.class);
    }

    @Test
    @DisplayName("Cannot draw with no eligible members")
    void cannotDrawWithNoEligible() {
        Iqub iqub = iqub(PayoutMode.LOTTERY);
        Round r = round(iqub, 1, RoundStatus.OPEN);

        when(roundRepository.findTopByIqubOrderByRoundNumberDesc(iqub)).thenReturn(Optional.of(r));
        when(roundRepository.findById(1L)).thenReturn(Optional.of(r));
        when(eligibilityChecker.getEligibleMembers(r)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.runDraw(r))
                .isInstanceOf(NoEligibleMembersException.class);
    }

    @Test
    @DisplayName("AUCTION mode: highest bidder wins with discounted payout")
    void auctionModeHighestBidderWins() {
        Iqub iqub = iqub(PayoutMode.AUCTION);
        Round r = round(iqub, 1, RoundStatus.OPEN);
        Member alice = member("Alice");
        Member bob = member("Bob");

        Contribution alicePaid = contribution(r, alice, ContributionStatus.PAID);
        Contribution bobPaid = contribution(r, bob, ContributionStatus.PAID);

        Bid aliceBid = new Bid(r, alice, new BigDecimal("10"), LocalDate.now(clock));
        when(contributionRepository.findByRound(r)).thenReturn(List.of(alicePaid, bobPaid));
        when(eligibilityChecker.getEligibleMembers(r)).thenReturn(List.of(alice, bob));
        when(bidService.getTopBid(r)).thenReturn(Optional.of(aliceBid));

        when(roundRepository.findTopByIqubOrderByRoundNumberDesc(iqub)).thenReturn(Optional.of(r));
        when(roundRepository.findById(1L)).thenReturn(Optional.of(r));
        when(roundRepository.save(r)).thenReturn(r);
        when(membershipRepository.findByMemberAndIqub(alice, iqub)).thenReturn(Optional.of(new Membership(alice, iqub, LocalDate.now(clock), MembershipStatus.ACTIVE)));
        when(membershipRepository.findByIqubAndStatus(any(Iqub.class), any(MembershipStatus.class))).thenReturn(Collections.emptyList());

        Round closed = service.runDraw(r);

        assertThat(closed.getStatus()).isEqualTo(RoundStatus.CLOSED);
        assertThat(closed.getWinner()).isEqualTo(alice);
        assertThat(closed.getPayoutAmount()).isEqualByComparingTo(new BigDecimal("900.00"));
    }

    @Test
    @DisplayName("State machine: OPEN becomes OVERDUE when deadline passes and not everyone paid")
    void openBecomesOverdueWhenDeadlinePasses() {
        Iqub iqub = iqub(PayoutMode.LOTTERY);
        Round r = round(iqub, 1, RoundStatus.OPEN, LocalDate.now(clock).minusDays(1));

        Member alice = member("Alice");
        Contribution paid = contribution(r, alice, ContributionStatus.PAID);
        Contribution pending = new Contribution(r, member("Bob"), new BigDecimal("500"));

        when(roundRepository.findTopByIqubOrderByRoundNumberDesc(iqub)).thenReturn(Optional.of(r));
        when(roundRepository.findById(1L)).thenReturn(Optional.of(r));
        when(contributionRepository.findByRound(r)).thenReturn(List.of(paid, pending));

        Round current = service.getCurrentRound(iqub);

        assertThat(current.getStatus()).isEqualTo(RoundStatus.OVERDUE);
        verify(roundRepository).save(r);
    }
}
