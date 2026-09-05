package com.merkatocircle.iqub.repository;

import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Membership;
import com.merkatocircle.iqub.domain.MembershipStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    Optional<Membership> findByMemberAndIqub(Member member, Iqub iqub);

    List<Membership> findByIqub(Iqub iqub);

    /** Stable ordering so the dashboard's rotation wheel doesn't reshuffle on every page load. */
    List<Membership> findByIqubOrderById(Iqub iqub);

    List<Membership> findByIqubAndStatus(Iqub iqub, MembershipStatus status);

    List<Membership> findByMember(Member member);

    long countByIqubAndStatus(Iqub iqub, MembershipStatus status);
}
