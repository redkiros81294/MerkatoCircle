package com.merkatocircle.iqub.config;

import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Membership;
import com.merkatocircle.iqub.domain.MembershipStatus;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.repository.ContributionRepository;
import com.merkatocircle.iqub.repository.IqubRepository;
import com.merkatocircle.iqub.repository.MemberRepository;
import com.merkatocircle.iqub.repository.MembershipRepository;
import com.merkatocircle.iqub.repository.RoundRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSeederTest {

    private MemberRepository memberRepository;
    private IqubRepository iqubRepository;
    private MembershipRepository membershipRepository;
    private RoundRepository roundRepository;
    private ContributionRepository contributionRepository;
    private PasswordEncoder passwordEncoder;
    private Clock clock;
    private DataSeeder seeder;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        iqubRepository = mock(IqubRepository.class);
        membershipRepository = mock(MembershipRepository.class);
        roundRepository = mock(RoundRepository.class);
        contributionRepository = mock(ContributionRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        clock = Clock.fixed(LocalDate.of(2026, 9, 3).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        seeder = new DataSeeder(memberRepository, iqubRepository, membershipRepository,
                roundRepository, contributionRepository, passwordEncoder, clock);
    }

    @Test
    void run_alreadySeeded_doesNothing() {
        when(memberRepository.count()).thenReturn(5L);

        seeder.run();

        verify(memberRepository).count();
        verify(memberRepository, never()).save(any());
    }

    @Test
    void run_seedsDatabase() {
        when(memberRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED_HASH");
        when(memberRepository.save(any(Member.class))).thenAnswer(i -> {
            Member m = i.getArgument(0);
            setField(m, "id", 1L);
            return m;
        });
        when(iqubRepository.save(any(Iqub.class))).thenAnswer(i -> {
            Iqub iqub = i.getArgument(0);
            setField(iqub, "id", 1L);
            return iqub;
        });
        when(membershipRepository.save(any(Membership.class))).thenAnswer(i -> i.getArgument(0));
        when(roundRepository.save(any(Round.class))).thenAnswer(i -> i.getArgument(0));
        when(contributionRepository.save(any(Contribution.class))).thenAnswer(i -> i.getArgument(0));

        seeder.run();

        verify(memberRepository).count();
        verify(memberRepository, org.mockito.Mockito.times(5)).save(any(Member.class));
        verify(iqubRepository, org.mockito.Mockito.times(2)).save(any(Iqub.class));
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
