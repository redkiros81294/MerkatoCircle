package com.merkatocircle.iqub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    /** Kept in Chapa's expected shape: 07xxxxxxxx or 09xxxxxxxx (spec §5, register.html). */
    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlatformRole platformRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GlobalStatus globalStatus;

    @Column(nullable = false)
    private LocalDate joinedDate;

    protected Member() {
        // required by JPA
    }

    public Member(String fullName, String email, String phone, String passwordHash, LocalDate joinedDate) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.joinedDate = joinedDate;
        this.platformRole = PlatformRole.MEMBER;
        this.globalStatus = GlobalStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public PlatformRole getPlatformRole() {
        return platformRole;
    }

    public void setPlatformRole(PlatformRole platformRole) {
        this.platformRole = platformRole;
    }

    public GlobalStatus getGlobalStatus() {
        return globalStatus;
    }

    public void setGlobalStatus(GlobalStatus globalStatus) {
        this.globalStatus = globalStatus;
    }

    public LocalDate getJoinedDate() {
        return joinedDate;
    }

    /** First name, used as the wheel/avatar initial on the dashboard. */
    public String firstName() {
        int space = fullName.indexOf(' ');
        return space > 0 ? fullName.substring(0, space) : fullName;
    }

    /** Everything after the first space — Chapa's checkout wants first/last name separately. */
    public String lastName() {
        int space = fullName.indexOf(' ');
        return space > 0 ? fullName.substring(space + 1).trim() : "";
    }
}
