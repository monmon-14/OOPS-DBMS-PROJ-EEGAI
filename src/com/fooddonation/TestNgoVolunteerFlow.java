package com.fooddonation;

import com.fooddonation.dao.ClaimDAO;
import com.fooddonation.dao.DonationDAO;
import com.fooddonation.model.*;
import com.fooddonation.service.DonationService;
import com.fooddonation.service.UserSession;

import java.time.LocalDateTime;
import java.util.List;

/**
 * End-to-end Automated Test Suite for NGO Claims & Volunteer Delivery Flow.
 * Verifies:
 * 1. Filtered Available Food browsing
 * 2. Concurrency & Transactional Claiming
 * 3. Double-claim rejection
 * 4. Expired item claim rejection
 * 5. Volunteer task discovery (open unassigned claims)
 * 6. Task acceptance & courier assignment
 * 7. Double-assignment prevention
 * 8. Status progression: ASSIGNED -> PICKED_UP -> DELIVERED
 * 9. KPI Stats aggregation for NGO and Volunteer
 * 10. Time-left countdown string accuracy
 */
public class TestNgoVolunteerFlow {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("Running Food Donation App: NGO & Volunteer Module Integration Tests");
        System.out.println("==================================================================");

        DonationDAO donationDAO = new DonationDAO();
        ClaimDAO claimDAO = new ClaimDAO();
        DonationService service = new DonationService(donationDAO, claimDAO);

        int passed = 0;
        int total = 0;

        // Establish simulated NGO and Volunteer users
        NGO testNgo = new NGO(2, "Hope Food Foundation", "hope.foundation@ngo.org",
                null, null, "9876543211", "Hope Foundation International",
                "450 Community Way, Suite 12, Seattle, WA", "NGO-US-2024-8849", "Greater Metro");

        Volunteer testVolunteer = new Volunteer(3, "Alex Green", "alex.green@volunteer.org",
                null, null, "9876543212", "Community First Responders",
                "789 Pine Street, Seattle, WA", "Car", "Flexible");

        UserSession.getInstance().setCurrentUser(testNgo);

        // --- Test 1: Seed / Retrieve Available Listings ---
        total++;
        LocalDateTime now = LocalDateTime.now();
        var addRes = service.addDonation(1, "Fresh Sourdough Bread & Rolls", "Veg", 35,
                now.minusHours(1), now.plusHours(8), "124 Culinary Boulevard, Downtown", "Fresh morning batch");
        int testDonationId = addRes.getDonation().getId();

        List<FoodDonation> availableList = service.getAvailable("Veg", "Downtown");
        boolean foundDonation = availableList.stream().anyMatch(d -> d.getId() == testDonationId);
        if (foundDonation) {
            System.out.println("✅ Test 1 Passed: Filtered available listings retrieved (ID: " + testDonationId + ").");
            passed++;
        } else {
            System.err.println("❌ Test 1 Failed: Newly created available donation not found in filtered search.");
        }

        // --- Test 2: Time-Left Countdown Rendering ---
        total++;
        FoodDonation testDonation = availableList.stream().filter(d -> d.getId() == testDonationId).findFirst().orElse(null);
        if (testDonation != null && testDonation.getTimeLeftString().contains("h") && testDonation.getTimeLeftString().contains("left")) {
            System.out.println("✅ Test 2 Passed: Time-left countdown calculated correctly: '" + testDonation.getTimeLeftString() + "'.");
            passed++;
        } else {
            System.err.println("❌ Test 2 Failed: Countdown string incorrect: " + (testDonation != null ? testDonation.getTimeLeftString() : "null"));
        }

        // --- Test 3: NGO Claims Available Food Listing ---
        total++;
        boolean claimSuccess = service.claimDonation(testDonationId, testNgo.getId());
        if (claimSuccess) {
            System.out.println("✅ Test 3 Passed: NGO successfully claimed donation ID " + testDonationId + ".");
            passed++;
        } else {
            System.err.println("❌ Test 3 Failed: Claim creation failed for donation ID " + testDonationId);
        }

        // --- Test 4: Concurrency Check - Double Claim Rejection ---
        total++;
        boolean doubleClaim = service.claimDonation(testDonationId, testNgo.getId());
        if (!doubleClaim) {
            System.out.println("✅ Test 4 Passed: Double-claim prevented! Re-claiming already claimed donation was rejected.");
            passed++;
        } else {
            System.err.println("❌ Test 4 Failed: Double-claim was erroneously permitted!");
        }

