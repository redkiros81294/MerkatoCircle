package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.ContributionStatus;
import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Membership;
import com.merkatocircle.iqub.domain.MembershipStatus;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.domain.RoundStatus;
import com.merkatocircle.iqub.repository.ContributionRepository;
import com.merkatocircle.iqub.repository.MembershipRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

class EligibilityCheckerTest {

    private ContributionRepository contributionRepository;
    private MembershipRepository membershipRepository;
    private EligibilityChecker checker;

    @BeforeEach
    void setUp() {
        contributionRepository = mock(ContributionRepository.class);
        membershipRepository = mock(MembershipRepository.class);
        checker = new EligibilityChecker(contributionRepository, membershipRepository);
    }

    private Member member(String name) {
        Member m = new Member(name, name.toLowerCase().replace(" ", ".") + "@example.com", "0712345678", "hash", LocalDate.now());
        return m;
    }

    private Iqub iqub() {
        return new Iqub("Group", new BigDecimal("500.00"), 7, 10, LocalDate.now());
    }

    private Round round(Iqub iqub, int number) {
        return new Round(iqub, number, LocalDate.now().plusDays(3));
    }

    private Membership membership(Member m, Iqub iqub, MembershipStatus status, boolean won) {
        Membership ms = new Membership(m, iqub, LocalDate.now(), status);
        ms.setHasReceivedPayoutThisCycle(won);
        return ms;
    }

    private Contribution contribution(Round r, Member m, ContributionStatus status) {
        Contribution c = new Contribution(r, m, new BigDecimal("500"));
        if (status == ContributionStatus.PAID || status == ContributionStatus.PAID_LATE) {
            c.markPaid(status, new BigDecimal("500"), LocalDate.now(), BigDecimal.ZERO);
        }
        return c;
    }

    @Test
    @DisplayName("Decision table: all three conditions must be met for eligibility")
    void decisionTableAllConditions() {
        Iqub iqub = iqub();
        Round r = round(iqub, 1);
        Member alice = member("Alice");

        // Case: C1=PAID, C2=false, C3=ACTIVE → eligible
        when(contributionRepository.findByRound(r)).thenReturn(List.of(contribution(r, alice, ContributionStatus.PAID)));
        when(membershipRepository.findByMemberAndIqub(alice, iqub)).thenReturn(Optional.of(membership(alice, iqub, MembershipStatus.ACTIVE, false)));
        assertThat(checker.getEligibleMembers(r)).containsExactly(alice);

        // Case: C1=PENDING → not eligible
        when(contributionRepository.findByRound(r)).thenReturn(List.of(contribution(r, alice, ContributionStatus.PENDING)));
        assertThat(checker.getEligibleMembers(r)).isEmpty();

        // Case: C2=true (already won) → not eligible
        when(contributionRepository.findByRound(r)).thenReturn(List.of(contribution(r, alice, ContributionStatus.PAID)));
        when(membershipRepository.findByMemberAndIqub(alice, iqub)).thenReturn(Optional.of(membership(alice, iqub, MembershipStatus.ACTIVE, true)));
        assertThat(checker.getEligibleMembers(r)).isEmpty();

        // Case: C3=DEFAULTED → not eligible
        when(membershipRepository.findByMemberAndIqub(alice, iqub)).thenReturn(Optional.of(membership(alice, iqub, MembershipStatus.DEFAULTED, false)));
        assertThat(checker.getEligibleMembers(r)).isEmpty();
    }

    @Test
    @DisplayName("Eligible list contains only members satisfying all three conditions")
    void filtersEligibleMembers() {
        Iqub iqub = iqub();
        Round r = round(iqub, 1);
        Member alice = member("Alice");
        Member bob = member("Bob");

        Contribution alicePaid = contribution(r, alice, ContributionStatus.PAID);
        Contribution bobPending = contribution(r, bob, ContributionStatus.PENDING);

        when(contributionRepository.findByRound(r)).thenReturn(List.of(alicePaid, bobPending));
        when(membershipRepository.findByMemberAndIqub(alice, iqub)).thenReturn(Optional.of(membership(alice, iqub, MembershipStatus.ACTIVE, false)));
        when(membershipRepository.findByMemberAndIqub(bob, iqub)).thenReturn(Optional.of(membership(bob, iqub, MembershipStatus.ACTIVE, false)));

        List<Member> eligible = checker.getEligibleMembers(r);
        assertThat(eligible).containsExactly(alice);
        assertThat(eligible).doesNotContain(bob);
    }
}
