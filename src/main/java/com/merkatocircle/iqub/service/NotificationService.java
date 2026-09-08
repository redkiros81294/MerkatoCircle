package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Notification;
import com.merkatocircle.iqub.repository.NotificationRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    public NotificationService(NotificationRepository notificationRepository, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
    }

    public void notify(Member member, String message) {
        notificationRepository.save(new Notification(member, message, LocalDate.now(clock)));
    }

    public List<Notification> recentFor(Member member) {
        return notificationRepository.findByMemberOrderByCreatedDateDesc(member);
    }

    public long unreadCount(Member member) {
        return notificationRepository.countByMemberAndReadFalse(member);
    }

    public void markAllRead(Member member) {
        List<Notification> all = notificationRepository.findByMemberOrderByCreatedDateDesc(member);
        for (Notification n : all) {
            if (!n.isRead()) {
                n.markRead();
                notificationRepository.save(n);
            }
        }
    }
}
