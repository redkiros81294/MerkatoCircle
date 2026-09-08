package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.repository.MemberRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** One place for "who is logged in right now", instead of repeating the lookup in every controller. */
@Component
public class CurrentMemberProvider {

    private final MemberRepository memberRepository;

    public CurrentMemberProvider(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member get(Authentication authentication) {
        return memberRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated as " + authentication.getName() + " but no matching Member exists"));
    }
}
