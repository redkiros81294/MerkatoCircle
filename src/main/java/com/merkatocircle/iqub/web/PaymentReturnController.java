package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.service.ContributionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The member's browser lands here straight from Chapa's checkout. We never trust that fact
 * alone — {@link ContributionService#confirmPayment} re-verifies server-side (spec §4.3)
 * before this page reports success either way.
 */
@Controller
public class PaymentReturnController {

    private final ContributionService contributionService;

    public PaymentReturnController(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @GetMapping("/payments/return")
    public String returned(@RequestParam("contribution_id") Long contributionId, Model model) {
        Contribution contribution = contributionService.findById(contributionId);

        if (contribution.getTxRef() != null) {
            contribution = contributionService.confirmPayment(contribution.getTxRef());
        }

        model.addAttribute("contribution", contribution);
        return "payment-return";
    }
}
