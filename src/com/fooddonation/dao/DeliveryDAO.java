package com.fooddonation.dao;

import com.fooddonation.model.Claim;
import com.fooddonation.model.Delivery;
import com.fooddonation.model.DeliveryStatus;
import com.fooddonation.model.DonationStatus;
import com.fooddonation.model.FoodDonation;
import com.fooddonation.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Data Access Object for Deliveries and live milestone tracking.
 * Strictly uses PreparedStatements only.
 * Coordinates atomic transactions to keep Donations, Claims, and Deliveries synchronized.
 */
public class DeliveryDAO {

    private static final String SQL_SELECT_BASE =
            "SELECT del.id, del.claim_id, del.volunteer_id, del.status, del.picked_at, del.delivered_at, " +
            "       del.meals_delivered, del.created_at, " +
            "       c.donation_id, d.food_name, d.category, d.quantity, " +
            "       uDonor.name AS donor_name, uDonor.address AS donor_address, " +
            "       uNgo.name AS ngo_name, uNgo.address AS ngo_address, " +
            "       uVol.name AS volunteer_name, uVol.phone AS volunteer_phone " +
            "FROM Deliveries del " +
            "JOIN Claims c ON del.claim_id = c.id " +
            "JOIN Donations d ON c.donation_id = d.id " +
            "JOIN Users uDonor ON d.donor_id = uDonor.id " +
            "JOIN Users uNgo ON c.ngo_id = uNgo.id " +
            "JOIN Users uVol ON del.volunteer_id = uVol.id ";

    private static final String SQL_SELECT_BY_ID =
            SQL_SELECT_BASE + "WHERE del.id = ?";

    private static final String SQL_SELECT_BY_CLAIM_ID =
            SQL_SELECT_BASE + "WHERE del.claim_id = ?";

    private static final String SQL_SELECT_BY_DONATION_ID =
            SQL_SELECT_BASE + "WHERE c.donation_id = ?";

    private static final String SQL_SELECT_BY_VOLUNTEER =
            SQL_SELECT_BASE + "WHERE del.volunteer_id = ? ORDER BY del.created_at DESC";

    private static final String SQL_INSERT_DELIVERY =
            "INSERT INTO Deliveries (claim_id, volunteer_id, status, picked_at, delivered_at, meals_delivered, created_at) " +
            "VALUES (?, ?, 'Accepted', NULL, NULL, NULL, ?)";

    private static final String SQL_ADVANCE_TO_PICKED_UP =
            "UPDATE Deliveries SET status = 'Picked Up', picked_at = ? WHERE id = ? AND status = 'Accepted'";

    private static final String SQL_ADVANCE_TO_DELIVERED =
            "UPDATE Deliveries SET status = 'Delivered', delivered_at = ?, meals_delivered = ? WHERE id = ? AND status = 'Picked Up'";

    private static final String SQL_SYNC_CLAIM_STATUS =
            "UPDATE Claims SET status = ? WHERE id = (SELECT claim_id FROM Deliveries WHERE id = ?)";

    private static final String SQL_SYNC_DONATION_STATUS =
            "UPDATE Donations SET status = ? WHERE id = (SELECT donation_id FROM Claims WHERE id = (SELECT claim_id FROM Deliveries WHERE id = ?))";

    private static final String SQL_SYNC_DONATION_MEALS =
            "UPDATE Donations SET status = 'Delivered', quantity = ? WHERE id = (SELECT donation_id FROM Claims WHERE id = (SELECT claim_id FROM Deliveries WHERE id = ?))";

    // In-memory fallback list for offline / zero-setup execution
    private static final List<Delivery> fallbackDeliveries = new CopyOnWriteArrayList<>();

