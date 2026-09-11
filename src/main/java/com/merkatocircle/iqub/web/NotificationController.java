package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.service.CurrentMemberProvider;
import com.merkatocircle.iqub.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentMemberProvider currentMemberProvider;

    public NotificationController(NotificationService notificationService,
                                   CurrentMemberProvider currentMemberProvider) {
        this.notificationService = notificationService;
        this.currentMemberProvider = currentMemberProvider;
    }

    @GetMapping("/notifications")
    public String list(Authentication authentication, Model model) {
        Member member = currentMemberProvider.get(authentication);
        model.addAttribute("notifications", notificationService.recentFor(member));
        return "notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllRead(Authentication authentication) {
        Member member = currentMemberProvider.get(authentication);
        notificationService.markAllRead(member);
        return "redirect:/notifications";
    }
}