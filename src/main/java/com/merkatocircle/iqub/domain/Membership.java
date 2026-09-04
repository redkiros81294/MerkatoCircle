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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/**
 * Member &lt;-&gt; Iqub, one row per (member, group) pair. hasReceivedPayoutThisCycle lives here
 * — not on Member — because it is scoped to one group; the same person can be mid-cycle in
 * one circle and freshly rotated in another (Tier 2).
 */
@Entity
@Table(name = "memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "iqub_id"}))
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iqub_id", nullable = false)
    private Iqub iqub;

    @Column(nullable = false)
    private LocalDate joinedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status;

    @Column(nullable = false)
    private boolean hasReceivedPayoutThisCycle;

    protected Membership() {
        // required by JPA
    }

    public Membership(Member member, Iqub iqub, LocalDate joinedDate, MembershipStatus status) {
        this.member = member;
        this.iqub = iqub;
        this.joinedDate = joinedDate;
        this.status = status;
        this.hasReceivedPayoutThisCycle = false;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public Iqub getIqub() {
        return iqub;
    }

    public LocalDate getJoinedDate() {
        return joinedDate;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public void setStatus(MembershipStatus status) {
        this.status = status;
    }

    public boolean isHasReceivedPayoutThisCycle() {
        return hasReceivedPayoutThisCycle;
    }

    public void setHasReceivedPayoutThisCycle(boolean hasReceivedPayoutThisCycle) {
        this.hasReceivedPayoutThisCycle = hasReceivedPayoutThisCycle;
    }
}