    static {
        LocalDateTime now = LocalDateTime.now();

        // 1. Delivery for Claim 2 (Hearty Minestrone Soup, Vol 7 Alex Green, Accepted)
        Delivery d1 = new Delivery(1, 2, 7, DeliveryStatus.ACCEPTED,
                null, null, null, now.minusHours(1));
        d1.setDonationId(5);
        d1.setFoodName("Hearty Minestrone Soup & Garlic Rolls");
        d1.setCategory("Veg");
        d1.setQuantity(50);
        d1.setDonorName("Green Garden Bistro");
        d1.setDonorAddress("124 Culinary Boulevard, Downtown");
        d1.setNgoName("City Harvest Rescue Alliance");
        d1.setNgoAddress("720 South Michigan Street, Seattle, WA");
        d1.setVolunteerName("Alex Green");
        d1.setVolunteerPhone("9876543216");
        fallbackDeliveries.add(d1);

        // 2. Delivery for Claim 3 (Steamed Tofu & Vegetable Stir-Fry, Vol 8 Maya Lin, Picked Up)
        Delivery d2 = new Delivery(2, 3, 8, DeliveryStatus.PICKED_UP,
                now.minusHours(1), null, null, now.minusHours(2));
        d2.setDonationId(6);
        d2.setFoodName("Steamed Tofu & Vegetable Stir-Fry");
        d2.setCategory("Veg");
        d2.setQuantity(30);
        d2.setDonorName("Elena Rostova");
        d2.setDonorAddress("312 Olive Way, Capitol Hill");
        d2.setNgoName("City Harvest Rescue Alliance");
        d2.setNgoAddress("720 South Michigan Street, Seattle, WA");
        d2.setVolunteerName("Maya Lin");
        d2.setVolunteerPhone("9876543217");
        fallbackDeliveries.add(d2);

        // 3. Delivery for Claim 4 (Fresh Ciabatta & Herb Rolls, Vol 7 Alex Green, Picked Up)
        Delivery d3 = new Delivery(3, 4, 7, DeliveryStatus.PICKED_UP,
                now.minusHours(2), null, null, now.minusHours(3));
        d3.setDonationId(7);
        d3.setFoodName("Fresh Ciabatta & Herb Rolls");
        d3.setCategory("Veg");
        d3.setQuantity(25);
        d3.setDonorName("Green Garden Bistro");
        d3.setDonorAddress("124 Culinary Boulevard, Downtown");
        d3.setNgoName("Hope Food Foundation");
        d3.setNgoAddress("450 Community Way, Suite 12, Seattle, WA");
        d3.setVolunteerName("Alex Green");
        d3.setVolunteerPhone("9876543216");
        fallbackDeliveries.add(d3);

        // 4. Delivery for Claim 5 (Assorted Gourmet Wraps & Sandwiches, Vol 9 David Chen, Delivered)
        Delivery d4 = new Delivery(4, 5, 9, DeliveryStatus.DELIVERED,
                now.minusHours(24), now.minusHours(23), 50, now.minusHours(25));
        d4.setDonationId(8);
        d4.setFoodName("Assorted Gourmet Wraps & Sandwiches");
        d4.setCategory("Non-Veg");
        d4.setQuantity(50);
        d4.setDonorName("Grand Pacific Hotel & Banquet");
        d4.setDonorAddress("850 Grand Plaza Avenue, Bellevue");
        d4.setNgoName("Compassion Meals Initiative");
        d4.setNgoAddress("108 North Aurora Lane, Shoreline, WA");
        d4.setVolunteerName("David Chen");
        d4.setVolunteerPhone("9876543218");
        fallbackDeliveries.add(d4);

        // 5. Delivery for Claim 6 (Royal Vegetable Biryani Feast, Vol 7 Alex Green, Delivered)
        Delivery d5 = new Delivery(5, 6, 7, DeliveryStatus.DELIVERED,
                now.minusHours(48), now.minusHours(47), 80, now.minusHours(49));
        d5.setDonationId(9);
        d5.setFoodName("Royal Vegetable Biryani Feast");
        d5.setCategory("Veg");
        d5.setQuantity(80);
        d5.setDonorName("Green Garden Bistro");
        d5.setDonorAddress("124 Culinary Boulevard, Downtown");
        d5.setNgoName("Hope Food Foundation");
        d5.setNgoAddress("450 Community Way, Suite 12, Seattle, WA");
        d5.setVolunteerName("Alex Green");
        d5.setVolunteerPhone("9876543216");
        fallbackDeliveries.add(d5);
    }

    /**
     * Creates a new Delivery record when a volunteer courier accepts a claim task.
     * Uses PreparedStatement strictly.
     */
    public Delivery createDelivery(int claimId, int volunteerId) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            Timestamp nowTs = Timestamp.valueOf(LocalDateTime.now());

