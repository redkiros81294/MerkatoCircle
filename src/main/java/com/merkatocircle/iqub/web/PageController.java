package com.merkatocircle.iqub.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Entry points that don't belong to any one feature: the root redirect and the login page. */
@Controller
public class PageController {

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    /** SecurityConfig points formLogin at this path; it just needs a view to render. */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}