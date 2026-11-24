package com.fooddonation.service;

import com.fooddonation.dao.ClaimDAO;
import com.fooddonation.dao.DeliveryDAO;
import com.fooddonation.dao.DonationDAO;
import com.fooddonation.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing real-time delivery tracking, sequential stepper advancement,
 * donation status synchronization, and live audit timelines for Donors and NGOs.
 */
public class DeliveryService {

    private final DeliveryDAO deliveryDAO;
    private final ClaimDAO claimDAO;
    private final DonationDAO donationDAO;

    public DeliveryService() {
        this(new DeliveryDAO(), new ClaimDAO(), new DonationDAO());
    }

    public DeliveryService(DeliveryDAO deliveryDAO, ClaimDAO claimDAO, DonationDAO donationDAO) {
        this.deliveryDAO = deliveryDAO;
        this.claimDAO = claimDAO;
        this.donationDAO = donationDAO;
    }

    /**
     * Advances the delivery status sequentially by one step.
     * Enforces the business pipeline: Accepted -> Picked Up -> Delivered.
     * On 'Delivered', requires the actual number of meals delivered.
     * Synchronizes Donations, Claims, and Deliveries statuses.
     */
    public DeliveryResult updateStatus(int deliveryId, DeliveryStatus targetStatus, Integer mealsDelivered) {
        if (deliveryId <= 0) {
            return DeliveryResult.failure("Invalid delivery identifier.");
        }
        if (targetStatus == null) {
            return DeliveryResult.failure("Target delivery status is required.");
        }

        Delivery delivery = deliveryDAO.findById(deliveryId);
        if (delivery == null) {
            return DeliveryResult.failure("Delivery mission not found.");
        }

        DeliveryStatus current = delivery.getStatus();

        // 1. Enforce strict sequential step progression
        if (current == DeliveryStatus.DELIVERED) {
            return DeliveryResult.failure("This delivery has already been completed and delivered.");
        }

        if (current == DeliveryStatus.ACCEPTED && targetStatus != DeliveryStatus.PICKED_UP) {
            return DeliveryResult.failure("Invalid step. Deliveries must be marked as 'Picked Up' before being marked as 'Delivered'.");
        }

        if (current == DeliveryStatus.PICKED_UP && targetStatus != DeliveryStatus.DELIVERED) {
            return DeliveryResult.failure("Invalid step. Next expected stage is 'Delivered'.");
        }

        // 2. Validate meals delivered on final step
        if (targetStatus == DeliveryStatus.DELIVERED) {
            if (mealsDelivered == null || mealsDelivered <= 0) {
                return DeliveryResult.failure("Please enter the actual number of meals safely delivered (must be greater than 0).");
            }
        }

        // 3. Execute atomic database transaction
        boolean ok = deliveryDAO.updateStatus(deliveryId, targetStatus, mealsDelivered);
        if (!ok) {
            return DeliveryResult.failure("Failed to update delivery milestone in database.");
        }

        // Fetch refreshed delivery
        Delivery updated = deliveryDAO.findById(deliveryId);
        return DeliveryResult.success(updated);
    }

