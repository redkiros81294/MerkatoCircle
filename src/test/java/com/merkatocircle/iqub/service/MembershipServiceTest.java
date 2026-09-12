package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.ContributionStatus;
import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Membership;
import com.merkatocircle.iqub.domain.MembershipStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

class MembershipServiceTest {

    private MembershipRepository membershipRepository;
    private RoundRepository roundRepository;
    private ContributionRepository contributionRepository;
    private NotificationService notificationService;
    private Clock clock;
    private MembershipService service;

    @BeforeEach
    void setUp() {
        membershipRepository = mock(MembershipRepository.class);
        roundRepository = mock(RoundRepository.class);
        contributionRepository = mock(ContributionRepository.class);
        notificationService = mock(NotificationService.class);
        clock = Clock.fixed(LocalDate.of(2026, 9, 3).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new MembershipService(membershipRepository, roundRepository, contributionRepository, notificationService, clock);
    }

    private Iqub iqub(int maxMembers) {
        return new Iqub("Group", new BigDecimal("500.00"), 7, maxMembers, LocalDate.now(clock));
    }

    private Member member(String name) {
        return new Member(name, name.toLowerCase().replace(" ", ".") + "@example.com", "0712345678", "hash", LocalDate.now(clock));
    }

    @Test
    @DisplayName("join: ACTIVE when under maxMembers, WAITLISTED when at capacity")
    void joinAssignsCorrectStatus() {
        Iqub small = iqub(2); // max 2
        Member m1 = member("Alice");
        Member m2 = member("Bob");
        Member m3 = member("Charlie");

        when(membershipRepository.countByIqubAndStatus(small, MembershipStatus.ACTIVE)).thenReturn(0L, 1L, 2L);
        when(membershipRepository.save(any(Membership.class))).thenAnswer(i -> i.getArgument(0));

        Membership first = service.join(small, m1, LocalDate.now(clock));
        Membership second = service.join(small, m2, LocalDate.now(clock));
        Membership third = service.join(small, m3, LocalDate.now(clock));

        assertThat(first.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(second.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(third.getStatus()).isEqualTo(MembershipStatus.WAITLISTED);
    }

    @Test
    @DisplayName("removeMember: removing ACTIVE member triggers waitlist promotion")
    void removeActivePromotesWaitlisted() {
        Iqub group = iqub(2);
        Member alice = member("Alice");
        Member bob = member("Bob");

        Membership activeAlice = new Membership(alice, group, LocalDate.now(clock), MembershipStatus.ACTIVE);
        Membership waitlistedBob = new Membership(bob, group, LocalDate.now(clock), MembershipStatus.WAITLISTED);

        when(membershipRepository.findByIqubAndStatus(group, MembershipStatus.WAITLISTED))
                .thenReturn(List.of(waitlistedBob));
        when(membershipRepository.save(any(Membership.class))).thenAnswer(i -> i.getArgument(0));

        service.removeMember(activeAlice);

        verify(membershipRepository).delete(activeAlice);
        assertThat(waitlistedBob.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }
}
