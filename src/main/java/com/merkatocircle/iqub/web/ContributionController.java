package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.service.ContributionService;
import com.merkatocircle.iqub.service.CurrentMemberProvider;
import com.merkatocircle.iqub.service.PaymentInitiation;
import com.merkatocircle.iqub.service.RoundService;
import com.merkatocircle.iqub.service.TheIqub;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
public class ContributionController {

    private final CurrentMemberProvider currentMemberProvider;
    private final TheIqub theIqub;
    private final RoundService roundService;
    private final ContributionService contributionService;

    public ContributionController(CurrentMemberProvider currentMemberProvider, TheIqub theIqub,
                                   RoundService roundService, ContributionService contributionService) {
        this.currentMemberProvider = currentMemberProvider;
        this.theIqub = theIqub;
        this.roundService = roundService;
        this.contributionService = contributionService;
    }

    @GetMapping("/contribute")
    public String show(Authentication authentication, Model model) {
        Member me = currentMemberProvider.get(authentication);
        Iqub iqub = theIqub.get();
        Round round = roundService.getCurrentRound(iqub);
        Contribution contribution = contributionService.findForRoundAndMember(round, me)
                .orElseThrow(() -> new IllegalStateException("No contribution record for the current round"));

        model.addAttribute("round", round);
        model.addAttribute("contribution", contribution);
        return "contribute";
    }

    @PostMapping("/contribute/pay")
    public String pay(Authentication authentication, HttpServletRequest request) {
        Member me = currentMemberProvider.get(authentication);
        Iqub iqub = theIqub.get();
        Round round = roundService.getCurrentRound(iqub);
        Contribution contribution = contributionService.findForRoundAndMember(round, me)
                .orElseThrow(() -> new IllegalStateException("No contribution record for the current round"));

        String base = ServletUriComponentsBuilder.fromContextPath(request).build().toUriString();
        String callbackUrl = base + "/payments/chapa/callback";
        // We embed our own contribution_id rather than relying on Chapa echoing tx_ref back
        // on the return_url — the callback_url is where Chapa's documented trx_ref applies
        // (spec §4.3); the return_url is just our own browser redirect target, so we control
        // exactly what identifies the payment when the member lands back on it.
        String returnUrl = base + "/payments/return?contribution_id=" + contribution.getId();

        PaymentInitiation initiation = contributionService.initiatePayment(round, me, callbackUrl, returnUrl);
        return "redirect:" + initiation.checkoutUrl();
    }
}
