package com.merkatocircle.iqub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/login", "/register", "/css/**",
                        // Chapa's own server calls this one — it will never carry our session.
                        "/payments/chapa/callback",
                        // the member's browser lands here straight from Chapa, and the fake
                        // checkout stands in for a page that, in production, is not ours at all.
                        "/payments/return", "/test/fake-checkout/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?loggedOut")
                .permitAll()
            );
        // Chapa's callback is documented as a plain GET (spec §4.3), and Spring Security's
        // CSRF filter only ever intercepts unsafe methods (POST/PUT/PATCH/DELETE), so the
        // callback needs no CSRF exemption — default CSRF protection stays on for everything
        // this app itself renders forms for.

        return http.build();
    }
}
