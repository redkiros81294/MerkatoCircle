package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.exception.DuplicateEmailException;
import com.merkatocircle.iqub.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

class MemberServiceTest {

    private MemberRepository memberRepository;
    private PasswordEncoder passwordEncoder;
    private Clock clock;
    private MemberService service;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        clock = Clock.fixed(LocalDate.of(2026, 9, 3).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new MemberService(memberRepository, passwordEncoder, clock);
    }

    @Test
    @DisplayName("Registration rejects duplicate email")
    void rejectsDuplicateEmail() {
        when(memberRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register("Test", "dup@example.com", "0712345678", "password123"))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("dup@example.com");
    }

    @Test
    @DisplayName("Registration creates member with encoded password")
    void createsMemberWithEncodedPassword() {
        when(memberRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("ENCODED_HASH");
        when(memberRepository.save(any(Member.class))).thenAnswer(i -> i.getArgument(0));

        Member m = service.register("Test User", "new@example.com", "0712345678", "password123");

        assertThat(m.getFullName()).isEqualTo("Test User");
        assertThat(m.getEmail()).isEqualTo("new@example.com");
        assertThat(m.getPhone()).isEqualTo("0712345678");
        assertThat(m.getPasswordHash()).isEqualTo("ENCODED_HASH");
        assertThat(m.getPlatformRole()).isEqualTo(com.merkatocircle.iqub.domain.PlatformRole.MEMBER);
        verify(memberRepository).save(m);
    }
}
