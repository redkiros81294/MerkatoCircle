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
@Table(name = "iqubs")
public class Iqub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal contributionAmount;

    @Column(nullable = false)
    private int roundIntervalDays;

    @Column(nullable = false)
    private int maxMembers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutMode payoutMode;

    /** Tier 2 field — the group's creator/organizer. Null is fine for Tier 1's single seeded group. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id")
    private Member organizer;

    @Column(nullable = false)
    private LocalDate createdDate;

    protected Iqub() {
        // required by JPA
    }

    public Iqub(String name, BigDecimal contributionAmount, int roundIntervalDays,
                int maxMembers, LocalDate createdDate) {
        this.name = name;
        this.contributionAmount = contributionAmount;
        this.roundIntervalDays = roundIntervalDays;
        this.maxMembers = maxMembers;
        this.createdDate = createdDate;
        this.payoutMode = PayoutMode.LOTTERY;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getContributionAmount() {
        return contributionAmount;
    }

    public int getRoundIntervalDays() {
        return roundIntervalDays;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public PayoutMode getPayoutMode() {
        return payoutMode;
    }

    public void setPayoutMode(PayoutMode payoutMode) {
        this.payoutMode = payoutMode;
    }

    public Member getOrganizer() {
        return organizer;
    }

    public void setOrganizer(Member organizer) {
        this.organizer = organizer;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }
}
