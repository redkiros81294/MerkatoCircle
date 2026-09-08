package com.merkatocircle.iqub.service;

/**
 * The one seam between the app and any real payment provider. ContributionService never
 * talks to Chapa (or anything else) directly — only through this interface, so tests and
 * local development can run against {@link FakePaymentGateway} with no network at all,
 * while production runs against {@link ChapaPaymentGateway} (spec §2, §4.3).
 */
public interface PaymentGateway {

    /**
     * Starts a checkout. On success, the caller redirects the member's browser to the
     * returned checkoutUrl.
     *
     * @throws com.merkatocircle.iqub.exception.PaymentInitiationException if the provider
     *         could not be reached or refused the request
     */
    PaymentInitiation initiate(PaymentRequest request);

    /**
     * The only source of truth for whether a payment succeeded. Must be called server-side —
     * never infer success from a redirect or callback payload alone (spec §4.3).
     */
    PaymentVerification verify(String txRef);
}
