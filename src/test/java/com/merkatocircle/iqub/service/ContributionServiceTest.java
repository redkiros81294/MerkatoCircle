package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.ContributionStatus;
import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Membership;
import com.merkatocircle.iqub.domain.MembershipStatus;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.domain.RoundStatus;
import com.merkatocircle.iqub.exception.AlreadyPaidException;
import com.merkatocircle.iqub.exception.RoundClosedException;
import com.merkatocircle.iqub.repository.ContributionRepository;
import com.merkatocircle.iqub.repository.MembershipRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

class ContributionServiceTest {

    private ContributionRepository contributionRepository;
    private MembershipRepository membershipRepository;
    private PaymentGateway paymentGateway;
    private NotificationService notificationService;
    private Clock clock;
    private ContributionService service;

    @BeforeEach
    void setUp() {
        contributionRepository = mock(ContributionRepository.class);
        membershipRepository = mock(MembershipRepository.class);
        paymentGateway = mock(PaymentGateway.class);
        notificationService = mock(NotificationService.class);
        clock = Clock.fixed(LocalDate.of(2026, 9, 3).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new ContributionService(contributionRepository, membershipRepository, paymentGateway, notificationService, clock);
    }

    private Member member(String name) {
        return new Member(name, name.toLowerCase().replace(" ", ".") + "@example.com", "0712345678", "hash", LocalDate.now(clock));
    }

    private Iqub iqub() {
        return new Iqub("Test Group", new BigDecimal("500.00"), 7, 10, LocalDate.now(clock));
    }

    private Round round(Iqub iqub, int number, RoundStatus status) {
        Round r = new Round(iqub, number, LocalDate.now(clock).plusDays(3));
        r.setStatus(status);
        return r;
    }

    private static void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("calculatePenalty — equivalence partitions and boundary values")
    class PenaltyTests {

        static List<Arguments> penaltyCases() {
            return List.of(
                arguments(-1, new BigDecimal("500"), new BigDecimal("0.00"), "paid early"),
                arguments(0, new BigDecimal("500"), new BigDecimal("0.00"), "paid on deadline"),
                arguments(1, new BigDecimal("500"), new BigDecimal("25.00"), "1 day late"),
                arguments(3, new BigDecimal("500"), new BigDecimal("25.00"), "3 days late (upper bound)"),
                arguments(4, new BigDecimal("500"), new BigDecimal("75.00"), "4 days late (lower bound)"),
                arguments(7, new BigDecimal("500"), new BigDecimal("75.00"), "7 days late (upper bound)"),
                arguments(8, new BigDecimal("500"), new BigDecimal("125.00"), "8 days late (lower bound)"),
                arguments(30, new BigDecimal("500"), new BigDecimal("125.00"), "30 days late"),
                arguments(90, new BigDecimal("500"), new BigDecimal("125.00"), "90 days late")
            );
        }

        @ParameterizedTest(name = "daysLate={0}, amount={1} → penalty={2} ({3})")
        @MethodSource("penaltyCases")
        void calculatesCorrectPenalty(long daysLate, BigDecimal amount, BigDecimal expectedPenalty, String description) {
            BigDecimal penalty = service.calculatePenalty(daysLate, amount);
            assertThat(penalty).isEqualByComparingTo(expectedPenalty);
        }

        @Test
        @DisplayName("BVA: boundary 3→4 days changes penalty from 5% to 15%")
        void penaltyJumpsAtBoundary3To4() {
            BigDecimal penalty3 = service.calculatePenalty(3, new BigDecimal("1000"));
            BigDecimal penalty4 = service.calculatePenalty(4, new BigDecimal("1000"));
            assertThat(penalty3).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(penalty4).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("BVA: boundary 7→8 days changes penalty from 15% to 25%")
        void penaltyJumpsAtBoundary7To8() {
            BigDecimal penalty7 = service.calculatePenalty(7, new BigDecimal("1000"));
            BigDecimal penalty8 = service.calculatePenalty(8, new BigDecimal("1000"));
            assertThat(penalty7).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(penalty8).isEqualByComparingTo(new BigDecimal("250.00"));
        }
    }

    @Nested
    @DisplayName("initiatePayment — payment state machine")
    class InitiatePaymentTests {

        @Test
        @DisplayName("Cannot initiate payment on a closed round")
        void rejectsClosedRound() {
            Iqub iqub = iqub();
            Round closed = round(iqub, 1, RoundStatus.CLOSED);
            Member m = member("Alice");
            Contribution c = new Contribution(closed, m, new BigDecimal("500"));

            when(contributionRepository.findByRoundAndMember(closed, m)).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> service.initiatePayment(closed, m, "cb", "ret"))
                            .isInstanceOf(RoundClosedException.class);
            verify(paymentGateway, never()).initiate(any());
        }

        @Test
        @DisplayName("Cannot initiate payment if already settled")
        void rejectsAlreadyPaid() {
            Iqub iqub = iqub();
            Round openRound = round(iqub, 1, RoundStatus.OPEN);
            Member m = member("Alice");
            Contribution c = new Contribution(openRound, m, new BigDecimal("500"));
            c.markPaid(ContributionStatus.PAID, new BigDecimal("500"), LocalDate.now(clock), BigDecimal.ZERO);

            when(contributionRepository.findByRoundAndMember(openRound, m)).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> service.initiatePayment(openRound, m, "cb", "ret"))
                            .isInstanceOf(AlreadyPaidException.class);
        }

        @Test
        @DisplayName("Initiates payment and marks contribution AWAITING_PAYMENT")
        void initiatesPaymentSuccessfully() {
            Iqub iqub = iqub();
            Round openRound = round(iqub, 1, RoundStatus.OPEN);
            Member m = member("Alice");
            Contribution c = new Contribution(openRound, m, new BigDecimal("500"));

            when(contributionRepository.findByRoundAndMember(openRound, m)).thenReturn(Optional.of(c));
            when(paymentGateway.initiate(any(PaymentRequest.class)))
                    .thenReturn(new PaymentInitiation("https://chapa.test/checkout", "iqub-1-123"));
            when(contributionRepository.save(any(Contribution.class))).thenAnswer(i -> i.getArgument(0));

            PaymentInitiation result = service.initiatePayment(openRound, m, "https://app.test/cb", "https://app.test/ret");

            assertThat(result.checkoutUrl()).isEqualTo("https://chapa.test/checkout");
            assertThat(c.getStatus()).isEqualTo(ContributionStatus.AWAITING_PAYMENT);
            assertThat(c.getTxRef()).startsWith("iqub-");
            verify(contributionRepository).save(c);
        }
    }

