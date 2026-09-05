package com.merkatocircle.iqub.repository;

import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Round;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributionRepository extends JpaRepository<Contribution, Long> {

    Optional<Contribution> findByRoundAndMember(Round round, Member member);

    List<Contribution> findByRound(Round round);

    List<Contribution> findByMember(Member member);

    Optional<Contribution> findByTxRef(String txRef);
}
