package com.merkatocircle.iqub.repository;

import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByMemberOrderByCreatedDateDesc(Member member);

    long countByMemberAndReadFalse(Member member);
}
