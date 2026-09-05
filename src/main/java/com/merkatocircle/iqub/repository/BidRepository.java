package com.merkatocircle.iqub.repository;

import com.merkatocircle.iqub.domain.Bid;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Round;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByRound(Round round);

    Optional<Bid> findByRoundAndMember(Round round, Member member);
}
