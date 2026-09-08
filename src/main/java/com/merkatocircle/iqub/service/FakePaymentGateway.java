package com.merkatocircle.iqub.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The test double for {@link PaymentGateway} (spec §2, §4.3). Active whenever the
 * {@code chapa} profile is NOT active — which is every automated test and every local
 * "just run it" session, so nobody needs a real Chapa account to see the app work end to end.
 *
 * <p>{@code initiate} never touches the network: it hands back a checkout URL pointing at
 * this app's own {@code /test/fake-checkout} page. That page's two buttons call
 * {@link #recordOutcome} directly, so a Selenium test can drive the *entire* payment journey —
 * click Pay, land on the fake checkout, click Simulate success, get redirected back, see the
 * status flip to Paid — without a single call leaving the machine the test runs on.
 */
@Component
@Profile("!chapa")
public class FakePaymentGateway implements PaymentGateway {

    private final Map<String, PaymentStatus> outcomes = new ConcurrentHashMap<>();

    @Override
    public PaymentInitiation initiate(PaymentRequest request) {
        outcomes.put(request.txRef(), PaymentStatus.PENDING);
        String checkoutUrl = "/test/fake-checkout?tx_ref=" + request.txRef();
        return new PaymentInitiation(checkoutUrl, request.txRef());
    }

    @Override
    public PaymentVerification verify(String txRef) {
        PaymentStatus status = outcomes.getOrDefault(txRef, PaymentStatus.PENDING);
        return new PaymentVerification(status, txRef, "fake-" + txRef);
    }

    /** Called by FakeCheckoutController when the member clicks a "Simulate ..." button. */
    public void recordOutcome(String txRef, PaymentStatus status) {
        outcomes.put(txRef, status);
    }
}
