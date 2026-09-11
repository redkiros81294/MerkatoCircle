package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.service.FakePaymentGateway;
import com.merkatocircle.iqub.service.PaymentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Stands in for Chapa's own hosted checkout page whenever the "chapa" profile is not active
 * (see FakePaymentGateway, spec §2). This lets the full pay-a-contribution journey — including
 * a Selenium test — run end to end with no network call leaving the machine the app runs on.
 *
 * <p>Depends on the concrete FakePaymentGateway, not the PaymentGateway interface, because its
 * only job is calling a fake-only method (recordOutcome). It's wired as optional so the app
 * still starts cleanly under the "chapa" profile, where this bean does not exist.
 */
@Controller
public class FakeCheckoutController {

    private final FakePaymentGateway fakePaymentGateway;

    public FakeCheckoutController(@Autowired(required = false) FakePaymentGateway fakePaymentGateway) {
        this.fakePaymentGateway = fakePaymentGateway;
    }

    @GetMapping("/test/fake-checkout")
    public String show(@RequestParam("tx_ref") String txRef, Model model) {
        model.addAttribute("txRef", txRef);
        return "fake-checkout";
    }

    @PostMapping("/test/fake-checkout/simulate")
    public String simulate(@RequestParam("tx_ref") String txRef, @RequestParam String outcome) {
        if (fakePaymentGateway != null) {
            PaymentStatus status = "success".equals(outcome) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
            fakePaymentGateway.recordOutcome(txRef, status);
        }
        return "redirect:/payments/return?tx_ref=" + txRef;
    }
}