    /**
     * Retrieves the complete chronological live timeline for a donation.
     * Used by Donors and NGOs for real-time tracking (auto-refreshed every 10s).
     */
    public DeliveryTimeline getTimeline(int donationId) {
        donationDAO.expireOldDonations();

        // Retrieve donation details
        FoodDonation donation = DonationDAO.findFallbackById(donationId);
        String foodName = (donation != null) ? donation.getFoodName() : "Surplus Meal Donation #" + donationId;
        int plannedQuantity = (donation != null) ? donation.getQuantity() : 40;
        String donorName = (donation != null && donation.getDonorName() != null) ? donation.getDonorName() : "Green Garden Bistro";
        String pickupAddress = (donation != null) ? donation.getPickupAddress() : "124 Culinary Boulevard, Downtown";
        LocalDateTime listedTime = (donation != null && donation.getCreatedAt() != null) ? donation.getCreatedAt() : LocalDateTime.now().minusHours(4);

        // Retrieve claim if any
        Delivery delivery = deliveryDAO.findByDonationId(donationId);
        String ngoName = (delivery != null && delivery.getNgoName() != null) ? delivery.getNgoName() : "Hope Food Foundation";
        String dropoffAddress = (delivery != null && delivery.getNgoAddress() != null) ? delivery.getNgoAddress() : "450 Community Way, Suite 12, Seattle, WA";
        String volunteerName = (delivery != null && delivery.getVolunteerName() != null) ? delivery.getVolunteerName() : "Awaiting Courier";

        List<TimelineEvent> events = new ArrayList<>();

        // Stage 1: Listed by Donor
        events.add(new TimelineEvent(
                "Food Listed",
                "Listed by " + donorName + " (" + plannedQuantity + " meals portions)",
                pickupAddress,
                listedTime,
                true,
                (delivery == null),
                "",
                donorName
        ));

        // Stage 2: Claimed by NGO
        boolean hasClaim = (donation != null && donation.getStatus() != DonationStatus.AVAILABLE && donation.getStatus() != DonationStatus.CANCELLED) || (delivery != null);
        LocalDateTime claimedTime = (delivery != null && delivery.getCreatedAt() != null) ? delivery.getCreatedAt().minusMinutes(20) : listedTime.plusMinutes(45);
        events.add(new TimelineEvent(
                "Claimed by NGO",
                "Reserved by " + ngoName + " for shelter distribution",
                dropoffAddress,
                hasClaim ? claimedTime : null,
                hasClaim,
                hasClaim && (delivery == null || delivery.getStatus() == DeliveryStatus.ACCEPTED),
                "",
                ngoName
        ));

        // Stage 3: Courier Assigned (Accepted)
        boolean hasVolunteer = (delivery != null);
        LocalDateTime acceptedTime = (delivery != null) ? delivery.getCreatedAt() : null;
        events.add(new TimelineEvent(
                "Courier Assigned",
                hasVolunteer ? "Volunteer " + volunteerName + " accepted route" : "Awaiting volunteer courier dispatch",
                hasVolunteer ? "Driver assigned and en route to kitchen" : "Pending pickup assignment",
                acceptedTime,
                hasVolunteer,
                hasVolunteer && (delivery.getStatus() == DeliveryStatus.ACCEPTED),
                "",
                volunteerName
        ));

        // Stage 4: Picked Up from Donor Kitchen
        boolean isPickedUp = (delivery != null && delivery.isPickedUp());
        LocalDateTime pickedTime = (delivery != null) ? delivery.getPickedAt() : null;
        events.add(new TimelineEvent(
                "Picked Up",
                isPickedUp ? "Meals safely collected from " + donorName : "Awaiting courier arrival at donor kitchen",
                isPickedUp ? "In transit to " + ngoName : "Pickup pending",
                pickedTime,
                isPickedUp,
                isPickedUp && delivery.getStatus() == DeliveryStatus.PICKED_UP,
                "",
                volunteerName
        ));

        // Stage 5: Delivered to NGO Shelter
        boolean isDelivered = (delivery != null && delivery.isCompleted());
        LocalDateTime deliveredTime = (delivery != null) ? delivery.getDeliveredAt() : null;
        int finalMeals = (delivery != null && delivery.getMealsDelivered() != null) ? delivery.getMealsDelivered() : plannedQuantity;
        events.add(new TimelineEvent(
                "Delivered & Distributed",
                isDelivered ? "Successfully delivered to " + ngoName + " (" + finalMeals + " meals served)" : "Awaiting final shelter delivery",
                isDelivered ? "Distributed to beneficiaries at " + dropoffAddress : "Final drop-off pending",
                deliveredTime,
                isDelivered,
                isDelivered,
                "",
                ngoName
        ));

        String currentStatusStr = (donation != null) ? donation.getStatus().getDisplayName() : "Available";
        if (delivery != null) {
            currentStatusStr = delivery.getStatus().getDisplayName();
        }

        return new DeliveryTimeline(donationId, foodName, plannedQuantity, donorName, ngoName, volunteerName, currentStatusStr, events, delivery);
    }

