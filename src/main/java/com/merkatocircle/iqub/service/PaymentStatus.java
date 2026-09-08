package com.merkatocircle.iqub.service;

/** The three states Chapa's verify endpoint can report (spec §4.3). */
public enum PaymentStatus {
    SUCCESS,
    FAILED,
    PENDING
}
