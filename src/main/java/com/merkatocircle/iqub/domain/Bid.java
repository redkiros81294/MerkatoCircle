package com.merkatocircle.iqub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An eligible member's offer to take a discounted payout in exchange for winning this round
 * (spec §3.7). At most one bid per (round, member) — enforced both here and in BidService.
 */
@Entity
@Table(name = "bids", uniqueConstraints = @UniqueConstraint(columnNames = {"round_id", "member_id"}))
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** Percentage of the pool the bidder is willing to forgo, 0-30 inclusive (spec §3.7). */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(nullable = false)
    private LocalDate submittedDate;

    protected Bid() {
        // required by JPA
    }

    public Bid(Round round, Member member, BigDecimal discountPercent, LocalDate submittedDate) {
        this.round = round;
        this.member = member;
        this.discountPercent = discountPercent;
        this.submittedDate = submittedDate;
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

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public LocalDate getSubmittedDate() {
        return submittedDate;
    }

    /** Lets a member revise their offer before the round closes — same row, fresh values. */
    public void updateOffer(BigDecimal discountPercent, LocalDate submittedDate) {
        this.discountPercent = discountPercent;
        this.submittedDate = submittedDate;
    }
}
