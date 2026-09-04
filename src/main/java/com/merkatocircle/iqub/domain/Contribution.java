package com.merkatocircle.iqub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contributions")
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountDue;

    @Column(precision = 10, scale = 2)
    private BigDecimal amountPaid;

    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContributionStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal penaltyApplied;

    /** Chapa's transaction reference for the current/most recent payment attempt (spec §4.3). */
    private String txRef;

    protected Contribution() {
        // required by JPA
    }

    public Contribution(Round round, Member member, BigDecimal amountDue) {
        this.round = round;
        this.member = member;
        this.amountDue = amountDue;
        this.status = ContributionStatus.PENDING;
        this.penaltyApplied = BigDecimal.ZERO.setScale(2);
    }

    public Long getId() {
        return id;
    }

    public Round getRound() {
        return round;
    }

    public Member getMember() {
        return member;
    }

    public BigDecimal getAmountDue() {
        return amountDue;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public LocalDate getPaidDate() {
        return paidDate;
    }

    public ContributionStatus getStatus() {
        return status;
    }

    public BigDecimal getPenaltyApplied() {
        return penaltyApplied;
    }

    public String getTxRef() {
        return txRef;
    }

    /** Contribution.initiatePayment transition: PENDING/PAYMENT_FAILED -> AWAITING_PAYMENT. */
    public void markAwaitingPayment(String txRef) {
        this.txRef = txRef;
        this.status = ContributionStatus.AWAITING_PAYMENT;
    }

    /** Contribution.confirmPayment success path: AWAITING_PAYMENT -> PAID or PAID_LATE. */
    public void markPaid(ContributionStatus paidStatus, BigDecimal amountPaid, LocalDate paidDate, BigDecimal penaltyApplied) {
        this.status = paidStatus;
        this.amountPaid = amountPaid;
        this.paidDate = paidDate;
        this.penaltyApplied = penaltyApplied;
    }

    /** Contribution.confirmPayment failure path: AWAITING_PAYMENT -> PAYMENT_FAILED. */
    public void markFailed() {
        this.status = ContributionStatus.PAYMENT_FAILED;
    }

    public boolean isSettled() {
        return status == ContributionStatus.PAID || status == ContributionStatus.PAID_LATE;
    }
}
