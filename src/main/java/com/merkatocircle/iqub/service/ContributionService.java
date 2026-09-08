package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.ContributionStatus;
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
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Owns two things: the lateness bands that turn "how late" into a penalty (spec §3.1),
 * and the payment state machine that a real Chapa checkout drives (spec §3.4).
 */
@Service
public class ContributionService {

    private static final int DEFAULT_THRESHOLD_DAYS = 8;

    private final ContributionRepository contributionRepository;
    private final MembershipRepository membershipRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;
    private final Clock clock;

    public ContributionService(ContributionRepository contributionRepository,
                                MembershipRepository membershipRepository,
                                PaymentGateway paymentGateway,
                                NotificationService notificationService,
                                Clock clock) {
        this.contributionRepository = contributionRepository;
        this.membershipRepository = membershipRepository;
        this.paymentGateway = paymentGateway;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /**
     * Pure function implementing spec §3.1. daysLate may legitimately be zero or negative
     * (paid on or before the deadline) — that is the "on time" band, not an error case.
     * This is the single most important method to unit-test: every boundary in §3.1
     * (-1/0/1, 3/4, 7/8, and one large value) should have a matching assertion here.
     */
    public BigDecimal calculatePenalty(long daysLate, BigDecimal amountDue) {
        BigDecimal percent;
        if (daysLate <= 0) {
            percent = BigDecimal.ZERO;
        } else if (daysLate <= 3) {
            percent = new BigDecimal("0.05");
        } else if (daysLate <= 7) {
            percent = new BigDecimal("0.15");
        } else {
            percent = new BigDecimal("0.25");
        }
        return amountDue.multiply(percent).setScale(2, RoundingMode.HALF_UP);
    }

    public Optional<Contribution> findForRoundAndMember(Round round, Member member) {
        return contributionRepository.findByRoundAndMember(round, member);
    }

    /** Every contribution this member has ever had, across every round — for the account overview. */
    public java.util.List<Contribution> findAllForMember(Member member) {
        return contributionRepository.findByMember(member);
    }

    public Contribution findById(Long contributionId) {
        return contributionRepository.findById(contributionId)
                .orElseThrow(() -> new IllegalArgumentException("No contribution with id " + contributionId));
    }

    public Contribution findByTxRef(String txRef) {
        return contributionRepository.findByTxRef(txRef)
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment reference: " + txRef));
    }

    /**
     * Starts a checkout with whichever {@link PaymentGateway} is active (real Chapa or the
     * fake). Generates a fresh tx_ref every attempt, including retries after a failure.
     */
    public PaymentInitiation initiatePayment(Round round, Member member, String callbackUrl, String returnUrl) {
        Contribution contribution = contributionRepository.findByRoundAndMember(round, member)
                .orElseThrow(() -> new IllegalStateException(
                        "No contribution record for member " + member.getId() + " in round " + round.getRoundNumber()));

        if (round.getStatus() == RoundStatus.CLOSED) {
            throw new RoundClosedException("Round " + round.getRoundNumber() + " is already closed");
        }
        if (contribution.isSettled()) {
            throw new AlreadyPaidException(round.getRoundNumber());
        }

        String txRef = "iqub-" + contribution.getId() + "-" + clock.millis();

        PaymentRequest request = new PaymentRequest(
                contribution.getAmountDue(),
                "ETB",
                member.getEmail(),
                member.firstName(),
                member.lastName(),
                member.getPhone(),
                txRef,
                callbackUrl,
                returnUrl
        );

        PaymentInitiation initiation = paymentGateway.initiate(request);

        contribution.markAwaitingPayment(txRef);
        contributionRepository.save(contribution);

        return initiation;
    }

    /**
     * The single method that moves a Contribution out of AWAITING_PAYMENT (spec §3.4).
     * Idempotent by design: called once from the return-page flow and once from Chapa's
     * server-to-server callback, and a second call for an already-settled contribution
     * must be a safe no-op, not a double-penalty or a double-default.
     */
    public Contribution confirmPayment(String txRef) {
        Contribution contribution = contributionRepository.findByTxRef(txRef)
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment reference: " + txRef));

        if (contribution.isSettled() || contribution.getStatus() == ContributionStatus.PAYMENT_FAILED) {
            return contribution;
        }

        PaymentVerification verification = paymentGateway.verify(txRef);

        if (verification.status() == PaymentStatus.SUCCESS) {
            applySuccessfulPayment(contribution);
        } else if (verification.status() == PaymentStatus.FAILED) {
            contribution.markFailed();
            contributionRepository.save(contribution);
            notificationService.notify(contribution.getMember(),
                    "Your payment for round " + contribution.getRound().getRoundNumber() + " didn't go through — nothing was charged, try again when ready.");
        }
        // PENDING: leave the contribution in AWAITING_PAYMENT; the return page can poll again.

        return contribution;
    }

    private void applySuccessfulPayment(Contribution contribution) {
        LocalDate paidDate = LocalDate.now(clock);
        long daysLate = ChronoUnit.DAYS.between(contribution.getRound().getDeadline(), paidDate);
        BigDecimal penalty = calculatePenalty(daysLate, contribution.getAmountDue());
        ContributionStatus status = daysLate <= 0 ? ContributionStatus.PAID : ContributionStatus.PAID_LATE;

        contribution.markPaid(status, contribution.getAmountDue(), paidDate, penalty);
        contributionRepository.save(contribution);

        if (daysLate >= DEFAULT_THRESHOLD_DAYS) {
            membershipRepository.findByMemberAndIqub(contribution.getMember(), contribution.getRound().getIqub())
                    .ifPresent(membership -> {
                        membership.setStatus(MembershipStatus.DEFAULTED);
                        membershipRepository.save(membership);
                        notificationService.notify(contribution.getMember(),
                                "You've been marked defaulted in " + contribution.getRound().getIqub().getName()
                                        + " after paying " + daysLate + " days late.");
                    });
        }
    }
}
