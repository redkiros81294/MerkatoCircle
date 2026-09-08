package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.exception.DuplicateEmailException;
import com.merkatocircle.iqub.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public Member register(String fullName, String email, String phone, String rawPassword) {
        if (memberRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        String hash = passwordEncoder.encode(rawPassword);
        LocalDate today = LocalDate.now(clock);
        Member member = new Member(fullName, email, phone, hash, today);
        return memberRepository.save(member);
    }

    public void updateProfile(Member member, String fullName, String phone) {
        member.setFullName(fullName);
        member.setPhone(phone);
        memberRepository.save(member);
    }

    public void changePassword(Member member, String rawNewPassword) {
        member.setPasswordHash(passwordEncoder.encode(rawNewPassword));
        memberRepository.save(member);
    }
}
