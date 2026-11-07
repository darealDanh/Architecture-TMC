package com.tmc.gate.credential;

public class MalformedCredentialException extends RuntimeException {

    public MalformedCredentialException(String message) {
        super(message);
    }

    public MalformedCredentialException(String message, Throwable cause) {
        super(message, cause);
    }
}
