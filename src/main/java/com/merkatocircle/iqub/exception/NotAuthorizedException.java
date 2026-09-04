package com.merkatocircle.iqub.exception;

/** Thrown when spec §3.6's authorization rule fails: not a platform ADMIN and not this group's organizer. */
public class NotAuthorizedException extends RuntimeException {
    public NotAuthorizedException(String message) {
        super(message);
    }
}
