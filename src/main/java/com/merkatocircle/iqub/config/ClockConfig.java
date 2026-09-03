package com.merkatocircle.iqub.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The real system clock, wired as a bean so it can be swapped for {@code Clock.fixed(...)}
 * in tests (spec §2). No service in this app should call {@code LocalDate.now()} directly —
 * always {@code LocalDate.now(clock)}.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
