package com.merkatocircle.iqub.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MemberNameTest {

    @Test
    void firstName_withSpace() {
        Member m = new Member("Alice Smith", "alice@test.com", "0712345678", "hash", java.time.LocalDate.now());
        assertThat(m.firstName()).isEqualTo("Alice");
        assertThat(m.lastName()).isEqualTo("Smith");
    }

    @Test
    void firstName_withoutSpace() {
        Member m = new Member("Cher", "cher@test.com", "0712345678", "hash", java.time.LocalDate.now());
        assertThat(m.firstName()).isEqualTo("Cher");
        assertThat(m.lastName()).isEqualTo("");
    }
}
