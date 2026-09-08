package com.merkatocircle.iqub.service;

import java.math.BigDecimal;

/**
 * Everything a PaymentGateway needs to start a checkout. Field names deliberately mirror
 * Chapa's own request fields (spec §4.3) so ChapaPaymentGateway.initiate is a near 1:1 mapping.
 */
public record PaymentRequest(
        BigDecimal amount,
        String currency,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String txRef,
        String callbackUrl,
        String returnUrl
) {
}
