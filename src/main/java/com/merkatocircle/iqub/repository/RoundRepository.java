package com.merkatocircle.iqub.repository;

import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Round;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoundRepository extends JpaRepository<Round, Long> {

    List<Round> findByIqubOrderByRoundNumberAsc(Iqub iqub);

    Optional<Round> findTopByIqubOrderByRoundNumberDesc(Iqub iqub);
}
