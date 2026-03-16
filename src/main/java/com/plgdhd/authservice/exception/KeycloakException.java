package com.plgdhd.authservice.exception;

public class KeycloakException extends RuntimeException {

    public KeycloakException(String s) {
        super(s);
    }

    public KeycloakException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
