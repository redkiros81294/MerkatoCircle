package com.merkatocircle.iqub.service;

/** What a PaymentGateway hands back after starting a checkout: where to send the browser. */
public record PaymentInitiation(String checkoutUrl, String txRef) {
}
