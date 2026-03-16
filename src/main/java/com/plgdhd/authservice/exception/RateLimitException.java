package com.plgdhd.authservice.exception;

public class RateLimitException extends RuntimeException {
    public RateLimitException() {
        super();
    }

    public RateLimitException(String message) {
        super(message);
    }

    public RateLimitException(Long retryAfter) {
        super(retryAfter.toString());
    }
}
