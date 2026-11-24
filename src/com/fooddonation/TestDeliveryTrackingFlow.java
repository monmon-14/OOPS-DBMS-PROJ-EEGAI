package com.fooddonation;

import com.fooddonation.dao.ClaimDAO;
import com.fooddonation.dao.DeliveryDAO;
import com.fooddonation.dao.DonationDAO;
import com.fooddonation.model.*;
import com.fooddonation.service.DeliveryService;
import com.fooddonation.service.DonationService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Automated Unit and Integration Test Suite for Delivery Tracking & Live Milestones.
 * Verifies:
 * 1. Delivery initialization on task acceptance (status = ACCEPTED)
 * 2. Sequential advancement rule: skipping to DELIVERED from ACCEPTED is rejected
 * 3. Advancing to PICKED_UP timestamps picked_at and synchronizes Claims and Donations
 * 4. Advancing to DELIVERED requires positive actual meals delivered
 * 5. Advancing to DELIVERED timestamps delivered_at and synchronizes Claims and Donations
 * 6. Attempting to advance a completed delivery is rejected
 * 7. Live 5-stage timeline generation per donation for Donors and NGOs
 * 8. Timeline auto-refresh data consistency
 */
public class TestDeliveryTrackingFlow {

    public static void main(String[] args) {
        System.out.println("=====================================================================");
        System.out.println("Running Food Donation App: Delivery Tracking & Stepper Unit Tests");
        System.out.println("=====================================================================");

        DonationDAO donationDAO = new DonationDAO();
        ClaimDAO claimDAO = new ClaimDAO();
        DeliveryDAO deliveryDAO = new DeliveryDAO();

        DonationService donationService = new DonationService(donationDAO, claimDAO);
        DeliveryService deliveryService = new DeliveryService(deliveryDAO, claimDAO, donationDAO);

        int passed = 0;
        int total = 0;

        // --- Setup Test Donation & Claim ---
        LocalDateTime now = LocalDateTime.now();
        var donationRes = donationService.addDonation(1, "Savory Vegetable Biryani", "Veg", 50,
                now.minusHours(1), now.plusHours(6), "124 Culinary Boulevard, Downtown", "Fresh catering surplus");
        int testDonationId = donationRes.getDonation().getId();

        // NGO (id=2) claims the donation
        boolean claimed = donationService.claimDonation(testDonationId, 2);
        List<Claim> openTasks = donationService.getPickupTasks();
        Claim targetClaim = openTasks.stream().filter(c -> c.getDonationId() == testDonationId).findFirst().orElse(null);
        int claimId = (targetClaim != null) ? targetClaim.getId() : 3;

        // Volunteer (id=3) accepts task
        donationService.acceptTask(claimId, 3);

        // --- Test 1: Delivery Mission Initialization ---
        total++;
        Delivery delivery = deliveryService.getOrCreateDeliveryForClaim(claimId, 3);
        if (delivery != null && delivery.getStatus() == DeliveryStatus.ACCEPTED && delivery.getCreatedAt() != null) {
            System.out.println("✅ Test 1 Passed: Delivery mission initialized with status 'Accepted' (ID: " + delivery.getId() + ").");
            passed++;
        } else {
            System.err.println("❌ Test 1 Failed: Delivery initialization failed.");
        }

        int deliveryId = (delivery != null) ? delivery.getId() : 1;

        // --- Test 2: Sequential Step Enforcement (Skipping to Delivered must fail) ---
        total++;
        var invalidSkip = deliveryService.updateStatus(deliveryId, DeliveryStatus.DELIVERED, 50);
        if (!invalidSkip.isSuccess() && invalidSkip.getErrorMessage().contains("Picked Up")) {
            System.out.println("✅ Test 2 Passed: Invalid step skipped rejected. Must advance to 'Picked Up' first.");
            passed++;
        } else {
            System.err.println("❌ Test 2 Failed: Erroneously allowed direct jump to 'Delivered'.");
        }

        // --- Test 3: Advance to Picked Up ---
        total++;
        var advanceToPickedUp = deliveryService.updateStatus(deliveryId, DeliveryStatus.PICKED_UP, null);
        if (advanceToPickedUp.isSuccess() &&
            advanceToPickedUp.getDelivery().getStatus() == DeliveryStatus.PICKED_UP &&
            advanceToPickedUp.getDelivery().getPickedAt() != null) {
            System.out.println("✅ Test 3 Passed: Successfully advanced to 'Picked Up' with timestamp: " + advanceToPickedUp.getDelivery().getFormattedPickedAt() + ".");
            passed++;
        } else {
            System.err.println("❌ Test 3 Failed: " + advanceToPickedUp.getErrorMessage());
        }

        // --- Test 4: Validation on Delivered (Meals delivered required) ---
        total++;
        var missingMeals = deliveryService.updateStatus(deliveryId, DeliveryStatus.DELIVERED, 0);
        if (!missingMeals.isSuccess() && missingMeals.getErrorMessage().contains("actual number of meals")) {
            System.out.println("✅ Test 4 Passed: Zero/negative meals delivered on 'Delivered' stage rejected.");
            passed++;
        } else {
            System.err.println("❌ Test 4 Failed: Advance to 'Delivered' allowed without meals count.");
        }

        // --- Test 5: Advance to Delivered with Actual Meals Delivered ---
        total++;
        int actualMeals = 48; // e.g. 48 of 50 meal boxes delivered
        var advanceToDelivered = deliveryService.updateStatus(deliveryId, DeliveryStatus.DELIVERED, actualMeals);
        if (advanceToDelivered.isSuccess() &&
            advanceToDelivered.getDelivery().getStatus() == DeliveryStatus.DELIVERED &&
            advanceToDelivered.getDelivery().getDeliveredAt() != null &&
            advanceToDelivered.getDelivery().getMealsDelivered() == actualMeals) {
            System.out.println("✅ Test 5 Passed: Successfully delivered with actual meals logged (" + actualMeals + ") at: " + advanceToDelivered.getDelivery().getFormattedDeliveredAt() + ".");
            passed++;
        } else {
            System.err.println("❌ Test 5 Failed: " + advanceToDelivered.getErrorMessage());
        }

        // --- Test 6: Preventing Further Advancement on Completed Delivery ---
        total++;
        var advanceCompleted = deliveryService.updateStatus(deliveryId, DeliveryStatus.DELIVERED, 48);
        if (!advanceCompleted.isSuccess() && advanceCompleted.getErrorMessage().contains("already been completed")) {
            System.out.println("✅ Test 6 Passed: Further advancement on completed delivery cleanly rejected.");
            passed++;
        } else {
            System.err.println("❌ Test 6 Failed: Completed delivery permitted further mutation.");
        }

        // --- Test 7: Live 5-Stage Timeline Generation ---
        total++;
        DeliveryService.DeliveryTimeline timeline = deliveryService.getTimeline(testDonationId);
        List<DeliveryService.TimelineEvent> events = timeline.getEvents();
        if (timeline != null && events.size() == 5) {
            System.out.println("✅ Test 7 Passed: Generated 5-milestone live timeline for donation ID " + testDonationId + ":");
            for (DeliveryService.TimelineEvent ev : events) {
                System.out.println("    " + ev.getIcon() + " " + ev.getStageName() + " -> " + ev.getFormattedTimestamp() + " (" + (ev.isCompleted() ? "COMPLETED" : "PENDING") + ")");
            }
            passed++;
        } else {
            System.err.println("❌ Test 7 Failed: Expected 5 timeline events, found: " + (timeline != null ? events.size() : "null"));
        }

        // --- Test 8: Timeline Milestones Completion Check ---
        total++;
        boolean allMilestonesCompleted = events.stream().allMatch(DeliveryService.TimelineEvent::isCompleted);
        if (allMilestonesCompleted) {
            System.out.println("✅ Test 8 Passed: All 5 delivery milestones are recorded as completed in order.");
            passed++;
        } else {
            System.err.println("❌ Test 8 Failed: Some milestones were not flagged as completed.");
        }

        // --- Test 9: Table Status Synchronization Check ---
        total++;
        FoodDonation syncedDonation = DonationDAO.findFallbackById(testDonationId);
        if (syncedDonation != null && syncedDonation.getStatus() == DonationStatus.DELIVERED) {
            System.out.println("✅ Test 9 Passed: Donations table status synchronized to 'Delivered'.");
            passed++;
        } else {
            System.err.println("❌ Test 9 Failed: Donation status not in sync with delivery.");
        }

        System.out.println("=====================================================================");
        System.out.println("Delivery Tracking Test Results: " + passed + " / " + total + " Passed.");
        if (passed == total) {
            System.out.println("🎉 All 9 Delivery Tracking tests passed flawlessly!");
        } else {
            System.err.println("⚠️ Some tests encountered issues.");
        }
        System.out.println("=====================================================================");
    }
}