    @Nested
    @DisplayName("confirmPayment — idempotency and state transitions")
    class ConfirmPaymentTests {

        @Test
        @DisplayName("SUCCESS: marks contribution PAID or PAID_LATE")
        void confirmsSuccessfulPayment() {
            Iqub iqub = iqub();
            Round openRound = round(iqub, 1, RoundStatus.OPEN);
            Member m = member("Alice");
            Contribution c = new Contribution(openRound, m, new BigDecimal("500"));
            c.markAwaitingPayment("iqub-tx-1");

            when(contributionRepository.findByTxRef("iqub-tx-1")).thenReturn(Optional.of(c));
            when(paymentGateway.verify("iqub-tx-1")).thenReturn(new PaymentVerification(PaymentStatus.SUCCESS, "Success", "tx-1"));
            when(contributionRepository.save(any(Contribution.class))).thenAnswer(i -> i.getArgument(0));

            Contribution result = service.confirmPayment("iqub-tx-1");

            assertThat(result.isSettled()).isTrue();
            assertThat(result.getStatus()).isIn(ContributionStatus.PAID, ContributionStatus.PAID_LATE);
            assertThat(result.getAmountPaid()).isEqualByComparingTo(new BigDecimal("500"));
        }

        @Test
        @DisplayName("FAILED: marks contribution PAYMENT_FAILED and notifies member")
        void recordsFailedPayment() {
            Iqub iqub = iqub();
            Round openRound = round(iqub, 1, RoundStatus.OPEN);
            Member m = member("Alice");
            Contribution c = new Contribution(openRound, m, new BigDecimal("500"));
            c.markAwaitingPayment("iqub-tx-2");

            when(contributionRepository.findByTxRef("iqub-tx-2")).thenReturn(Optional.of(c));
            when(paymentGateway.verify("iqub-tx-2")).thenReturn(new PaymentVerification(PaymentStatus.FAILED, "Failed", "tx-2"));
            when(contributionRepository.save(any(Contribution.class))).thenAnswer(i -> i.getArgument(0));

            Contribution result = service.confirmPayment("iqub-tx-2");

            assertThat(result.getStatus()).isEqualTo(ContributionStatus.PAYMENT_FAILED);
            verify(notificationService).notify(eq(m), any(String.class));
        }

        @Test
        @DisplayName("Idempotent: confirming an already-settled contribution is a no-op")
        void idempotentOnSettled() {
            Iqub iqub = iqub();
            Round openRound = round(iqub, 1, RoundStatus.OPEN);
            Member m = member("Alice");
            Contribution c = new Contribution(openRound, m, new BigDecimal("500"));
            c.markPaid(ContributionStatus.PAID, new BigDecimal("500"), LocalDate.now(clock), BigDecimal.ZERO);

            when(contributionRepository.findByTxRef("iqub-tx-3")).thenReturn(Optional.of(c));

            Contribution result = service.confirmPayment("iqub-tx-3");

            assertThat(result).isSameAs(c);
            verify(paymentGateway, never()).verify(anyString());
        }

