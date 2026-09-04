package com.merkatocircle.iqub.exception;

/** Thrown when the PaymentGateway (Chapa or otherwise) fails to start a checkout session. */
public class PaymentInitiationException extends RuntimeException {
    public PaymentInitiationException(String message, Throwable cause) {
        super(message, cause);
    }
}
