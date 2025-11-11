package com.PlanInk.mvc.auth;

public class FirebaseLoginException extends Exception {

    private final boolean invalidCredentials;

    public FirebaseLoginException(String message) {
        this(message, false, null);
    }

    public FirebaseLoginException(String message, boolean invalidCredentials) {
        this(message, invalidCredentials, null);
    }

    public FirebaseLoginException(String message, Throwable cause) {
        this(message, false, cause);
    }

    public FirebaseLoginException(String message, boolean invalidCredentials, Throwable cause) {
        super(message, cause);
        this.invalidCredentials = invalidCredentials;
    }

    public boolean isInvalidCredentials() {
        return invalidCredentials;
    }
}

