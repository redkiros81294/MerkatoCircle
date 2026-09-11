package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.service.ContributionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * The two endpoints a payment provider (or FakePaymentGateway's stand-in) calls back into:
 * the browser's landing page after checkout, and the server-to-server callback. Both are
 * permitAll in SecurityConfig, so neither can assume there's a logged-in session — everything
 * needed comes from the tx_ref, via ContributionService.confirmPayment's idempotent lookup.
 */
@Controller
public class PaymentController {

    private final ContributionService contributionService;

    public PaymentController(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @GetMapping("/payments/return")
    public String returnFromCheckout(@RequestParam("tx_ref") String txRef, Model model) {
        Contribution contribution = contributionService.confirmPayment(txRef);
        model.addAttribute("contribution", contribution);
        return "confirm";
    }

    /** Chapa's server calls this directly — spec §4.3 documents it as a plain GET. */
    @GetMapping("/payments/chapa/callback")
    @ResponseBody
    public String chapaCallback(@RequestParam("tx_ref") String txRef) {
        contributionService.confirmPayment(txRef);
        return "OK";
    }
}