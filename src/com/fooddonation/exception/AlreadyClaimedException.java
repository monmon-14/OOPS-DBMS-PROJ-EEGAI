package com.fooddonation.exception;

/**
 * Custom Exception thrown when an NGO or volunteer attempts to claim or accept
 * a food listing that has already been claimed or reserved.
 *
 * OOP Concept: Custom Exceptions (Domain-specific checked exception hierarchy)
 */
public class AlreadyClaimedException extends Exception {

    private final int donationId;

    public AlreadyClaimedException(int donationId, String message) {
        super(message);
        this.donationId = donationId;
    }

    public int getDonationId() {
        return donationId;
    }
}
