package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.repository.MemberRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Bridges our Member entity to Spring Security. Email is the username. */
@Service
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    public MemberUserDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));

        String role = "ROLE_" + member.getPlatformRole().name();
        return new User(member.getEmail(), member.getPasswordHash(), List.of(new SimpleGrantedAuthority(role)));
    }
}
