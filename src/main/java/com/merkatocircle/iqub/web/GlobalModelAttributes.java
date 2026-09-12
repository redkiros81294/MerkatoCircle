package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.service.CurrentMemberProvider;
import com.merkatocircle.iqub.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Makes the logged-in Member available as "me" on every page the same way, so the shared
 * layout fragment (the top bar's avatar initial, the notification badge) works regardless
 * of which controller rendered the page, without every controller remembering to add it.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final CurrentMemberProvider currentMemberProvider;
    private final NotificationService notificationService;

    public GlobalModelAttributes(CurrentMemberProvider currentMemberProvider, NotificationService notificationService) {
        this.currentMemberProvider = currentMemberProvider;
        this.notificationService = notificationService;
    }

    @ModelAttribute("me")
    public Member currentMember(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return currentMemberProvider.get(authentication);
    }

    @ModelAttribute("unreadNotifications")
    public long unreadNotifications(Authentication authentication) {
        Member me = currentMember(authentication);
        return me == null ? 0 : notificationService.unreadCount(me);
    }
}

