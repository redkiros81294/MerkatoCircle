package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.service.ContributionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chapa calls this directly, server-to-server, with no session and no CSRF token — a plain
 * GET carrying {@code trx_ref} (per developer.chapa.co). This is the reliable confirmation
 * path; {@link PaymentReturnController} handles the same confirmation for the member's own
 * browser, and {@code confirmPayment} being idempotent (spec §3.4) is exactly what makes it
 * safe for both to call it for the same payment.
 */
@RestController
public class ChapaCallbackController {

    private final ContributionService contributionService;

    public ChapaCallbackController(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @GetMapping("/payments/chapa/callback")
    @ResponseBody
    public ResponseEntity<Void> callback(@RequestParam("trx_ref") String trxRef) {
        try {
            contributionService.confirmPayment(trxRef);
        } catch (IllegalArgumentException unknownRef) {
            // an unrecognised tx_ref isn't worth Chapa retrying forever over — acknowledge anyway.
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok().build();
    }
}