            int generatedId = -1;
            try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_DELIVERY, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, claimId);
                pstmt.setInt(2, volunteerId);
                pstmt.setTimestamp(3, nowTs);

                pstmt.executeUpdate();
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        generatedId = keys.getInt(1);
                    }
                }
            }

            if (generatedId > 0) {
                return findById(generatedId);
            }
        } catch (SQLException e) {
            System.err.println("[DeliveryDAO] MySQL insert notice (using fallback): " + e.getMessage());
        }

        // Fallback in-memory creation
        int newId = fallbackDeliveries.size() + 1;
        Delivery fb = new Delivery(newId, claimId, volunteerId, DeliveryStatus.ACCEPTED, null, null, null, LocalDateTime.now());
        populateFallbackDetails(fb, claimId, volunteerId);
        fallbackDeliveries.add(0, fb);
        return fb;
    }

    /**
     * Finds a delivery record by its primary key.
     */
    public Delivery findById(int deliveryId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_BY_ID)) {

            pstmt.setInt(1, deliveryId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDelivery(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DeliveryDAO] MySQL findById notice: " + e.getMessage());
        }

        for (Delivery d : fallbackDeliveries) {
            if (d.getId() == deliveryId) return d;
        }
        return null;
    }

    /**
     * Finds a delivery by claim ID, creating one if not yet initialized.
     */
    public Delivery getOrCreateDeliveryForClaim(int claimId, int volunteerId) {
        Delivery existing = findByClaimId(claimId);
        if (existing != null) {
            return existing;
        }
        return createDelivery(claimId, volunteerId);
    }

    /**
     * Finds the Delivery associated with a specific Claim ID.
     */
    public Delivery findByClaimId(int claimId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_BY_CLAIM_ID)) {

            pstmt.setInt(1, claimId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDelivery(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DeliveryDAO] MySQL findByClaimId notice: " + e.getMessage());
        }

        for (Delivery d : fallbackDeliveries) {
            if (d.getClaimId() == claimId) return d;
        }
        return null;
    }

    /**
     * Finds the Delivery associated with a specific Donation ID.
     */
    public Delivery findByDonationId(int donationId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_BY_DONATION_ID)) {

            pstmt.setInt(1, donationId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDelivery(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DeliveryDAO] MySQL findByDonationId notice: " + e.getMessage());
        }

        for (Delivery d : fallbackDeliveries) {
            if (d.getDonationId() == donationId) return d;
        }
        return null;
    }

    /**
     * Retrieves all deliveries assigned to a volunteer courier.
     */
    public List<Delivery> findByVolunteerId(int volunteerId) {
        List<Delivery> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_BY_VOLUNTEER)) {

            pstmt.setInt(1, volunteerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDelivery(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            System.err.println("[DeliveryDAO] MySQL findByVolunteer notice: " + e.getMessage());
        }

        List<Delivery> fbList = new ArrayList<>();
        for (Delivery d : fallbackDeliveries) {
            if (d.getVolunteerId() == volunteerId) {
                fbList.add(d);
            }
        }
        return fbList;
    }

    /**
     * Advances delivery status one step at a time inside an atomic database transaction.
     * Synchronizes Deliveries, Claims, and Donations tables simultaneously.
     * Strictly uses PreparedStatement only.
     */
    public boolean updateStatus(int deliveryId, DeliveryStatus nextStatus, Integer mealsDelivered) {
        Connection conn = null;
        LocalDateTime now = LocalDateTime.now();
        Timestamp nowTs = Timestamp.valueOf(now);

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            if (nextStatus == DeliveryStatus.PICKED_UP) {
                // 1. Advance Deliveries: status -> 'Picked Up', picked_at = NOW()
                try (PreparedStatement pstmt1 = conn.prepareStatement(SQL_ADVANCE_TO_PICKED_UP)) {
                    pstmt1.setTimestamp(1, nowTs);
                    pstmt1.setInt(2, deliveryId);
                    int affected = pstmt1.executeUpdate();
                    if (affected == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                // 2. Synchronize Claims -> 'Picked Up'
                try (PreparedStatement pstmt2 = conn.prepareStatement(SQL_SYNC_CLAIM_STATUS)) {
                    pstmt2.setString(1, "Picked Up");
                    pstmt2.setInt(2, deliveryId);
                    pstmt2.executeUpdate();
                }

                // 3. Synchronize Donations -> 'Picked Up'
                try (PreparedStatement pstmt3 = conn.prepareStatement(SQL_SYNC_DONATION_STATUS)) {
                    pstmt3.setString(1, "Picked Up");
                    pstmt3.setInt(2, deliveryId);
                    pstmt3.executeUpdate();
                }

            } else if (nextStatus == DeliveryStatus.DELIVERED) {
                int actualMeals = (mealsDelivered != null && mealsDelivered > 0) ? mealsDelivered : 1;

                // 1. Advance Deliveries: status -> 'Delivered', delivered_at = NOW(), meals_delivered = ?
                try (PreparedStatement pstmt1 = conn.prepareStatement(SQL_ADVANCE_TO_DELIVERED)) {
                    pstmt1.setTimestamp(1, nowTs);
                    pstmt1.setInt(2, actualMeals);
                    pstmt1.setInt(3, deliveryId);
                    int affected = pstmt1.executeUpdate();
                    if (affected == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                // 2. Synchronize Claims -> 'Delivered'
                try (PreparedStatement pstmt2 = conn.prepareStatement(SQL_SYNC_CLAIM_STATUS)) {
                    pstmt2.setString(1, "Delivered");
                    pstmt2.setInt(2, deliveryId);
                    pstmt2.executeUpdate();
                }

                // 3. Synchronize Donations -> 'Delivered' & update final quantity
                try (PreparedStatement pstmt3 = conn.prepareStatement(SQL_SYNC_DONATION_MEALS)) {
                    pstmt3.setInt(1, actualMeals);
                    pstmt3.setInt(2, deliveryId);
                    pstmt3.executeUpdate();
                }
            }

            conn.commit(); // Transaction successful
            updateFallbackStatus(deliveryId, nextStatus, mealsDelivered, now);
            return true;

        } catch (SQLException e) {
            System.err.println("[DeliveryDAO] MySQL updateStatus notice (using fallback): " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* log */ }
            }
            return updateFallbackStatus(deliveryId, nextStatus, mealsDelivered, now);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) { /* log */ }
            }
        }
    }

    private boolean updateFallbackStatus(int deliveryId, DeliveryStatus nextStatus, Integer mealsDelivered, LocalDateTime now) {
        for (Delivery d : fallbackDeliveries) {
            if (d.getId() == deliveryId) {
                if (nextStatus == DeliveryStatus.PICKED_UP && d.getStatus() == DeliveryStatus.ACCEPTED) {
                    d.setStatus(DeliveryStatus.PICKED_UP);
                    d.setPickedAt(now);
                    FoodDonation don = DonationDAO.findFallbackById(d.getDonationId());
                    if (don != null) don.setStatus(DonationStatus.PICKED_UP);
                    return true;
                } else if (nextStatus == DeliveryStatus.DELIVERED && d.getStatus() == DeliveryStatus.PICKED_UP) {
                    d.setStatus(DeliveryStatus.DELIVERED);
                    d.setDeliveredAt(now);
                    int finalMeals = (mealsDelivered != null) ? mealsDelivered : d.getQuantity();
                    d.setMealsDelivered(finalMeals);
                    FoodDonation don = DonationDAO.findFallbackById(d.getDonationId());
                    if (don != null) {
                        don.setStatus(DonationStatus.DELIVERED);
                        don.setQuantity(finalMeals);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void populateFallbackDetails(Delivery d, int claimId, int volunteerId) {
        Claim claim = ClaimDAO.findFallbackById(claimId);
        if (claim != null) {
            d.setDonationId(claim.getDonationId());
            d.setFoodName(claim.getFoodName());
            d.setCategory(claim.getCategory());
            d.setQuantity(claim.getQuantity());
            d.setDonorName(claim.getDonorName());
            d.setDonorAddress(claim.getPickupAddress());
            d.setNgoName(claim.getNgoName());
            d.setNgoAddress(claim.getDropoffAddress());
            d.setVolunteerName((claim.getVolunteerName() != null) ? claim.getVolunteerName() : "Alex Green");
            d.setVolunteerPhone("9876543212");
        } else {
            // Default enriched details for demonstration
            d.setDonationId(3);
            d.setFoodName("Grilled Herb Chicken & Rice Bowls");
            d.setCategory("Non-Veg");
            d.setQuantity(40);
            d.setDonorName("Green Garden Bistro");
            d.setDonorAddress("124 Culinary Boulevard, Downtown");
            d.setNgoName("Hope Food Foundation");
            d.setNgoAddress("450 Community Way, Suite 12, Seattle, WA");
            d.setVolunteerName("Alex Green");
            d.setVolunteerPhone("9876543212");
        }
    }

    private Delivery mapResultSetToDelivery(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int claimId = rs.getInt("claim_id");
        int volunteerId = rs.getInt("volunteer_id");
        DeliveryStatus status = DeliveryStatus.fromString(rs.getString("status"));

        Timestamp pickedTs = rs.getTimestamp("picked_at");
        LocalDateTime pickedAt = (pickedTs != null) ? pickedTs.toLocalDateTime() : null;

        Timestamp delivTs = rs.getTimestamp("delivered_at");
        LocalDateTime deliveredAt = (delivTs != null) ? delivTs.toLocalDateTime() : null;

        int meals = rs.getInt("meals_delivered");
        Integer mealsDelivered = rs.wasNull() ? null : meals;

        Timestamp createdTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = (createdTs != null) ? createdTs.toLocalDateTime() : LocalDateTime.now();

        Delivery delivery = new Delivery(id, claimId, volunteerId, status, pickedAt, deliveredAt, mealsDelivered, createdAt);
        delivery.setDonationId(rs.getInt("donation_id"));
        delivery.setFoodName(rs.getString("food_name"));
        delivery.setCategory(rs.getString("category"));
        delivery.setQuantity(rs.getInt("quantity"));
        delivery.setDonorName(rs.getString("donor_name"));
        delivery.setDonorAddress(rs.getString("donor_address"));
        delivery.setNgoName(rs.getString("ngo_name"));
        delivery.setNgoAddress(rs.getString("ngo_address"));
        delivery.setVolunteerName(rs.getString("volunteer_name"));
        delivery.setVolunteerPhone(rs.getString("volunteer_phone"));

        return delivery;
    }
}
