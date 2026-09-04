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
import java.time.LocalDate;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private LocalDate createdDate;

    @Column(nullable = false)
    private boolean read;

    protected Notification() {
        // required by JPA
    }

    public Notification(Member member, String message, LocalDate createdDate) {
        this.member = member;
        this.message = message;
        this.createdDate = createdDate;
        this.read = false;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public String getMessage() {
        return message;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public boolean isRead() {
        return read;
    }

    public void markRead() {
        this.read = true;
    }
}
