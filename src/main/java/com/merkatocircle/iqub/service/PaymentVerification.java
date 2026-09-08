package com.merkatocircle.iqub.service;

/** The verified truth about one payment attempt — never trust anything less than this. */
public record PaymentVerification(PaymentStatus status, String txRef, String providerRefId) {
}
