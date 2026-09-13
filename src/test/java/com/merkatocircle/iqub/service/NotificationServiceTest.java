package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Notification;
import com.merkatocircle.iqub.repository.NotificationRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final Clock clock = Clock.fixed(LocalDate.of(2026, 9, 3).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    private final NotificationService service = new NotificationService(notificationRepository, clock);

    private Member member(String name) {
        return new Member(name, name.toLowerCase() + "@example.com", "0712345678", "hash", LocalDate.now(clock));
    }

    @Test
    void markAllRead_onlyUnreadsRead() {
        Member m = member("Alice");
        Notification unread1 = new Notification(m, "msg1", LocalDate.now(clock));
        Notification read = new Notification(m, "msg2", LocalDate.now(clock));
        read.markRead();
        Notification unread2 = new Notification(m, "msg3", LocalDate.now(clock));

        when(notificationRepository.findByMemberOrderByCreatedDateDesc(m))
                .thenReturn(List.of(unread1, read, unread2));

        service.markAllRead(m);

        verify(notificationRepository).save(unread1);
        verify(notificationRepository).save(unread2);
        verify(notificationRepository, never()).save(read);
    }

    @Test
    void markAllRead_emptyList_doesNothing() {
        Member m = member("Alice");
        when(notificationRepository.findByMemberOrderByCreatedDateDesc(m))
                .thenReturn(List.of());

        service.markAllRead(m);

        verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unreadCount_delegatesToRepository() {
        Member m = member("Alice");
        when(notificationRepository.countByMemberAndReadFalse(m)).thenReturn(5L);

        long count = service.unreadCount(m);

        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(5L);
    }
}
