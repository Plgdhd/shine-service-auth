package com.plgdhd.authservice.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {}

    public InvalidCredentialsException(String s) {
        super(s);
    }
    public InvalidCredentialsException(String s, Throwable throwable) {}
}
