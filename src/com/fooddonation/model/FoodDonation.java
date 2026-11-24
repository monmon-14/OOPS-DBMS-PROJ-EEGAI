package com.fooddonation.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model class representing a Food Donation listing created by a Donor.
 *
 * OOP Concept: Interface Implementation (implements Trackable)
 */
public class FoodDonation implements Trackable {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Food Category Items in English
    public static final String[] TAMIL_FOOD_ITEMS = {
            "Idli & Sambar",
            "Pongal & Vada",
            "Dosa Varieties",
            "Sambar Rice",
            "Curd Rice",
            "Lemon Rice",
            "Puliyotharai (Tamarind Rice)",
            "Biryani",
            "Traditional Full Meals",
            "Chapati & Kurma"
    };

    // Festival / Occasion Options
    public static final String[] FESTIVAL_OCCASIONS = {
            "Regular",
            "Pongal Festival",
            "Deepavali",
            "Karthigai Deepam",
            "Wedding Feast",
            "Temple Festival"
    };

    private int id;
    private int donorId;
    private String foodName;
    private String category; // "Veg" or "Non-Veg"
    private int quantity;    // Number of meals
    private LocalDateTime prepTime;
    private LocalDateTime expiryTime;
    private String pickupAddress;
    private DonationStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private String donorName; // Display property for donor
    private String occasion;  // Optional Festival / Occasion
    private boolean isDemo = false; // Demo flag for fictional Tamil culture seed listings

    public FoodDonation() {
        this.status = DonationStatus.AVAILABLE;
        this.createdAt = LocalDateTime.now();
    }

    public String getTimeLeftString() {
        if (expiryTime == null) return "N/A";
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(expiryTime)) {
            return "Expired";
        }
        java.time.Duration diff = java.time.Duration.between(now, expiryTime);
        long hours = diff.toHours();
        long minutes = diff.toMinutesPart();
        if (hours > 0) {
            return hours + "h " + minutes + "m left";
        } else {
            return minutes + "m left";
        }
    }

    public String getDonorName() {
        return (donorName != null && !donorName.isEmpty()) ? donorName : "Green Garden Bistro";
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public FoodDonation(int id, int donorId, String foodName, String category, int quantity,
                        LocalDateTime prepTime, LocalDateTime expiryTime, String pickupAddress,
                        DonationStatus status, String notes, LocalDateTime createdAt) {
        this(id, donorId, foodName, category, quantity, prepTime, expiryTime, pickupAddress, status, notes, createdAt, null, false);
    }

    public FoodDonation(int id, int donorId, String foodName, String category, int quantity,
                        LocalDateTime prepTime, LocalDateTime expiryTime, String pickupAddress,
                        DonationStatus status, String notes, LocalDateTime createdAt,
                        String occasion, boolean isDemo) {
        this.id = id;
        this.donorId = donorId;
        this.foodName = foodName;
        this.category = (category != null) ? category : "Veg";
        this.quantity = quantity;
        this.prepTime = prepTime;
        this.expiryTime = expiryTime;
        this.pickupAddress = pickupAddress;
        this.status = (status != null) ? status : DonationStatus.AVAILABLE;
        this.notes = notes;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
        this.occasion = occasion;
        this.isDemo = isDemo;
    }

    public boolean isCancelable() {
        return this.status == DonationStatus.AVAILABLE;
    }

    public boolean isExpired() {
        return this.expiryTime != null && LocalDateTime.now().isAfter(this.expiryTime);
    }

    public String getFormattedPrepTime() {
        return (prepTime != null) ? prepTime.format(DATE_TIME_FORMATTER) : "N/A";
    }

    public String getFormattedExpiryTime() {
        return (expiryTime != null) ? expiryTime.format(DATE_TIME_FORMATTER) : "N/A";
    }

    // --- Getters and Setters ---
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
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

    public LocalDateTime getPrepTime() {
        return prepTime;
    }

    public void setPrepTime(LocalDateTime prepTime) {
        this.prepTime = prepTime;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    // OOP Concept: Interface Implementation
    @Override
    public DonationStatus getStatus() {
        return status;
    }

    public void setStatus(DonationStatus status) {
        this.status = status;
    }

    // OOP Concept: Interface Implementation
    @Override
    public void updateStatus(String newStatus) {
        if (newStatus != null) {
            this.status = DonationStatus.fromString(newStatus);
        }
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getOccasion() {
        return occasion;
    }

    public void setOccasion(String occasion) {
        this.occasion = occasion;
    }

    public boolean isDemo() {
        return isDemo;
    }

    public void setDemo(boolean demo) {
        isDemo = demo;
    }

    public String getFestivalBadge() {
        if (occasion == null || occasion.trim().isEmpty() || occasion.equalsIgnoreCase("None") || occasion.equalsIgnoreCase("Regular")) {
            return null;
        }
        String occLower = occasion.trim().toLowerCase();
        if (occLower.contains("pongal")) {
            return "Pongal Festival";
        } else if (occLower.contains("deepavali")) {
            return "Deepavali";
        } else if (occLower.contains("karthigai")) {
            return "Karthigai Deepam";
        } else if (occLower.contains("wedding")) {
            return "Wedding Feast";
        } else if (occLower.contains("temple")) {
            return "Temple Festival";
        }
        return occasion;
    }

    @Override
    public String toString() {
        return "FoodDonation{" +
                "id=" + id +
                ", foodName='" + foodName + '\'' +
                ", category='" + category + '\'' +
                ", quantity=" + quantity +
                ", status=" + status +
                '}';
    }
}