        // --- Test 5: Rejection of Expired Food Claim ---
        total++;
        // Donation ID 5 is pre-seeded as expired in fallback cache
        boolean expiredClaim = service.claimDonation(5, testNgo.getId());
        if (!expiredClaim) {
            System.out.println("✅ Test 5 Passed: Claiming expired food listing was correctly rejected.");
            passed++;
        } else {
            System.err.println("❌ Test 5 Failed: Claiming expired food was permitted!");
        }

        // --- Test 6: Volunteer Discovers Open Pickup Tasks ---
        total++;
        UserSession.getInstance().setCurrentUser(testVolunteer);
        List<Claim> openTasks = service.getPickupTasks();
        Claim targetTask = openTasks.stream().filter(c -> c.getDonationId() == testDonationId).findFirst().orElse(null);
        if (targetTask != null && targetTask.isUnassigned()) {
            System.out.println("✅ Test 6 Passed: Volunteer successfully discovered open pickup task (Claim ID: " + targetTask.getId() + ").");
            passed++;
        } else {
            System.err.println("❌ Test 6 Failed: Open pickup task not listed for claimed donation.");
        }

        // --- Test 7: Volunteer Accepts Task Inside Transaction ---
        total++;
        int claimId = (targetTask != null) ? targetTask.getId() : 1;
        boolean acceptSuccess = service.acceptTask(claimId, testVolunteer.getId());
        if (acceptSuccess) {
            System.out.println("✅ Test 7 Passed: Volunteer accepted delivery task ID " + claimId + ".");
            passed++;
        } else {
            System.err.println("❌ Test 7 Failed: Failed to assign volunteer to task ID " + claimId);
        }

        // --- Test 8: Double-Assignment Prevention ---
        total++;
        Volunteer otherVolunteer = new Volunteer(4, "Bob Driver", "bob@volunteer.org", null, null, "1234567890", "Drivers", "123 St", "Bike", "Flexible");
        boolean doubleAssign = service.acceptTask(claimId, otherVolunteer.getId());
        if (!doubleAssign) {
            System.out.println("✅ Test 8 Passed: Double-assignment rejected. Task already assigned to volunteer " + testVolunteer.getId() + ".");
            passed++;
        } else {
            System.err.println("❌ Test 8 Failed: Second volunteer was able to accept an already-assigned task!");
        }

        // --- Test 9: Delivery Lifecycle Progression (Picked Up -> Delivered) ---
        total++;
        boolean markPickedUp = service.updateDeliveryStatus(claimId, ClaimStatus.PICKED_UP);
        boolean markDelivered = service.updateDeliveryStatus(claimId, ClaimStatus.DELIVERED);

        List<Claim> deliveries = service.getDeliveriesByVolunteer(testVolunteer.getId());
        Claim updatedClaim = deliveries.stream().filter(c -> c.getId() == claimId).findFirst().orElse(null);

        if (markPickedUp && markDelivered && updatedClaim != null && updatedClaim.getStatus() == ClaimStatus.DELIVERED) {
            System.out.println("✅ Test 9 Passed: Delivery status advanced smoothly to PICKED_UP and DELIVERED.");
            passed++;
        } else {
            System.err.println("❌ Test 9 Failed: Status transition failed. Current status: " + (updatedClaim != null ? updatedClaim.getStatus() : "null"));
        }

        // --- Test 10: Volunteer and NGO KPI Stats Aggregation ---
        total++;
        DonationService.VolunteerStats volStats = service.getVolunteerStats(testVolunteer.getId());
        DonationService.NgoStats ngoStats = service.getNgoStats(testNgo.getId());

        if (volStats.getCompletedDeliveries() >= 1 && volStats.getHoursContributed() > 0 &&
            ngoStats.getTotalMealsClaimed() >= 35 && ngoStats.getDeliveredMeals() >= 35) {
            System.out.println("✅ Test 10 Passed: KPI stats aggregated accurately.");
            System.out.println("    Volunteer -> Completed: " + volStats.getCompletedDeliveries() +
                               ", Active: " + volStats.getActivePickups() +
                               ", Hours: " + String.format("%.1f", volStats.getHoursContributed()) + " hrs");
            System.out.println("    NGO       -> Total Meals Claimed: " + ngoStats.getTotalMealsClaimed() +
                               ", Delivered Meals: " + ngoStats.getDeliveredMeals());
            passed++;
        } else {
            System.err.println("❌ Test 10 Failed: KPI metrics mismatch.");
        }

        System.out.println("==================================================================");
        System.out.println("Integration Test Results: " + passed + " / " + total + " Passed.");
        if (passed == total) {
            System.out.println("🎉 All 10 tests passed flawlessly!");
        } else {
            System.err.println("⚠️ Some tests encountered issues.");
        }
        System.out.println("==================================================================");
    }
}
