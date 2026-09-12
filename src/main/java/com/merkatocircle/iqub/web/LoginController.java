package com.merkatocircle.iqub.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                         @RequestParam(value = "loggedOut", required = false) String loggedOut,
                         org.springframework.ui.Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Incorrect email or password.");
        }
        if (loggedOut != null) {
            model.addAttribute("infoMessage", "You've been logged out.");
        }
        return "login";
    }
}
