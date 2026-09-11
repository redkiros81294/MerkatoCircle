package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.service.ContributionService;
import com.merkatocircle.iqub.service.CurrentMemberProvider;
import com.merkatocircle.iqub.service.MemberService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {

    private final MemberService memberService;
    private final ContributionService contributionService;
    private final CurrentMemberProvider currentMemberProvider;

    public AccountController(MemberService memberService,
                              ContributionService contributionService,
                              CurrentMemberProvider currentMemberProvider) {
        this.memberService = memberService;
        this.contributionService = contributionService;
        this.currentMemberProvider = currentMemberProvider;
    }

    @GetMapping("/account")
    public String show(Authentication authentication, Model model) {
        Member member = currentMemberProvider.get(authentication);
        model.addAttribute("member", member);
        model.addAttribute("contributions", contributionService.findAllForMember(member));
        return "account";
    }

    @PostMapping("/account")
    public String updateProfile(Authentication authentication,
                                 @RequestParam String fullName,
                                 @RequestParam String phone,
                                 RedirectAttributes redirectAttributes) {
        Member member = currentMemberProvider.get(authentication);
        memberService.updateProfile(member, fullName, phone);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated.");
        return "redirect:/account";
    }

    @PostMapping("/account/password")
    public String changePassword(Authentication authentication,
                                  @RequestParam String newPassword,
                                  RedirectAttributes redirectAttributes) {
        Member member = currentMemberProvider.get(authentication);
        memberService.changePassword(member, newPassword);
        redirectAttributes.addFlashAttribute("successMessage", "Password changed.");
        return "redirect:/account";
    }
}