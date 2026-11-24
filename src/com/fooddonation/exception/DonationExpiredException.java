package com.fooddonation.exception;

/**
 * Custom Exception thrown when an operation is attempted on a food donation
 * that has exceeded its safe consumption expiry timestamp.
 *
 * OOP Concept: Custom Exceptions (Domain-specific checked exception hierarchy)
 */
public class DonationExpiredException extends Exception {

    private final int donationId;

    public DonationExpiredException(int donationId, String message) {
        super(message);
        this.donationId = donationId;
    }

    public int getDonationId() {
        return donationId;
    }
}
