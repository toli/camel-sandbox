package com.carmanconsulting.sandbox.camel.throttle;

public class ClientThrottlingException extends RuntimeException {
    public ClientThrottlingException(String message) {
        super(message);
    }
}
