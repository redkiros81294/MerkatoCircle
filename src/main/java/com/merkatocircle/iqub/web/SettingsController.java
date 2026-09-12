package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.service.CurrentMemberProvider;
import com.merkatocircle.iqub.service.MemberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SettingsController {

    private final CurrentMemberProvider currentMemberProvider;
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;

    public SettingsController(CurrentMemberProvider currentMemberProvider, MemberService memberService,
                               PasswordEncoder passwordEncoder) {
        this.currentMemberProvider = currentMemberProvider;
        this.memberService = memberService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/settings")
    public String show(Authentication authentication, Model model) {
        Member me = currentMemberProvider.get(authentication);
        ensureProfileForm(model, me);
        ensurePasswordForm(model);
        return "settings";
    }

    @PostMapping("/settings/profile")
    public String updateProfile(Authentication authentication,
                                 @Valid @ModelAttribute("profileForm") ProfileForm form,
                                 BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        Member me = currentMemberProvider.get(authentication);
        if (result.hasErrors()) {
            ensurePasswordForm(model);
            return "settings";
        }
        memberService.updateProfile(me, form.getFullName(), form.getPhone());
        redirectAttributes.addFlashAttribute("infoMessage", "Profile updated.");
        return "redirect:/settings";
    }

    @PostMapping("/settings/password")
    public String updatePassword(Authentication authentication,
                                  @Valid @ModelAttribute("passwordForm") PasswordForm form,
                                  BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        Member me = currentMemberProvider.get(authentication);

        if (!result.hasErrors() && !passwordEncoder.matches(form.getCurrentPassword(), me.getPasswordHash())) {
            result.rejectValue("currentPassword", "mismatch", "Current password is incorrect");
        }
        if (result.hasErrors()) {
            ensureProfileForm(model, me);
            return "settings";
        }
        memberService.changePassword(me, form.getNewPassword());
        redirectAttributes.addFlashAttribute("infoMessage", "Password updated.");
        return "redirect:/settings";
    }

    private void ensureProfileForm(Model model, Member me) {
        if (!model.containsAttribute("profileForm")) {
            ProfileForm form = new ProfileForm();
            form.setFullName(me.getFullName());
            form.setPhone(me.getPhone());
            model.addAttribute("profileForm", form);
        }
    }

    private void ensurePasswordForm(Model model) {
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordForm());
        }
    }

    public static class ProfileForm {

        @NotBlank(message = "Enter your full name")
        private String fullName;

        @NotBlank(message = "Enter your phone number")
        @Pattern(regexp = "^0[79][0-9]{8}$", message = "Use the format 07xxxxxxxx or 09xxxxxxxx")
        private String phone;

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    public static class PasswordForm {

        @NotBlank(message = "Enter your current password")
        private String currentPassword;

        @NotBlank(message = "Enter a new password")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String newPassword;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
