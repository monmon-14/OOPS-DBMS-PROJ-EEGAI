package com.fooddonation.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model class representing an NGO Claim on a Food Donation listing,
 * including courier assignment and delivery tracking.
 */
public class Claim {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private int id;
    private int donationId;
    private int ngoId;
    private Integer volunteerId; // Nullable until a volunteer accepts the rescue task
    private ClaimStatus status;
    private LocalDateTime claimedAt;

    // Associated relational display properties
    private String foodName;
    private String category;
    private int quantity;
    private String donorName;
    private String pickupAddress;
    private String ngoName;
    private String dropoffAddress;
    private String volunteerName;

    public Claim() {
        this.status = ClaimStatus.CLAIMED;
        this.claimedAt = LocalDateTime.now();
    }

    public Claim(int id, int donationId, int ngoId, Integer volunteerId, ClaimStatus status, LocalDateTime claimedAt) {
        this.id = id;
        this.donationId = donationId;
        this.ngoId = ngoId;
        this.volunteerId = volunteerId;
        this.status = (status != null) ? status : ClaimStatus.CLAIMED;
        this.claimedAt = (claimedAt != null) ? claimedAt : LocalDateTime.now();
    }

    public boolean isUnassigned() {
        return this.volunteerId == null || this.volunteerId <= 0;
    }

    public String getFormattedClaimedAt() {
        return (claimedAt != null) ? claimedAt.format(FORMATTER) : "N/A";
    }

    // --- Getters & Setters ---
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDonationId() {
        return donationId;
    }

    public void setDonationId(int donationId) {
        this.donationId = donationId;
    }

    public int getNgoId() {
        return ngoId;
    }

    public void setNgoId(int ngoId) {
        this.ngoId = ngoId;
    }

    public Integer getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(Integer volunteerId) {
        this.volunteerId = volunteerId;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public LocalDateTime getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(LocalDateTime claimedAt) {
        this.claimedAt = claimedAt;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getNgoName() {
        return ngoName;
    }

    public void setNgoName(String ngoName) {
        this.ngoName = ngoName;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }

    public String getVolunteerName() {
        return volunteerName;
    }

    public void setVolunteerName(String volunteerName) {
        this.volunteerName = volunteerName;
    }

    @Override
    public String toString() {
        return "Claim{" +
                "id=" + id +
                ", donationId=" + donationId +
                ", ngoId=" + ngoId +
                ", volunteerId=" + volunteerId +
                ", status=" + status +
                '}';
    }
}
