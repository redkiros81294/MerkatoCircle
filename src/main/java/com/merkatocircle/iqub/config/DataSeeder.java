package com.merkatocircle.iqub.config;

import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.ContributionStatus;
import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Membership;
import com.merkatocircle.iqub.domain.MembershipStatus;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.repository.ContributionRepository;
import com.merkatocircle.iqub.repository.IqubRepository;
import com.merkatocircle.iqub.repository.MemberRepository;
import com.merkatocircle.iqub.repository.MembershipRepository;
import com.merkatocircle.iqub.repository.RoundRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds exactly the scenario the mockup (iqub-mockup.html) already shows, so the first time
 * you run this for real it tells the same story: two closed rounds, a live third round with
 * three of five paid, and a full spread of contribution states to look at.
 *
 * <p>A CommandLineRunner is used instead of {@code data.sql} for one reason: password hashes
 * need a real {@link PasswordEncoder} call, which data.sql has no way to do correctly.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String SEED_PASSWORD = "password123";

    private final MemberRepository memberRepository;
    private final IqubRepository iqubRepository;
    private final MembershipRepository membershipRepository;
    private final RoundRepository roundRepository;
    private final ContributionRepository contributionRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DataSeeder(MemberRepository memberRepository, IqubRepository iqubRepository,
                       MembershipRepository membershipRepository, RoundRepository roundRepository,
                       ContributionRepository contributionRepository, PasswordEncoder passwordEncoder,
                       Clock clock) {
        this.memberRepository = memberRepository;
        this.iqubRepository = iqubRepository;
        this.membershipRepository = membershipRepository;
        this.roundRepository = roundRepository;
        this.contributionRepository = contributionRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (memberRepository.count() > 0) {
            return; // already seeded
        }

        LocalDate today = LocalDate.now(clock);
        String hash = passwordEncoder.encode(SEED_PASSWORD);

        Member selam = memberRepository.save(new Member("Selam Tesfaye", "selam@merkatocircle.et", "0911223344", hash, today.minusDays(40)));
        Member abel = memberRepository.save(new Member("Abel Getachew", "abel@merkatocircle.et", "0911223345", hash, today.minusDays(40)));
        Member marta = memberRepository.save(new Member("Marta Alemu", "marta@merkatocircle.et", "0911223346", hash, today.minusDays(40)));
        Member yonas = memberRepository.save(new Member("Yonas Bekele", "yonas@merkatocircle.et", "0911223347", hash, today.minusDays(40)));
        Member betty = memberRepository.save(new Member("Betty Wolde", "betty@merkatocircle.et", "0911223348", hash, today.minusDays(40)));

        Iqub iqub = iqubRepository.save(
                new Iqub("Merkato Circle", new BigDecimal("500.00"), 7, 20, today.minusDays(40)));
        iqub.setOrganizer(selam);
        iqubRepository.save(iqub);

        Membership mSelam = membershipRepository.save(new Membership(selam, iqub, today.minusDays(40), MembershipStatus.ACTIVE));
        Membership mAbel = membershipRepository.save(new Membership(abel, iqub, today.minusDays(40), MembershipStatus.ACTIVE));
        Membership mMarta = membershipRepository.save(new Membership(marta, iqub, today.minusDays(40), MembershipStatus.ACTIVE));
        Membership mYonas = membershipRepository.save(new Membership(yonas, iqub, today.minusDays(40), MembershipStatus.ACTIVE));
        Membership mBetty = membershipRepository.save(new Membership(betty, iqub, today.minusDays(40), MembershipStatus.ACTIVE));

        // Round 1 — closed, everyone paid on time, Selam won.
        Round round1 = new Round(iqub, 1, today.minusDays(21));
        roundRepository.save(round1);
        BigDecimal amount = iqub.getContributionAmount();
        payOnTime(round1, selam, amount, today.minusDays(23));
        payOnTime(round1, abel, amount, today.minusDays(22));
        payOnTime(round1, marta, amount, today.minusDays(22));
        payOnTime(round1, yonas, amount, today.minusDays(21));
        payOnTime(round1, betty, amount, today.minusDays(21));
        round1.closeWithWinner(selam, amount.multiply(BigDecimal.valueOf(5)), today.minusDays(20));
        roundRepository.save(round1);
        mSelam.setHasReceivedPayoutThisCycle(true);
        membershipRepository.save(mSelam);

        // Round 2 — closed, Abel paid two days late (5% band), Abel won.
        Round round2 = new Round(iqub, 2, today.minusDays(14));
        roundRepository.save(round2);
        payOnTime(round2, selam, amount, today.minusDays(15));
        payLate(round2, abel, amount, today.minusDays(12)); // 2 days late -> 5% band
        payOnTime(round2, marta, amount, today.minusDays(14));
        payOnTime(round2, yonas, amount, today.minusDays(14));
        payOnTime(round2, betty, amount, today.minusDays(13));
        round2.closeWithWinner(abel, amount.multiply(BigDecimal.valueOf(5)), today.minusDays(11));
        roundRepository.save(round2);
        mAbel.setHasReceivedPayoutThisCycle(true);
        membershipRepository.save(mAbel);

        // Round 3 — open now: Selam, Abel, Marta paid; Yonas and Betty have not yet.
        Round round3 = roundRepository.save(new Round(iqub, 3, today.plusDays(4)));
        payOnTime(round3, selam, amount, today.minusDays(2));
        payOnTime(round3, abel, amount, today.minusDays(1));
        payOnTime(round3, marta, amount, today);
        contributionRepository.save(new Contribution(round3, yonas, amount));
        contributionRepository.save(new Contribution(round3, betty, amount));
    }

    private void payOnTime(Round round, Member member, BigDecimal amount, LocalDate paidDate) {
        Contribution contribution = new Contribution(round, member, amount);
        contribution.markPaid(ContributionStatus.PAID, amount, paidDate, BigDecimal.ZERO.setScale(2));
        contributionRepository.save(contribution);
    }

    private void payLate(Round round, Member member, BigDecimal amount, LocalDate paidDate) {
        BigDecimal penalty = amount.multiply(new BigDecimal("0.05")).setScale(2, java.math.RoundingMode.HALF_UP);
        Contribution contribution = new Contribution(round, member, amount);
        contribution.markPaid(ContributionStatus.PAID_LATE, amount, paidDate, penalty);
        contributionRepository.save(contribution);
    }
}
