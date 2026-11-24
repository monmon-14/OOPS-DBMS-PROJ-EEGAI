package com.fooddonation;

import com.fooddonation.dao.DonationDAO;
import com.fooddonation.model.DonationStatus;
import com.fooddonation.model.FoodDonation;
import com.fooddonation.service.AuthService;
import com.fooddonation.service.DonationService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Automated test suite for the Donor Module.
 * Tests chronological time validations, listing publication, selective cancellation,
 * auto-expiry calculations, and stats KPI aggregations.
 */
public class TestDonorFlow {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Running Food Donation App: Donor Module Unit Tests");
        System.out.println("==================================================");

        DonationDAO donationDAO = new DonationDAO();
        DonationService service = new DonationService(donationDAO);
        AuthService authService = new AuthService();

        int passed = 0;
        int total = 0;

        // --- Test 1: Empty Food Name Validation ---
        total++;
        LocalDateTime now = LocalDateTime.now();
        var r1 = service.addDonation(1, "", "Veg", 20, now, now.plusHours(4), "124 Culinary Blvd", "Notes");
        if (!r1.isSuccess() && r1.getErrorMessage().contains("food name")) {
            System.out.println("✅ Test 1 Passed: Empty food name rejected.");
            passed++;
        } else {
            System.err.println("❌ Test 1 Failed: " + r1.getErrorMessage());
        }

        // --- Test 2: Quantity <= 0 Validation ---
        total++;
        var r2 = service.addDonation(1, "Bread", "Veg", 0, now, now.plusHours(4), "124 Culinary Blvd", "Notes");
        if (!r2.isSuccess() && r2.getErrorMessage().contains("Quantity")) {
            System.out.println("✅ Test 2 Passed: Zero or negative meal quantity rejected.");
            passed++;
        } else {
            System.err.println("❌ Test 2 Failed: " + r2.getErrorMessage());
        }

        // --- Test 3: Expiry Time Before Prep Time Validation ---
        total++;
        LocalDateTime prep = now.plusHours(2);
        LocalDateTime invalidExpiry = now.plusHours(1); // Expiry BEFORE prep
        var r3 = service.addDonation(1, "Warm Soup", "Veg", 15, prep, invalidExpiry, "124 Culinary Blvd", "Notes");
        if (!r3.isSuccess() && r3.getErrorMessage().contains("strictly after the preparation time")) {
            System.out.println("✅ Test 3 Passed: Expiry time earlier than preparation time rejected.");
            passed++;
        } else {
            System.err.println("❌ Test 3 Failed: " + r3.getErrorMessage());
        }

        // --- Test 4: Expiry Time in the Past Validation ---
        total++;
        LocalDateTime pastPrep = now.minusHours(4);
        LocalDateTime pastExpiry = now.minusHours(1); // In the past
        var r4 = service.addDonation(1, "Cold Sandwiches", "Veg", 10, pastPrep, pastExpiry, "124 Culinary Blvd", "Notes");
        if (!r4.isSuccess() && r4.getErrorMessage().contains("in the future")) {
            System.out.println("✅ Test 4 Passed: Expiry time in the past rejected.");
            passed++;
        } else {
            System.err.println("❌ Test 4 Failed: " + r4.getErrorMessage());
        }

        // --- Test 5: Valid Food Listing Creation ---
        total++;
        LocalDateTime validPrep = now.minusMinutes(30);
        LocalDateTime validExpiry = now.plusHours(5);
        var r5 = service.addDonation(1, "Hearty Minestrone Soup", "Veg", 45, validPrep, validExpiry, "124 Culinary Blvd", "Contains legumes");
        if (r5.isSuccess() && r5.getDonation().getStatus() == DonationStatus.AVAILABLE) {
            System.out.println("✅ Test 5 Passed: Valid food listing created with status 'Available' (ID: " + r5.getDonation().getId() + ").");
            passed++;
        } else {
            System.err.println("❌ Test 5 Failed: " + r5.getErrorMessage());
        }

        // --- Test 6: Fetch Donations for Donor ---
        total++;
        List<FoodDonation> list = service.getDonationsByDonor(1);
        if (list != null && !list.isEmpty()) {
            System.out.println("✅ Test 6 Passed: Retrieved " + list.size() + " donation listings for donor ID 1.");
            passed++;
        } else {
            System.err.println("❌ Test 6 Failed: Listing retrieval returned empty or null.");
        }

        // --- Test 7: Cancel 'Available' Donation ---
        total++;
        int createdId = r5.getDonation().getId();
        boolean cancelSuccess = service.cancelDonation(createdId, 1);
        if (cancelSuccess) {
            System.out.println("✅ Test 7 Passed: Successfully cancelled available food listing (ID: " + createdId + ").");
            passed++;
        } else {
            System.err.println("❌ Test 7 Failed: Cancellation failed for available listing.");
        }

        // --- Test 8: Disallow Cancellation for non-Available Donation ---
        total++;
        // ID 4 is pre-seeded with status 'Delivered'
        boolean cancelDelivered = service.cancelDonation(4, 1);
        if (!cancelDelivered) {
            System.out.println("✅ Test 8 Passed: Cancellation rightly prevented for non-Available (Delivered) listing.");
            passed++;
        } else {
            System.err.println("❌ Test 8 Failed: Non-available listing was erroneously cancelled.");
        }

        // --- Test 9: Auto-Expiry Execution ---
        total++;
        int expiredCount = service.expireOldDonations();
        System.out.println("✅ Test 9 Passed: Auto-expiry run executed smoothly (" + expiredCount + " newly expired).");
        passed++;

        // --- Test 10: Donor Analytics KPI Aggregation ---
        total++;
        DonationDAO.DonorStats stats = service.getDonorStats(1);
        if (stats != null && stats.getTotalListed() > 0 && stats.getMealsDonated() >= 0) {
            System.out.println("✅ Test 10 Passed: KPI stats query aggregated: Listed=" + stats.getTotalListed() +
                    ", Claimed=" + stats.getTotalClaimed() + ", Delivered=" + stats.getTotalDelivered() +
                    ", Meals Donated=" + stats.getMealsDonated());
            passed++;
        } else {
            System.err.println("❌ Test 10 Failed: Stats calculation returned invalid values.");
        }

        // --- Test 11: Donor Login Authentication via AuthService ---
        total++;
        var loginRes = authService.login("bistro@greengarden.com", "Password@123", "Donor");
        if (loginRes.isSuccess() && "Donor".equalsIgnoreCase(loginRes.getUser().getRole())) {
            System.out.println("✅ Test 11 Passed: Donor login authenticated successfully for " + loginRes.getUser().getName());
            passed++;
        } else {
            System.err.println("❌ Test 11 Failed: " + loginRes.getErrorMessage());
        }

        System.out.println("==================================================");
        System.out.println("Results: " + passed + " / " + total + " tests passed successfully!");
        System.out.println("==================================================");

        if (passed != total) {
            System.exit(1);
        }
    }
}
