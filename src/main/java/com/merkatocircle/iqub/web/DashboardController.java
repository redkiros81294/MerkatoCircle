package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Membership;
import com.merkatocircle.iqub.repository.IqubRepository;
import com.merkatocircle.iqub.repository.MembershipRepository;
import com.merkatocircle.iqub.service.CurrentMemberProvider;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * "My Groups" — every circle the member already belongs to, plus any group on the platform
 * they haven't joined yet. A member's groups are found through Membership rather than by
 * assuming there's exactly one Iqub, since MembershipService already supports many.
 */
@Controller
public class DashboardController {

    private final MembershipRepository membershipRepository;
    private final IqubRepository iqubRepository;
    private final CurrentMemberProvider currentMemberProvider;

    public DashboardController(MembershipRepository membershipRepository,
                                IqubRepository iqubRepository,
                                CurrentMemberProvider currentMemberProvider) {
        this.membershipRepository = membershipRepository;
        this.iqubRepository = iqubRepository;
        this.currentMemberProvider = currentMemberProvider;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        Member member = currentMemberProvider.get(authentication);
        List<Membership> memberships = membershipRepository.findByMember(member);

        List<Long> joinedIqubIds = memberships.stream().map(m -> m.getIqub().getId()).toList();
        List<Iqub> availableToJoin = iqubRepository.findAll().stream()
                .filter(iqub -> !joinedIqubIds.contains(iqub.getId()))
                .toList();

        model.addAttribute("member", member);
        model.addAttribute("memberships", memberships);
        model.addAttribute("availableToJoin", availableToJoin);
        return "dashboard";
    }
}