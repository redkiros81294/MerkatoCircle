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
@Table(name = "rounds")
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iqub_id", nullable = false)
    private Iqub iqub;

    @Column(nullable = false)
    private int roundNumber;

    @Column(nullable = false)
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_member_id")
    private Member winner;

    @Column(precision = 10, scale = 2)
    private BigDecimal payoutAmount;

    private LocalDate payoutDate;

    protected Round() {
        // required by JPA
    }

    public Round(Iqub iqub, int roundNumber, LocalDate deadline) {
        this.iqub = iqub;
        this.roundNumber = roundNumber;
        this.deadline = deadline;
        this.status = RoundStatus.OPEN;
    }

    public Long getId() {
        return id;
    }

    public Iqub getIqub() {
        return iqub;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public RoundStatus getStatus() {
        return status;
    }

    public void setStatus(RoundStatus status) {
        this.status = status;
    }

    public Member getWinner() {
        return winner;
    }

    public BigDecimal getPayoutAmount() {
        return payoutAmount;
    }

    public LocalDate getPayoutDate() {
        return payoutDate;
    }

    /** Called once, by RoundService.runDraw, when the round closes (spec §3.3 / §4.4). */
    public void closeWithWinner(Member winner, BigDecimal payoutAmount, LocalDate payoutDate) {
        this.winner = winner;
        this.payoutAmount = payoutAmount;
        this.payoutDate = payoutDate;
        this.status = RoundStatus.CLOSED;
    }
}
