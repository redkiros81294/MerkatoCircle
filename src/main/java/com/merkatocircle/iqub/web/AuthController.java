package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.service.MemberService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Self-service account creation. SecurityConfig permits both "/register" endpoints without
 * authentication. A duplicate email throws DuplicateEmailException, which
 * GlobalExceptionHandler turns into a flash-messaged redirect back to this form.
 */
@Controller
public class AuthController {

    private final MemberService memberService;

    public AuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/register")
    public String showForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName,
                            @RequestParam String email,
                            @RequestParam String phone,
                            @RequestParam String password) {
        memberService.register(fullName, email, phone, password);
        return "redirect:/login?registered";
    }
}