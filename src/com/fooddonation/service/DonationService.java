package com.fooddonation.service;

import com.fooddonation.dao.ClaimDAO;
import com.fooddonation.dao.DonationDAO;
import com.fooddonation.dao.UserDAO;
import com.fooddonation.exception.AlreadyClaimedException;
import com.fooddonation.exception.DonationExpiredException;
import com.fooddonation.model.Claim;
import com.fooddonation.model.ClaimStatus;
import com.fooddonation.model.DonationStatus;
import com.fooddonation.model.FoodDonation;
import com.fooddonation.model.NGO;
import com.fooddonation.model.User;
import com.fooddonation.util.StringUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service orchestrating Food Donation listings, NGO claims,
 * courier task dispatching, selective cancellations, and automatic expiration.
 */
public class DonationService {

    private final DonationDAO donationDAO;
    private final ClaimDAO claimDAO;
    private final UserDAO userDAO;

    public DonationService() {
        this.donationDAO = new DonationDAO();
        this.claimDAO = new ClaimDAO();
        this.userDAO = new UserDAO();
    }

    public DonationService(DonationDAO donationDAO) {
        this.donationDAO = donationDAO;
        this.claimDAO = new ClaimDAO();
        this.userDAO = new UserDAO();
    }

    public DonationService(DonationDAO donationDAO, ClaimDAO claimDAO) {
        this.donationDAO = donationDAO;
        this.claimDAO = claimDAO;
        this.userDAO = new UserDAO();
    }

    public DonationService(DonationDAO donationDAO, ClaimDAO claimDAO, UserDAO userDAO) {
        this.donationDAO = donationDAO;
        this.claimDAO = claimDAO;
        this.userDAO = (userDAO != null) ? userDAO : new UserDAO();
    }

    /**
     * Validates and creates a new Food Donation listing.
     */
    public DonationResult addDonation(int donorId, String foodName, String category, int quantity,
                                      LocalDateTime prepTime, LocalDateTime expiryTime,
                                      String pickupAddress, String notes) {
        return addDonation(donorId, foodName, category, quantity, prepTime, expiryTime, pickupAddress, notes, "None", false);
    }

