package com.eventsApp.integration.mail;

/** Raised when Google rejects the stored refresh token and the tenant must reconnect their account. */
public class GoogleReauthRequiredException extends RuntimeException {

    public GoogleReauthRequiredException(String message) {
        super(message);
    }
}
