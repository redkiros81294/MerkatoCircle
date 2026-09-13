package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Member;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RandomWinnerSelectorTest {

    private final RandomWinnerSelector selector = new RandomWinnerSelector();

    private Member member(String name) {
        return new Member(name, name.toLowerCase() + "@example.com", "0712345678", "hash", LocalDate.now());
    }

    @Test
    void select_singleMember() {
        Member alice = member("Alice");
        Member selected = selector.select(List.of(alice));
        assertThat(selected).isEqualTo(alice);
    }

    @Test
    void select_multipleMembers_returnsOneOfThem() {
        Member alice = member("Alice");
        Member bob = member("Bob");
        Member selected = selector.select(List.of(alice, bob));
        assertThat(List.of(alice, bob)).contains(selected);
    }

    @Test
    void select_emptyList_throws() {
        assertThatThrownBy(() -> selector.select(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty pool");
    }
}