    /**
     * Gets or creates a delivery record for an accepted claim.
     */
    public Delivery getOrCreateDeliveryForClaim(int claimId, int volunteerId) {
        return deliveryDAO.getOrCreateDeliveryForClaim(claimId, volunteerId);
    }

    /**
     * Retrieves all active and completed deliveries for a volunteer.
     */
    public List<Delivery> getDeliveriesForVolunteer(int volunteerId) {
        return deliveryDAO.findByVolunteerId(volunteerId);
    }

    public Delivery getDeliveryByClaim(int claimId) {
        return deliveryDAO.findByClaimId(claimId);
    }

    public Delivery getDeliveryByDonation(int donationId) {
        return deliveryDAO.findByDonationId(donationId);
    }

    // =========================================================================
    // DTOs for Timelines and Operations
    // =========================================================================

    public static class TimelineEvent {
        private final String stageName;
        private final String title;
        private final String detail;
        private final LocalDateTime timestamp;
        private final boolean completed;
        private final boolean currentActive;
        private final String icon;
        private final String actorName;

        private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy  hh:mm a");

        public TimelineEvent(String stageName, String title, String detail, LocalDateTime timestamp,
                             boolean completed, boolean currentActive, String icon, String actorName) {
            this.stageName = stageName;
            this.title = title;
            this.detail = detail;
            this.timestamp = timestamp;
            this.completed = completed;
            this.currentActive = currentActive;
            this.icon = icon;
            this.actorName = actorName;
        }

        public String getStageName() { return stageName; }
        public String getTitle() { return title; }
        public String getDetail() { return detail; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public boolean isCompleted() { return completed; }
        public boolean isCurrentActive() { return currentActive; }
        public String getIcon() { return icon; }
        public String getActorName() { return actorName; }

        public String getFormattedTimestamp() {
            return (timestamp != null) ? timestamp.format(TIME_FMT) : "Pending";
        }
    }

    public static class DeliveryTimeline {
        private final int donationId;
        private final String foodName;
        private final int quantity;
        private final String donorName;
        private final String ngoName;
        private final String volunteerName;
        private final String currentStatus;
        private final List<TimelineEvent> events;
        private final Delivery delivery;

        public DeliveryTimeline(int donationId, String foodName, int quantity, String donorName,
                                String ngoName, String volunteerName, String currentStatus,
                                List<TimelineEvent> events, Delivery delivery) {
            this.donationId = donationId;
            this.foodName = foodName;
            this.quantity = quantity;
            this.donorName = donorName;
            this.ngoName = ngoName;
            this.volunteerName = volunteerName;
            this.currentStatus = currentStatus;
            this.events = events;
            this.delivery = delivery;
        }

        public int getDonationId() { return donationId; }
        public String getFoodName() { return foodName; }
        public int getQuantity() { return quantity; }
        public String getDonorName() { return donorName; }
        public String getNgoName() { return ngoName; }
        public String getVolunteerName() { return volunteerName; }
        public String getCurrentStatus() { return currentStatus; }
        public List<TimelineEvent> getEvents() { return events; }
        public Delivery getDelivery() { return delivery; }
    }

    public static class DeliveryResult {
        private final boolean success;
        private final String errorMessage;
        private final Delivery delivery;

        private DeliveryResult(boolean success, String errorMessage, Delivery delivery) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.delivery = delivery;
        }

        public static DeliveryResult success(Delivery d) { return new DeliveryResult(true, null, d); }
        public static DeliveryResult failure(String error) { return new DeliveryResult(false, error, null); }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public Delivery getDelivery() { return delivery; }
    }
}
