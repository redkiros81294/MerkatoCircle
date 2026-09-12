package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Bid;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.service.BidService;
import com.merkatocircle.iqub.service.CurrentMemberProvider;
import com.merkatocircle.iqub.service.RoundService;
import java.math.BigDecimal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Bid submission for AUCTION-mode groups (spec §3.7) — Tier 3. */
@Controller
public class BidController {

    private final RoundService roundService;
    private final BidService bidService;
    private final CurrentMemberProvider currentMemberProvider;

    public BidController(RoundService roundService, BidService bidService, CurrentMemberProvider currentMemberProvider) {
        this.roundService = roundService;
        this.bidService = bidService;
        this.currentMemberProvider = currentMemberProvider;
    }

    @GetMapping("/rounds/{id}/bid")
    public String show(@PathVariable Long id, Authentication authentication, Model model) {
        Round round = roundService.getById(id);
        Member me = currentMemberProvider.get(authentication);

        model.addAttribute("round", round);
        model.addAttribute("existingBid", bidService.myBid(round, me).orElse(null));
        return "bid";
    }

    @PostMapping("/rounds/{id}/bid")
    public String submit(@PathVariable Long id, Authentication authentication,
                          @RequestParam("discountPercent") BigDecimal discountPercent,
                          RedirectAttributes redirectAttributes) {
        Round round = roundService.getById(id);
        Member me = currentMemberProvider.get(authentication);

        Bid bid = bidService.submitBid(round, me, discountPercent);
        redirectAttributes.addFlashAttribute("infoMessage",
                "Bid recorded: " + bid.getDiscountPercent() + "% discount for round " + round.getRoundNumber() + ".");
        return "redirect:/rounds/" + id;
    }
}
