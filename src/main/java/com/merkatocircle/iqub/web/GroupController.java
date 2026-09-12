package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Membership;
import com.merkatocircle.iqub.domain.MembershipStatus;
import com.merkatocircle.iqub.domain.PayoutMode;
import com.merkatocircle.iqub.domain.PlatformRole;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.exception.NotAuthorizedException;
import com.merkatocircle.iqub.repository.IqubRepository;
import com.merkatocircle.iqub.service.ContributionService;
import com.merkatocircle.iqub.service.CurrentMemberProvider;
import com.merkatocircle.iqub.service.MembershipService;
import com.merkatocircle.iqub.service.NotificationService;
import com.merkatocircle.iqub.service.RoundService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Everything scoped to one Iqub group: its home page, joining it, and managing its roster.
 * Round/bid/payment actions live in RoundController — this class stays about membership,
 * per spec §3.5 and the organizer-or-admin rule in spec §3.6.
 */
@Controller
public class GroupController {

    private final IqubRepository iqubRepository;
    private final MembershipService membershipService;
    private final RoundService roundService;
    private final ContributionService contributionService;
    private final NotificationService notificationService;
    private final CurrentMemberProvider currentMemberProvider;

    public GroupController(IqubRepository iqubRepository,
                            MembershipService membershipService,
                            RoundService roundService,
                            ContributionService contributionService,
                            NotificationService notificationService,
                            CurrentMemberProvider currentMemberProvider) {
        this.iqubRepository = iqubRepository;
        this.membershipService = membershipService;
        this.roundService = roundService;
        this.contributionService = contributionService;
        this.notificationService = notificationService;
        this.currentMemberProvider = currentMemberProvider;
    }

    @PostMapping("/iqubs/{iqubId}/join")
    public String join(@PathVariable Long iqubId, Authentication authentication,
                        RedirectAttributes redirectAttributes) {
        Iqub iqub = findIqub(iqubId);
        Member member = currentMemberProvider.get(authentication);

        if (membershipService.alreadyMember(iqub, member)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You're already in " + iqub.getName() + ".");
            return "redirect:/dashboard";
        }

        Membership membership = membershipService.join(iqub, member, LocalDate.now());
        redirectAttributes.addFlashAttribute("successMessage",
                membership.getStatus() == MembershipStatus.ACTIVE
                        ? "You're in! Welcome to " + iqub.getName() + "."
                        : "That group is full — you're on the waitlist for " + iqub.getName()
                                + " and will be added automatically when a seat opens.");
        return "redirect:/iqubs/" + iqubId;
    }

    @GetMapping("/iqubs/{iqubId}")
    public String home(@PathVariable Long iqubId, Authentication authentication, Model model) {
        Iqub iqub = findIqub(iqubId);
        Member member = requireMember(iqub, authentication);

        Round round = roundService.getCurrentRound(iqub);
        List<Contribution> contributions = roundService.getContributions(round);
        Optional<Contribution> myContribution = contributionService.findForRoundAndMember(round, member);

        model.addAttribute("iqub", iqub);
        model.addAttribute("round", round);
        model.addAttribute("contributions", contributions);
        model.addAttribute("myContribution", myContribution.orElse(null));
        model.addAttribute("isOrganizer", isOrganizerOrAdmin(iqub, member));
        model.addAttribute("isAuction", iqub.getPayoutMode() == PayoutMode.AUCTION);
        model.addAttribute("unreadCount", notificationService.unreadCount(member));
        return "group";
    }

    @GetMapping("/iqubs/{iqubId}/members")
    public String members(@PathVariable Long iqubId, Authentication authentication, Model model) {
        Iqub iqub = findIqub(iqubId);
        Member member = requireMember(iqub, authentication);

        model.addAttribute("iqub", iqub);
        model.addAttribute("roster", membershipService.membersOf(iqub));
        model.addAttribute("isOrganizer", isOrganizerOrAdmin(iqub, member));
        return "members";
    }

    @PostMapping("/iqubs/{iqubId}/members/{membershipId}/remove")
    public String remove(@PathVariable Long iqubId, @PathVariable Long membershipId,
                          Authentication authentication, RedirectAttributes redirectAttributes) {
        Iqub iqub = findIqub(iqubId);
        Member member = currentMemberProvider.get(authentication);
        requireOrganizerOrAdmin(iqub, member);

        Membership target = membershipService.getById(membershipId);
        membershipService.removeMember(target);
        redirectAttributes.addFlashAttribute("successMessage",
                "Removed " + target.getMember().getFullName() + " from " + iqub.getName() + ".");
        return "redirect:/iqubs/" + iqubId + "/members";
    }

    @GetMapping("/groups")
    public String groups(Authentication authentication, Model model) {
        Member member = currentMemberProvider.get(authentication);
        List<Iqub> groups = iqubRepository.findAll();
        Map<Long, Membership> myMembershipsByGroup = groups.stream()
                .flatMap(g -> membershipService.membersOf(g).stream())
                .filter(m -> m.getMember().getId().equals(member.getId()))
                .collect(Collectors.toMap(m -> m.getIqub().getId(), m -> m));

        model.addAttribute("groups", groups);
        model.addAttribute("myMembershipsByGroup", myMembershipsByGroup);
        model.addAttribute("me", member);
        model.addAttribute("createForm", new GroupForm());
        return "groups";
    }

    @PostMapping("/groups")
    public String create(@ModelAttribute GroupForm form, Authentication authentication, RedirectAttributes redirectAttributes) {
        Member member = currentMemberProvider.get(authentication);
        Iqub iqub = iqubRepository.save(new Iqub(form.name(), form.contributionAmount(), form.roundIntervalDays(), form.maxMembers(), LocalDate.now()));
        iqub.setOrganizer(member);
        iqub.setPayoutMode(form.auctionMode() ? PayoutMode.AUCTION : PayoutMode.LOTTERY);
        iqubRepository.save(iqub);
        membershipService.join(iqub, member, LocalDate.now());
        redirectAttributes.addFlashAttribute("successMessage", "Circle created — you're the organizer.");
        return "redirect:/iqubs/" + iqub.getId();
    }

    public record GroupForm(String name, java.math.BigDecimal contributionAmount, int roundIntervalDays, int maxMembers, boolean auctionMode) {
        public GroupForm() {
            this("", java.math.BigDecimal.ZERO, 7, 2, false);
        }
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

    /** The spec §3.6 authorization rule: not a platform ADMIN and not this group's organizer. */
    private void requireOrganizerOrAdmin(Iqub iqub, Member member) {
        if (!isOrganizerOrAdmin(iqub, member)) {
            throw new NotAuthorizedException("Only " + iqub.getName() + "'s organizer or a platform admin can do this");
        }
    }
}