    /**
     * OOP Concept: Method Overloading (Extended parameter signature supporting Tamil Festival / Occasion and Demo flag)
     */
    public DonationResult addDonation(int donorId, String foodName, String category, int quantity,
                                      LocalDateTime prepTime, LocalDateTime expiryTime,
                                      String pickupAddress, String notes, String occasion, boolean isDemo) {
        if (donorId <= 0) {
            return DonationResult.failure("Invalid donor authentication session.");
        }
        if (foodName == null || foodName.trim().isEmpty()) {
            return DonationResult.failure("Please enter the food name or item description.");
        }
        if (category == null || (!category.equalsIgnoreCase("Veg") && !category.equalsIgnoreCase("Non-Veg"))) {
            return DonationResult.failure("Please select a valid category (Veg or Non-Veg).");
        }
        if (quantity <= 0) {
            return DonationResult.failure("Quantity must be at least 1 meal portion.");
        }
        if (prepTime == null) {
            return DonationResult.failure("Preparation time is required.");
        }
        if (expiryTime == null) {
            return DonationResult.failure("Expiry time is required.");
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Validation: Expiry time must be strictly AFTER preparation time
        if (!expiryTime.isAfter(prepTime)) {
            return DonationResult.failure("Expiry time must be strictly after the preparation time.");
        }

        // 2. Validation: Expiry time must be in the future
        if (!expiryTime.isAfter(now)) {
            return DonationResult.failure("Expiry time must be in the future (food cannot already be expired).");
        }

        if (pickupAddress == null || pickupAddress.trim().isEmpty()) {
            return DonationResult.failure("Pickup address cannot be empty.");
        }

        FoodDonation donation = new FoodDonation(
                0,
                donorId,
                foodName.trim(),
                category.trim(),
                quantity,
                prepTime,
                expiryTime,
                pickupAddress.trim(),
                DonationStatus.AVAILABLE,
                (notes != null) ? notes.trim() : "",
                now,
                (occasion != null) ? occasion.trim() : "None",
                isDemo
        );

        boolean saved = donationDAO.insertDonation(donation);
        if (saved) {
            return DonationResult.success(donation);
        } else {
            return DonationResult.failure("Failed to publish food donation to database.");
        }
    }

    // =========================================================================
    // OOP Concept: Method Overloading (getDonations with 3 distinct parameter signatures)
    // =========================================================================

    /**
     * OOP Concept: Method Overloading (Overload 1: Parameterless — returns all active available donations)
     */
    public List<FoodDonation> getDonations() {
        return getAvailable("All", "");
    }

    /**
     * OOP Concept: Method Overloading (Overload 2: By Donor ID — returns listings posted by this donor)
     */
    public List<FoodDonation> getDonations(int donorId) {
        return getDonationsByDonor(donorId);
    }

    /**
     * OOP Concept: Method Overloading (Overload 3: By City — case-insensitive city search)
     */
    public List<FoodDonation> getDonations(String city) {
        return getAvailable("All", city);
    }

    /**
     * Retrieves all active, unexpired available donations with optional filtering and search.
     */
    public List<FoodDonation> getAvailable(String categoryFilter, String citySearch) {
        donationDAO.expireOldDonations();
        List<FoodDonation> raw = donationDAO.findAvailable(categoryFilter, citySearch);

        // Filter using StringUtil for case-insensitive city matching if specified
        if (citySearch != null && !citySearch.trim().isEmpty()) {
            List<FoodDonation> matched = new ArrayList<>();
            for (FoodDonation d : raw) {
                if (StringUtil.containsCityIgnoreCase(d.getPickupAddress(), citySearch)) {
                    matched.add(d);
                }
            }
            return matched;
        }
        return raw;
    }

    /**
     * Atomically claims a donation for an NGO inside a DB transaction.
     * Prevents double-claims and rejects already-claimed or expired items.
     *
     * Rule: Only PARTNERED NGOs can claim food. If an NGO has partner_status = LISTED,
     * the claim is strictly rejected.
     *
     * OOP Concept: Custom Exceptions (try/catch handling for AlreadyClaimedException & DonationExpiredException)
     */
    public boolean claimDonation(int donationId, int ngoId) {
        if (donationId <= 0 || ngoId <= 0) return false;

        try {
            // Check partner status of NGO
            for (NGO n : userDAO.getAllNgos()) {
                if (n.getId() == ngoId) {
                    if (!n.isPartnered()) {
                        System.err.println("[DonationService] Claim rejected: NGO '" + n.getName() + "' has LISTED status (not yet partnered).");
                        return false;
                    }
                    break;
                }
            }

            donationDAO.expireOldDonations();

            // Find listing to verify state before claim
            List<FoodDonation> available = donationDAO.findAvailable("All", "");
            boolean found = false;
            for (FoodDonation d : available) {
                if (d.getId() == donationId) {
                    found = true;
                    if (d.isExpired()) {
                        throw new DonationExpiredException(donationId, "Cannot claim food listing #" + donationId + " because it has expired.");
                    }
                    if (d.getStatus() != DonationStatus.AVAILABLE) {
                        throw new AlreadyClaimedException(donationId, "Listing #" + donationId + " is already in state: " + d.getStatus());
                    }
                    break;
                }
            }

            return claimDAO.createClaim(donationId, ngoId);

        } catch (DonationExpiredException e) {
            System.err.println("[DonationService] Caught Custom Exception: " + e.getMessage());
            return false;
        } catch (AlreadyClaimedException e) {
            System.err.println("[DonationService] Caught Custom Exception: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("[DonationService] Claim error: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // OOP Concept: Recursion
    // =========================================================================

    /**
     * OOP Concept: Recursion (Recursive meal quantity summation across donation list)
     */
    public int sumMeals(List<FoodDonation> list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            return 0; // Base condition
        }
        // Recursive step: current item meals + sum of remaining items
        return list.get(index).getQuantity() + sumMeals(list, index + 1);
    }

    /**
     * OOP Concept: Recursion (Recursive binary search on donations sorted by expiry timestamp)
     */
    public int binarySearchByExpiry(List<FoodDonation> sortedList, LocalDateTime targetExpiry, int low, int high) {
        if (sortedList == null || low > high || targetExpiry == null) {
            return -1; // Element not found base condition
        }
        int mid = low + (high - low) / 2;
        LocalDateTime midTime = sortedList.get(mid).getExpiryTime();
        if (midTime == null) return -1;

        int cmp = midTime.compareTo(targetExpiry);
        if (cmp == 0) {
            return mid; // Found match
        } else if (cmp > 0) {
            return binarySearchByExpiry(sortedList, targetExpiry, low, mid - 1); // Recurse left
        } else {
            return binarySearchByExpiry(sortedList, targetExpiry, mid + 1, high); // Recurse right
        }
    }

    // =========================================================================
    // OOP Concept: Collections (ArrayList, HashMap, Comparator)
    // =========================================================================

    /**
     * OOP Concept: Collections & Comparator (Sorts donations chronologically by expiry time)
     */
    public List<FoodDonation> getDonationsSortedByExpiry(List<FoodDonation> list) {
        if (list == null) return new ArrayList<>();
        List<FoodDonation> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparing(FoodDonation::getExpiryTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return sorted;
    }

    /**
     * OOP Concept: Collections (HashMap for aggregating statistics by status)
     * Control Flow: Uses switch statement to handle polymorphic donation statuses.
     */
    public Map<String, Integer> getStatusCountsMap(List<FoodDonation> list) {
        Map<String, Integer> counts = new HashMap<>();
        if (list == null) return counts;

        for (FoodDonation d : list) {
            DonationStatus st = d.getStatus();
            String key;
            // OOP Concept: Control Flow (Switch statement for status handling)
            switch (st) {
                case AVAILABLE:
                    key = "Available";
                    break;
                case CLAIMED:
                    key = "Claimed";
                    break;
                case PICKED_UP:
                    key = "Picked Up";
                    break;
                case DELIVERED:
                    key = "Delivered";
                    break;
                case EXPIRED:
                    key = "Expired";
                    break;
                case CANCELLED:
                    key = "Cancelled";
                    break;
                default:
                    key = "Other";
                    break;
            }
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
        return counts;
    }

    /**
     * Retrieves all claims created by a specific NGO.
     */
    public List<Claim> getClaimsByNgo(int ngoId) {
        donationDAO.expireOldDonations();
        return claimDAO.findByNgoId(ngoId);
    }

    /**
     * Retrieves open food rescue pickup tasks (claimed donations awaiting courier).
     */
    public List<Claim> getPickupTasks() {
        donationDAO.expireOldDonations();
        return claimDAO.findUnassignedPickupTasks();
    }

    /**
     * Accepts a pickup task, assigning a volunteer courier inside an atomic transaction.
     */
    public boolean acceptTask(int claimId, int volunteerId) {
        if (claimId <= 0 || volunteerId <= 0) return false;
        return claimDAO.assignVolunteer(claimId, volunteerId);
    }

    /**
     * Retrieves deliveries accepted by a specific volunteer courier.
     */
    public List<Claim> getDeliveriesByVolunteer(int volunteerId) {
        donationDAO.expireOldDonations();
        return claimDAO.findDeliveriesByVolunteer(volunteerId);
    }

    /**
     * Updates delivery lifecycle status (e.g. 'Picked Up' or 'Delivered').
     */
    public boolean updateDeliveryStatus(int claimId, ClaimStatus status) {
        return claimDAO.updateClaimStatus(claimId, status);
    }

    /**
     * Retrieves all donations listed by a donor, automatically marking expired items first.
     */
    public List<FoodDonation> getDonationsByDonor(int donorId) {
        donationDAO.expireOldDonations();
        return donationDAO.findByDonorId(donorId);
    }

    /**
     * Retrieves recent donations for a donor dashboard widget.
     */
    public List<FoodDonation> getRecentDonations(int donorId, int limit) {
        donationDAO.expireOldDonations();
        return donationDAO.findRecentByDonorId(donorId, limit);
    }

    /**
     * Cancels an available donation. Only items with status 'Available' can be cancelled.
     */
    public boolean cancelDonation(int donationId, int donorId) {
        return donationDAO.cancelDonation(donationId, donorId);
    }

    /**
     * Auto-marks listings as expired if their expiry time has passed.
     */
    public int expireOldDonations() {
        return donationDAO.expireOldDonations();
    }

    /**
     * Retrieves KPI summary statistics for a donor.
     */
    public DonationDAO.DonorStats getDonorStats(int donorId) {
        donationDAO.expireOldDonations();
        return donationDAO.getDonorStats(donorId);
    }

    /**
     * Calculates KPI statistics for an NGO.
     */
    public NgoStats getNgoStats(int ngoId) {
        List<Claim> claims = getClaimsByNgo(ngoId);
        List<FoodDonation> available = getAvailable("All", "");

        int activeClaims = 0;
        int totalMealsClaimed = 0;
        int deliveredMeals = 0;

        for (Claim c : claims) {
            totalMealsClaimed += c.getQuantity();
            if (c.getStatus() == ClaimStatus.DELIVERED) {
                deliveredMeals += c.getQuantity();
            } else if (c.getStatus() != ClaimStatus.CANCELLED) {
                activeClaims++;
            }
        }

        return new NgoStats(activeClaims, totalMealsClaimed, deliveredMeals, available.size());
    }

    /**
     * Calculates KPI statistics for a Volunteer courier.
     */
    public VolunteerStats getVolunteerStats(int volunteerId) {
        List<Claim> deliveries = getDeliveriesByVolunteer(volunteerId);
        List<Claim> openTasks = getPickupTasks();

        int completed = 0;
        int active = 0;

        for (Claim c : deliveries) {
            if (c.getStatus() == ClaimStatus.DELIVERED) {
                completed++;
            } else if (c.getStatus() != ClaimStatus.CANCELLED) {
                active++;
            }
        }

        double hoursLogged = completed * 1.5 + (active * 0.5);
        return new VolunteerStats(completed, active, openTasks.size(), hoursLogged);
    }

    // --- Statistics DTOs ---

    public static class NgoStats {
        private final int activeClaims;
        private final int totalMealsClaimed;
        private final int deliveredMeals;
        private final int availableListings;

        public NgoStats(int activeClaims, int totalMealsClaimed, int deliveredMeals, int availableListings) {
            this.activeClaims = activeClaims;
            this.totalMealsClaimed = totalMealsClaimed;
            this.deliveredMeals = deliveredMeals;
            this.availableListings = availableListings;
        }

        public int getActiveClaims() { return activeClaims; }
        public int getTotalMealsClaimed() { return totalMealsClaimed; }
        public int getDeliveredMeals() { return deliveredMeals; }
        public int getAvailableListings() { return availableListings; }
    }

    public static class VolunteerStats {
        private final int completedDeliveries;
        private final int activePickups;
        private final int availableTasks;
        private final double hoursContributed;

        public VolunteerStats(int completedDeliveries, int activePickups, int availableTasks, double hoursContributed) {
            this.completedDeliveries = completedDeliveries;
            this.activePickups = activePickups;
            this.availableTasks = availableTasks;
            this.hoursContributed = hoursContributed;
        }

        public int getCompletedDeliveries() { return completedDeliveries; }
        public int getActivePickups() { return activePickups; }
        public int getAvailableTasks() { return availableTasks; }
        public double getHoursContributed() { return hoursContributed; }
    }

    /**
     * Operation outcome DTO.
     */
    public static class DonationResult {
        private final boolean success;
        private final String errorMessage;
        private final FoodDonation donation;

        private DonationResult(boolean success, String errorMessage, FoodDonation donation) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.donation = donation;
        }

        public static DonationResult success(FoodDonation d) { return new DonationResult(true, null, d); }
        public static DonationResult failure(String error) { return new DonationResult(false, error, null); }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public FoodDonation getDonation() { return donation; }
    }
}
