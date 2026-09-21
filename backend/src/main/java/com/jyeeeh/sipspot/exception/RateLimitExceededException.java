package com.jyeeeh.sipspot.exception;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException() {
        super("Rate limit exceeded");
    }
}