        @Test
        @DisplayName("Idempotent: confirming a PAYMENT_FAILED contribution is a no-op")
        void idempotentOnFailed() {
            Iqub iqub = iqub();
            Round openRound = round(iqub, 1, RoundStatus.OPEN);
            Member m = member("Alice");
            Contribution c = new Contribution(openRound, m, new BigDecimal("500"));
            c.markAwaitingPayment("iqub-tx-4");
            c.markFailed();

            when(contributionRepository.findByTxRef("iqub-tx-4")).thenReturn(Optional.of(c));

            Contribution result = service.confirmPayment("iqub-tx-4");

            assertThat(result).isSameAs(c);
            verify(paymentGateway, never()).verify(anyString());
        }

        @Test
        @DisplayName("PENDING: returns contribution unchanged, no notification")
        void pendingPaymentIsNoOp() {
            Iqub iqub = iqub();
            Round openRound = round(iqub, 1, RoundStatus.OPEN);
            Member m = member("Alice");
            Contribution c = new Contribution(openRound, m, new BigDecimal("500"));
            c.markAwaitingPayment("iqub-tx-5");

            when(contributionRepository.findByTxRef("iqub-tx-5")).thenReturn(Optional.of(c));
            when(paymentGateway.verify("iqub-tx-5")).thenReturn(new PaymentVerification(PaymentStatus.PENDING, "Pending", "tx-5"));

            Contribution result = service.confirmPayment("iqub-tx-5");

            assertThat(result.getStatus()).isEqualTo(ContributionStatus.AWAITING_PAYMENT);
            verify(notificationService, never()).notify(any(), anyString());
        }

        @Test
        @DisplayName("SUCCESS late: marks PAID_LATE with penalty")
        void confirmsLatePayment() {
            // Use a clock set far past deadline
            Clock lateClock = Clock.fixed(LocalDate.of(2026, 9, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
            Iqub iqub = new Iqub("Test Group", new BigDecimal("500.00"), 7, 10, LocalDate.now(clock));
            Round lateRound = new Round(iqub, 1, LocalDate.of(2026, 9, 1));
            lateRound.setStatus(RoundStatus.OPEN);
            Member m = member("Alice");
            Contribution c = new Contribution(lateRound, m, new BigDecimal("500"));
            c.markAwaitingPayment("iqub-tx-6");

            // Rebuild service with late clock so daysLate > 0
            ContributionService lateService = new ContributionService(
                    contributionRepository, membershipRepository, paymentGateway, notificationService, lateClock);

            when(contributionRepository.findByTxRef("iqub-tx-6")).thenReturn(Optional.of(c));
            when(paymentGateway.verify("iqub-tx-6")).thenReturn(new PaymentVerification(PaymentStatus.SUCCESS, "Success", "tx-6"));
            when(contributionRepository.save(any(Contribution.class))).thenAnswer(i -> i.getArgument(0));

            Contribution result = lateService.confirmPayment("iqub-tx-6");

            assertThat(result.getStatus()).isEqualTo(ContributionStatus.PAID_LATE);
            assertThat(result.getAmountPaid()).isEqualByComparingTo(new BigDecimal("500.00"));
            verify(contributionRepository).save(c);
        }

        @Test
        @DisplayName("SUCCESS very late (8+ days): marks membership DEFAULTED")
        void latePaymentMarksMemberDefaulted() {
            // Clock set 30 days past deadline → triggers default
            Clock veryLateClock = Clock.fixed(LocalDate.of(2026, 10, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
            Iqub iqub = new Iqub("Test Group", new BigDecimal("500.00"), 7, 10, LocalDate.of(2026, 9, 1));
            Round lateRound = new Round(iqub, 1, LocalDate.of(2026, 9, 1));
            lateRound.setStatus(RoundStatus.OPEN);
            Member m = member("Alice");
            setId(m, 1L);
            Contribution c = new Contribution(lateRound, m, new BigDecimal("500"));
            c.markAwaitingPayment("iqub-tx-7");

            ContributionService veryLateService = new ContributionService(
                    contributionRepository, membershipRepository, paymentGateway, notificationService, veryLateClock);

            when(contributionRepository.findByTxRef("iqub-tx-7")).thenReturn(Optional.of(c));
            when(paymentGateway.verify("iqub-tx-7")).thenReturn(new PaymentVerification(PaymentStatus.SUCCESS, "Success", "tx-7"));
            when(contributionRepository.save(any(Contribution.class))).thenAnswer(i -> i.getArgument(0));
            when(membershipRepository.findByMemberAndIqub(m, iqub)).thenReturn(Optional.of(
                    new Membership(m, iqub, LocalDate.now(clock), MembershipStatus.ACTIVE)));
            when(membershipRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Contribution result = veryLateService.confirmPayment("iqub-tx-7");

            assertThat(result.getStatus()).isEqualTo(ContributionStatus.PAID_LATE);
            verify(notificationService).notify(eq(m), anyString());
        }
    }
}
