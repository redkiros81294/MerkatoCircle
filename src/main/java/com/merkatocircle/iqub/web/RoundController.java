package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Bid;
import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.PayoutMode;
import com.merkatocircle.iqub.domain.PlatformRole;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.exception.NotAuthorizedException;
import com.merkatocircle.iqub.repository.IqubRepository;
import com.merkatocircle.iqub.service.BidService;
import com.merkatocircle.iqub.service.ContributionService;
import com.merkatocircle.iqub.service.CurrentMemberProvider;
import com.merkatocircle.iqub.service.MembershipService;
import com.merkatocircle.iqub.service.PaymentInitiation;
import com.merkatocircle.iqub.service.RoundService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Round history, round detail, the draw, auction bidding (spec §3.7), and starting a payment. */
@Controller
public class RoundController {

    private final IqubRepository iqubRepository;
    private final RoundService roundService;
    private final BidService bidService;
    private final ContributionService contributionService;
    private final MembershipService membershipService;
    private final CurrentMemberProvider currentMemberProvider;

    public RoundController(IqubRepository iqubRepository,
                            RoundService roundService,
                            BidService bidService,
                            ContributionService contributionService,
                            MembershipService membershipService,
                            CurrentMemberProvider currentMemberProvider) {
        this.iqubRepository = iqubRepository;
        this.roundService = roundService;
        this.bidService = bidService;
        this.contributionService = contributionService;
        this.membershipService = membershipService;
        this.currentMemberProvider = currentMemberProvider;
    }

    @GetMapping("/iqubs/{iqubId}/rounds")
    public String list(@PathVariable Long iqubId, Authentication authentication, Model model) {
        Iqub iqub = findIqub(iqubId);
        requireMember(iqub, authentication);

        model.addAttribute("iqub", iqub);
        model.addAttribute("rounds", roundService.getAllRounds(iqub));
        return "rounds";
    }

    @GetMapping("/iqubs/{iqubId}/rounds/{roundId}")
    public String detail(@PathVariable Long iqubId, @PathVariable Long roundId,
                          Authentication authentication, Model model) {
        Iqub iqub = findIqub(iqubId);
        Member member = requireMember(iqub, authentication);
        Round round = roundService.getById(roundId);

        List<Member> eligibleMembers = roundService.getEligibleMembers(round);
        Optional<Bid> myBid = bidService.myBid(round, member);

        model.addAttribute("iqub", iqub);
        model.addAttribute("round", round);
        model.addAttribute("contributions", roundService.getContributions(round));
        model.addAttribute("eligibleMembers", eligibleMembers);
        model.addAttribute("isEligible", eligibleMembers.stream().anyMatch(m -> m.getId().equals(member.getId())));
        model.addAttribute("isAuction", iqub.getPayoutMode() == PayoutMode.AUCTION);
        model.addAttribute("bids", iqub.getPayoutMode() == PayoutMode.AUCTION ? bidService.allBidsFor(round) : List.of());
        model.addAttribute("myBid", myBid.orElse(null));
        model.addAttribute("isOrganizer", isOrganizerOrAdmin(iqub, member));
        return "round-detail";
    }

    @PostMapping("/iqubs/{iqubId}/rounds/{roundId}/draw")
    public String draw(@PathVariable Long iqubId, @PathVariable Long roundId,
                        Authentication authentication, RedirectAttributes redirectAttributes) {
        Iqub iqub = findIqub(iqubId);
        Member member = currentMemberProvider.get(authentication);
        requireOrganizerOrAdmin(iqub, member);

        Round closed = roundService.runDraw(roundService.getById(roundId));
        redirectAttributes.addFlashAttribute("successMessage",
                closed.getWinner().getFullName() + " won round " + closed.getRoundNumber()
                        + " — " + closed.getPayoutAmount() + " ETB paid out.");
        return "redirect:/iqubs/" + iqubId + "/rounds/" + roundId;
    }

    @PostMapping("/iqubs/{iqubId}/rounds/{roundId}/bid")
    public String bid(@PathVariable Long iqubId, @PathVariable Long roundId,
                       @RequestParam BigDecimal discountPercent,
                       Authentication authentication, RedirectAttributes redirectAttributes) {
        Iqub iqub = findIqub(iqubId);
        Member member = requireMember(iqub, authentication);
        Round round = roundService.getById(roundId);

        bidService.submitBid(round, member, discountPercent);
        redirectAttributes.addFlashAttribute("successMessage", "Your bid of " + discountPercent + "% is in.");
        return "redirect:/iqubs/" + iqubId + "/rounds/" + roundId;
    }

    /** Starts a Chapa (or fake) checkout and sends the member's browser straight to it. */
    @PostMapping("/iqubs/{iqubId}/rounds/{roundId}/pay")
    public String pay(@PathVariable Long iqubId, @PathVariable Long roundId,
                       Authentication authentication, HttpServletRequest request) {
        Iqub iqub = findIqub(iqubId);
        Member member = requireMember(iqub, authentication);
        Round round = roundService.getById(roundId);

        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (isDefaultPort(request) ? "" : ":" + request.getServerPort());

        PaymentInitiation initiation = contributionService.initiatePayment(
                round, member, baseUrl + "/payments/chapa/callback", baseUrl + "/payments/return");

        return "redirect:" + initiation.checkoutUrl();
    }

    private boolean isDefaultPort(HttpServletRequest request) {
        return (request.getServerPort() == 80 && "http".equals(request.getScheme()))
                || (request.getServerPort() == 443 && "https".equals(request.getScheme()));
    }

    private Iqub findIqub(Long iqubId) {
        return iqubRepository.findById(iqubId)
                .orElseThrow(() -> new IllegalArgumentException("No iqub with id " + iqubId));
    }

    private Member requireMember(Iqub iqub, Authentication authentication) {
        Member member = currentMemberProvider.get(authentication);
        if (!membershipService.alreadyMember(iqub, member)) {
            throw new NotAuthorizedException("You're not a member of " + iqub.getName() + " yet");
        }
        return member;
    }

    private boolean isOrganizerOrAdmin(Iqub iqub, Member member) {
        boolean isOrganizer = iqub.getOrganizer() != null && iqub.getOrganizer().getId().equals(member.getId());
        return isOrganizer || member.getPlatformRole() == PlatformRole.ADMIN;
    }

    private void requireOrganizerOrAdmin(Iqub iqub, Member member) {
        if (!isOrganizerOrAdmin(iqub, member)) {
            throw new NotAuthorizedException("Only " + iqub.getName() + "'s organizer or a platform admin can do this");
        }
    }
}