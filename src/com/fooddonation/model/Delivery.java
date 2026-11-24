package com.fooddonation.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Concrete domain model representing a physical food courier rescue mission.
 * Links the claimed food donation to the assigned volunteer courier,
 * recording precise timestamps for each milestone in the delivery lifecycle.
 *
 * OOP Concept: Interface Implementation (implements Trackable)
 */
public class Delivery implements Trackable {

    private int id;
    private int claimId;
    private int volunteerId;
    private DeliveryStatus status;
    private LocalDateTime pickedAt;
    private LocalDateTime deliveredAt;
    private Integer mealsDelivered;
    private LocalDateTime createdAt; // Timestamp when delivery was accepted

    // Joined presentation properties
    private int donationId;
    private String foodName;
    private String category;
    private int quantity;
    private String donorName;
    private String donorAddress;
    private String donorCity;
    private String ngoName;
    private String ngoAddress;
    private String ngoCity;
    private String volunteerName;
    private String volunteerPhone;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy  hh:mm a");
    private static final DateTimeFormatter SHORT_TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    public Delivery() {
        this.status = DeliveryStatus.ACCEPTED;
        this.createdAt = LocalDateTime.now();
    }

    public Delivery(int id, int claimId, int volunteerId, DeliveryStatus status,
                    LocalDateTime pickedAt, LocalDateTime deliveredAt,
                    Integer mealsDelivered, LocalDateTime createdAt) {
        this.id = id;
        this.claimId = claimId;
        this.volunteerId = volunteerId;
        this.status = (status != null) ? status : DeliveryStatus.ACCEPTED;
        this.pickedAt = pickedAt;
        this.deliveredAt = deliveredAt;
        this.mealsDelivered = mealsDelivered;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClaimId() { return claimId; }
    public void setClaimId(int claimId) { this.claimId = claimId; }

    public int getVolunteerId() { return volunteerId; }
    public void setVolunteerId(int volunteerId) { this.volunteerId = volunteerId; }

    // OOP Concept: Interface Implementation
    @Override
    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    // OOP Concept: Interface Implementation
    @Override
    public void updateStatus(String newStatus) {
        if (newStatus != null) {
            this.status = DeliveryStatus.fromString(newStatus);
        }
    }

    public LocalDateTime getPickedAt() { return pickedAt; }
    public void setPickedAt(LocalDateTime pickedAt) { this.pickedAt = pickedAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public Integer getMealsDelivered() { return mealsDelivered; }
    public void setMealsDelivered(Integer mealsDelivered) { this.mealsDelivered = mealsDelivered; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getDonationId() { return donationId; }
    public void setDonationId(int donationId) { this.donationId = donationId; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }

    public String getDonorAddress() { return donorAddress; }
    public void setDonorAddress(String donorAddress) { this.donorAddress = donorAddress; }

    public String getDonorCity() { return donorCity; }
    public void setDonorCity(String donorCity) { this.donorCity = donorCity; }

    public String getNgoName() { return ngoName; }
    public void setNgoName(String ngoName) { this.ngoName = ngoName; }

    public String getNgoAddress() { return ngoAddress; }
    public void setNgoAddress(String ngoAddress) { this.ngoAddress = ngoAddress; }

    public String getNgoCity() { return ngoCity; }
    public void setNgoCity(String ngoCity) { this.ngoCity = ngoCity; }

    public String getVolunteerName() { return volunteerName; }
    public void setVolunteerName(String volunteerName) { this.volunteerName = volunteerName; }

    public String getVolunteerPhone() { return volunteerPhone; }
    public void setVolunteerPhone(String volunteerPhone) { this.volunteerPhone = volunteerPhone; }

    // Formatting Helpers
    public String getFormattedCreatedAt() {
        return (createdAt != null) ? createdAt.format(TIME_FMT) : "Pending";
    }

    public String getFormattedPickedAt() {
        return (pickedAt != null) ? pickedAt.format(TIME_FMT) : "Not yet picked up";
    }

    public String getFormattedDeliveredAt() {
        return (deliveredAt != null) ? deliveredAt.format(TIME_FMT) : "In transit";
    }

    public String getShortPickedAt() {
        return (pickedAt != null) ? pickedAt.format(SHORT_TIME_FMT) : "—";
    }

    public String getShortDeliveredAt() {
        return (deliveredAt != null) ? deliveredAt.format(SHORT_TIME_FMT) : "—";
    }

    public boolean isCompleted() {
        return status == DeliveryStatus.DELIVERED;
    }

    public boolean isPickedUp() {
        return status == DeliveryStatus.PICKED_UP || status == DeliveryStatus.DELIVERED;
    }
}
