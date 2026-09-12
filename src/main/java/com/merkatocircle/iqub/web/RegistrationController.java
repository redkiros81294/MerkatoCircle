package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.exception.DuplicateEmailException;
import com.merkatocircle.iqub.service.MemberService;
import com.merkatocircle.iqub.service.MembershipService;
import com.merkatocircle.iqub.service.TheIqub;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {

    private final MemberService memberService;
    private final MembershipService membershipService;
    private final TheIqub theIqub;
    private final Clock clock;

    public RegistrationController(MemberService memberService, MembershipService membershipService,
                                   TheIqub theIqub, Clock clock) {
        this.memberService = memberService;
        this.membershipService = membershipService;
        this.theIqub = theIqub;
        this.clock = clock;
    }

    @GetMapping("/register")
    public String showForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegistrationForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String submit(@Valid @ModelAttribute("form") RegistrationForm form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {

        if (!form.password.equals(form.confirmPassword)) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords don't match");
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            Member member = memberService.register(form.fullName, form.email, form.phone, form.password);
            Iqub iqub = theIqub.get();
            membershipService.join(iqub, member, LocalDate.now(clock));
        } catch (DuplicateEmailException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "register";
        }

        redirectAttributes.addFlashAttribute("infoMessage", "Account created — log in to continue.");
        return "redirect:/login";
    }

    /** Form-backing object for the registration page. */
    public static class RegistrationForm {

        @NotBlank(message = "Enter your full name")
        private String fullName;

        @NotBlank(message = "Enter your email")
        @Email(message = "Enter a valid email address")
        private String email;

        @NotBlank(message = "Enter your phone number")
        @Pattern(regexp = "^0[79][0-9]{8}$", message = "Use the format 07xxxxxxxx or 09xxxxxxxx")
        private String phone;

        @NotBlank(message = "Choose a password")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotBlank(message = "Confirm your password")
        private String confirmPassword;

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }
}
