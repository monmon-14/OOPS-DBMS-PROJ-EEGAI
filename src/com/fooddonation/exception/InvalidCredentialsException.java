package com.fooddonation.exception;

/**
 * Custom Exception thrown when authentication fails due to incorrect email,
 * password, or role mismatch.
 *
 * OOP Concept: Custom Exceptions (Domain-specific checked exception hierarchy)
 */
public class InvalidCredentialsException extends Exception {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
