package com.fooddonation.model;

/**
 * Interface defining the contract for entities whose lifecycle status
 * can be queried and updated as they progress through system milestones.
 *
 * OOP Concept: Interface Definition
 */
public interface Trackable {

    /**
     * Retrieves the current operational status of the trackable entity.
     * Covariant return types in implementing classes allow returning
     * specific Enum types (e.g. DonationStatus, DeliveryStatus).
     *
     * @return Current status object
     */
    Object getStatus();

    /**
     * Updates the status of the entity using a string representation.
     *
     * @param newStatus The new status value
     */
    void updateStatus(String newStatus);

    /**
     * Default method providing a string representation of the status.
     *
     * @return Formatted status string
     */
    default String getStatusAsString() {
        Object s = getStatus();
        return s != null ? s.toString() : "";
    }
}
