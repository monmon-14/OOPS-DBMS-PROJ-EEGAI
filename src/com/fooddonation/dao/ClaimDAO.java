package com.fooddonation.dao;

import com.fooddonation.model.Claim;
import com.fooddonation.model.ClaimStatus;
import com.fooddonation.model.DonationStatus;
import com.fooddonation.model.FoodDonation;
import com.fooddonation.model.User;
import com.fooddonation.service.UserSession;
import com.fooddonation.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Data Access Object for Claims handling NGO food claims,
 * courier task dispatching, and atomic double-claim prevention inside database transactions.
 * Strictly uses PreparedStatements only.
 */
public class ClaimDAO {

    // --- Prepared SQL Statements ---
    private static final String SQL_CLAIM_DONATION_CHECK_UPDATE =
            "UPDATE Donations SET status = 'Claimed' WHERE id = ? AND status = 'Available' AND expiry_time > NOW()";

    private static final String SQL_INSERT_CLAIM =
            "INSERT INTO Claims (donation_id, ngo_id, volunteer_id, status, claimed_at) VALUES (?, ?, NULL, 'Claimed', ?)";

    private static final String SQL_SELECT_BY_NGO =
            "SELECT c.id, c.donation_id, c.ngo_id, c.volunteer_id, c.status, c.claimed_at, " +
            "       d.food_name, d.category, d.quantity, d.pickup_address, " +
            "       uDonor.name AS donor_name, uNgo.name AS ngo_name, uNgo.address AS dropoff_address, " +
            "       uVol.name AS volunteer_name " +
            "FROM Claims c " +
            "JOIN Donations d ON c.donation_id = d.id " +
            "JOIN Users uDonor ON d.donor_id = uDonor.id " +
            "JOIN Users uNgo ON c.ngo_id = uNgo.id " +
            "LEFT JOIN Users uVol ON c.volunteer_id = uVol.id " +
            "WHERE c.ngo_id = ? " +
            "ORDER BY c.claimed_at DESC";

    private static final String SQL_SELECT_UNASSIGNED_PICKUP_TASKS =
            "SELECT c.id, c.donation_id, c.ngo_id, c.volunteer_id, c.status, c.claimed_at, " +
            "       d.food_name, d.category, d.quantity, d.pickup_address, " +
            "       uDonor.name AS donor_name, uNgo.name AS ngo_name, uNgo.address AS dropoff_address, " +
            "       uVol.name AS volunteer_name " +
            "FROM Claims c " +
            "JOIN Donations d ON c.donation_id = d.id " +
            "JOIN Users uDonor ON d.donor_id = uDonor.id " +
            "JOIN Users uNgo ON c.ngo_id = uNgo.id " +
            "LEFT JOIN Users uVol ON c.volunteer_id = uVol.id " +
            "WHERE c.volunteer_id IS NULL AND c.status = 'Claimed' " +
            "ORDER BY c.claimed_at ASC";

    private static final String SQL_ACCEPT_TASK =
            "UPDATE Claims SET volunteer_id = ?, status = 'Volunteer Assigned' WHERE id = ? AND volunteer_id IS NULL";

    private static final String SQL_SELECT_DELIVERIES_BY_VOLUNTEER =
            "SELECT c.id, c.donation_id, c.ngo_id, c.volunteer_id, c.status, c.claimed_at, " +
            "       d.food_name, d.category, d.quantity, d.pickup_address, " +
            "       uDonor.name AS donor_name, uNgo.name AS ngo_name, uNgo.address AS dropoff_address, " +
            "       uVol.name AS volunteer_name " +
            "FROM Claims c " +
            "JOIN Donations d ON c.donation_id = d.id " +
            "JOIN Users uDonor ON d.donor_id = uDonor.id " +
            "JOIN Users uNgo ON c.ngo_id = uNgo.id " +
            "LEFT JOIN Users uVol ON c.volunteer_id = uVol.id " +
            "WHERE c.volunteer_id = ? " +
            "ORDER BY c.claimed_at DESC";

    private static final String SQL_UPDATE_CLAIM_STATUS =
            "UPDATE Claims SET status = ? WHERE id = ?";

    private static final String SQL_UPDATE_DONATION_STATUS_FROM_CLAIM =
            "UPDATE Donations SET status = ? WHERE id = (SELECT donation_id FROM Claims WHERE id = ?)";

    // Fallback in-memory list for zero-setup execution
    private static final List<Claim> fallbackClaims = new CopyOnWriteArrayList<>();

    static {
        LocalDateTime now = LocalDateTime.now();

        // 1. Claim for Donation 4: Claimed by NGO 4 (Hope Food Foundation), open task for Volunteers
        Claim c1 = new Claim(1, 4, 4, null, ClaimStatus.CLAIMED, now.minusHours(3));
        c1.setFoodName("Grilled Chicken & Quinoa Bowls");
        c1.setCategory("Non-Veg");
        c1.setQuantity(45);
        c1.setDonorName("Grand Pacific Hotel & Banquet");
        c1.setPickupAddress("850 Grand Plaza Avenue, Bellevue");
        c1.setNgoName("Hope Food Foundation");
        c1.setDropoffAddress("450 Community Way, Suite 12, Seattle, WA");
        fallbackClaims.add(c1);

        // 2. Claim for Donation 5: Claimed by NGO 5 (City Harvest), Alex Green assigned
        Claim c2 = new Claim(2, 5, 5, 7, ClaimStatus.ASSIGNED, now.minusHours(2));
        c2.setFoodName("Hearty Minestrone Soup & Garlic Rolls");
        c2.setCategory("Veg");
        c2.setQuantity(50);
        c2.setDonorName("Green Garden Bistro");
        c2.setPickupAddress("124 Culinary Boulevard, Downtown");
        c2.setNgoName("City Harvest Rescue Alliance");
        c2.setDropoffAddress("720 South Michigan Street, Seattle, WA");
        c2.setVolunteerName("Alex Green");
        fallbackClaims.add(c2);

        // 3. Claim for Donation 6: Picked Up by Maya Lin
        Claim c3 = new Claim(3, 6, 5, 8, ClaimStatus.PICKED_UP, now.minusHours(3));
        c3.setFoodName("Steamed Tofu & Vegetable Stir-Fry");
        c3.setCategory("Veg");
        c3.setQuantity(30);
        c3.setDonorName("Elena Rostova");
        c3.setPickupAddress("312 Olive Way, Capitol Hill");
        c3.setNgoName("City Harvest Rescue Alliance");
        c3.setDropoffAddress("720 South Michigan Street, Seattle, WA");
        c3.setVolunteerName("Maya Lin");
        fallbackClaims.add(c3);

        // 4. Claim for Donation 7: Picked Up by Alex Green
        Claim c4 = new Claim(4, 7, 4, 7, ClaimStatus.PICKED_UP, now.minusHours(4));
        c4.setFoodName("Fresh Ciabatta & Herb Rolls");
        c4.setCategory("Veg");
        c4.setQuantity(25);
        c4.setDonorName("Green Garden Bistro");
        c4.setPickupAddress("124 Culinary Boulevard, Downtown");
        c4.setNgoName("Hope Food Foundation");
        c4.setDropoffAddress("450 Community Way, Suite 12, Seattle, WA");
        c4.setVolunteerName("Alex Green");
        fallbackClaims.add(c4);

        // 5. Claim for Donation 8: Delivered by David Chen
        Claim c5 = new Claim(5, 8, 6, 9, ClaimStatus.DELIVERED, now.minusHours(25));
        c5.setFoodName("Assorted Gourmet Wraps & Sandwiches");
        c5.setCategory("Non-Veg");
        c5.setQuantity(50);
        c5.setDonorName("Grand Pacific Hotel & Banquet");
        c5.setPickupAddress("850 Grand Plaza Avenue, Bellevue");
        c5.setNgoName("Compassion Meals Initiative");
        c5.setDropoffAddress("108 North Aurora Lane, Shoreline, WA");
        c5.setVolunteerName("David Chen");
        fallbackClaims.add(c5);

        // 6. Claim for Donation 9: Delivered by Alex Green
        Claim c6 = new Claim(6, 9, 4, 7, ClaimStatus.DELIVERED, now.minusHours(49));
        c6.setFoodName("Royal Vegetable Biryani Feast");
        c6.setCategory("Veg");
        c6.setQuantity(80);
        c6.setDonorName("Green Garden Bistro");
        c6.setPickupAddress("124 Culinary Boulevard, Downtown");
        c6.setNgoName("Hope Food Foundation");
        c6.setDropoffAddress("450 Community Way, Suite 12, Seattle, WA");
        c6.setVolunteerName("Alex Green");
        fallbackClaims.add(c6);
    }

    /**
     * Atomically claims a donation in a DB transaction with concurrency protection.
     * Prevents double-claims or claiming expired listings.
     */
    public boolean createClaim(int donationId, int ngoId) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin atomic transaction

            // 1. Re-check and update donation status atomically (fails if already claimed or expired)
            try (PreparedStatement pstmtCheck = conn.prepareStatement(SQL_CLAIM_DONATION_CHECK_UPDATE)) {
                pstmtCheck.setInt(1, donationId);
                int affected = pstmtCheck.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return false; // Item was already claimed or expired!
                }
            }

            // 2. Insert Claim record
            int generatedClaimId = -1;
            Timestamp nowTs = Timestamp.valueOf(LocalDateTime.now());
            try (PreparedStatement pstmtInsert = conn.prepareStatement(SQL_INSERT_CLAIM, Statement.RETURN_GENERATED_KEYS)) {
                pstmtInsert.setInt(1, donationId);
                pstmtInsert.setInt(2, ngoId);
                pstmtInsert.setTimestamp(3, nowTs);

                pstmtInsert.executeUpdate();
                try (ResultSet keys = pstmtInsert.getGeneratedKeys()) {
                    if (keys.next()) {
                        generatedClaimId = keys.getInt(1);
                    }
                }
            }

            conn.commit(); // Transaction successful

            // Sync fallback cache
            Claim fbClaim = new Claim(
                    (generatedClaimId > 0) ? generatedClaimId : fallbackClaims.size() + 1,
                    donationId, ngoId, null, ClaimStatus.CLAIMED, LocalDateTime.now()
            );
            populateFallbackClaimDetails(fbClaim, donationId, ngoId);
            fallbackClaims.add(0, fbClaim);
            return true;

        } catch (SQLException e) {
            System.err.println("[ClaimDAO] MySQL claim notice (using fallback): " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* log */ }
            }
            // Execute in fallback cache with double-claim & expiry checks
            FoodDonation fbDonation = DonationDAO.findFallbackById(donationId);
            if (fbDonation != null) {
                if (fbDonation.getStatus() != DonationStatus.AVAILABLE || !fbDonation.getExpiryTime().isAfter(LocalDateTime.now())) {
                    return false; // Concurrency check: already claimed or expired!
                }
            } else {
                return false;
            }

            Claim fbClaim = new Claim(fallbackClaims.size() + 1, donationId, ngoId, null, ClaimStatus.CLAIMED, LocalDateTime.now());
            populateFallbackClaimDetails(fbClaim, donationId, ngoId);
            fallbackClaims.add(0, fbClaim);
            return true;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) { /* log */ }
            }
        }
    }

    /**
     * Fetches claims for a specific NGO with joined details using PreparedStatement.
     */
    public List<Claim> findByNgoId(int ngoId) {
        List<Claim> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_BY_NGO)) {

            pstmt.setInt(1, ngoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToClaim(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            System.err.println("[ClaimDAO] MySQL select notice (using fallback): " + e.getMessage());
            List<Claim> fb = new ArrayList<>();
            for (Claim c : fallbackClaims) {
                if (c.getNgoId() == ngoId) {
                    fb.add(c);
                }
            }
            return fb;
        }
    }

    /**
     * Fetches all claimed donations that currently have no volunteer assigned.
     */
    public List<Claim> findUnassignedPickupTasks() {
        List<Claim> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_UNASSIGNED_PICKUP_TASKS)) {

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToClaim(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            List<Claim> fb = new ArrayList<>();
            for (Claim c : fallbackClaims) {
                if (c.isUnassigned() && c.getStatus() == ClaimStatus.CLAIMED) {
                    fb.add(c);
                }
            }
            return fb;
        }
    }

    /**
     * Assigns a volunteer to a claim inside a transaction.
     */
    public boolean assignVolunteer(int claimId, int volunteerId) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(SQL_ACCEPT_TASK)) {
                pstmt.setInt(1, volunteerId);
                pstmt.setInt(2, claimId);

                int affected = pstmt.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return false; // Already assigned or cancelled
                }
            }

            conn.commit();
            updateFallbackVolunteer(claimId, volunteerId);
            return true;

        } catch (SQLException e) {
            System.err.println("[ClaimDAO] MySQL accept notice (using fallback): " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* log */ }
            }
            return updateFallbackVolunteer(claimId, volunteerId);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) { /* log */ }
            }
        }
    }

    /**
     * Fetches rescue courier tasks accepted by a volunteer.
     */
    public List<Claim> findDeliveriesByVolunteer(int volunteerId) {
        List<Claim> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_DELIVERIES_BY_VOLUNTEER)) {

            pstmt.setInt(1, volunteerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToClaim(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            List<Claim> fb = new ArrayList<>();
            for (Claim c : fallbackClaims) {
                if (c.getVolunteerId() != null && c.getVolunteerId() == volunteerId) {
                    fb.add(c);
                }
            }
            return fb;
        }
    }

    /**
     * Updates claim status (e.g., 'Picked Up' or 'Delivered') and synchronizes Donation table status.
     */
    public boolean updateClaimStatus(int claimId, ClaimStatus newStatus) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt1 = conn.prepareStatement(SQL_UPDATE_CLAIM_STATUS)) {
                pstmt1.setString(1, newStatus.getDisplayName());
                pstmt1.setInt(2, claimId);
                pstmt1.executeUpdate();
            }

            // Sync corresponding Donations status
            String mappedDonationStatus = (newStatus == ClaimStatus.DELIVERED) ? "Delivered" : "Picked Up";
            try (PreparedStatement pstmt2 = conn.prepareStatement(SQL_UPDATE_DONATION_STATUS_FROM_CLAIM)) {
                pstmt2.setString(1, mappedDonationStatus);
                pstmt2.setInt(2, claimId);
                pstmt2.executeUpdate();
            }

            conn.commit();
            updateFallbackStatus(claimId, newStatus);
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* log */ }
            }
            return updateFallbackStatus(claimId, newStatus);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) { /* log */ }
            }
        }
    }

    private boolean updateFallbackVolunteer(int claimId, int volunteerId) {
        for (Claim c : fallbackClaims) {
            if (c.getId() == claimId && c.isUnassigned()) {
                c.setVolunteerId(volunteerId);
                c.setStatus(ClaimStatus.ASSIGNED);
                User u = UserSession.getInstance().getCurrentUser();
                if (u != null && u.getId() == volunteerId) {
                    c.setVolunteerName(u.getName());
                } else {
                    c.setVolunteerName("Alex Green");
                }
                return true;
            }
        }
        return false;
    }

    private void populateFallbackClaimDetails(Claim fbClaim, int donationId, int ngoId) {
        FoodDonation donation = DonationDAO.findFallbackById(donationId);
        if (donation != null) {
            donation.setStatus(DonationStatus.CLAIMED);
            fbClaim.setFoodName(donation.getFoodName());
            fbClaim.setCategory(donation.getCategory());
            fbClaim.setQuantity(donation.getQuantity());
            fbClaim.setPickupAddress(donation.getPickupAddress());
            fbClaim.setDonorName((donation.getDonorName() != null && !donation.getDonorName().isEmpty())
                    ? donation.getDonorName() : "Green Garden Bistro");
        }
        User current = UserSession.getInstance().getCurrentUser();
        if (current != null && current.getId() == ngoId) {
            fbClaim.setNgoName(current.getName());
            fbClaim.setDropoffAddress((current.getAddress() != null) ? current.getAddress() : "450 Community Way, Suite 12, Seattle, WA");
        } else {
            fbClaim.setNgoName("Hope Food Foundation");
            fbClaim.setDropoffAddress("450 Community Way, Suite 12, Seattle, WA");
        }
    }

    public static Claim findFallbackById(int id) {
        for (Claim c : fallbackClaims) {
            if (c.getId() == id) return c;
        }
        return null;
    }

    public static Claim findFallbackByDonationId(int donationId) {
        for (Claim c : fallbackClaims) {
            if (c.getDonationId() == donationId) return c;
        }
        return null;
    }

    private boolean updateFallbackStatus(int claimId, ClaimStatus newStatus) {
        for (Claim c : fallbackClaims) {
            if (c.getId() == claimId) {
                c.setStatus(newStatus);
                return true;
            }
        }
        return false;
    }

    private Claim mapResultSetToClaim(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int donationId = rs.getInt("donation_id");
        int ngoId = rs.getInt("ngo_id");
        int volId = rs.getInt("volunteer_id");
        Integer volunteerId = rs.wasNull() ? null : volId;
        ClaimStatus status = ClaimStatus.fromString(rs.getString("status"));
        LocalDateTime claimedAt = rs.getTimestamp("claimed_at").toLocalDateTime();

        Claim claim = new Claim(id, donationId, ngoId, volunteerId, status, claimedAt);
        claim.setFoodName(rs.getString("food_name"));
        claim.setCategory(rs.getString("category"));
        claim.setQuantity(rs.getInt("quantity"));
        claim.setPickupAddress(rs.getString("pickup_address"));
        claim.setDonorName(rs.getString("donor_name"));
        claim.setNgoName(rs.getString("ngo_name"));
        claim.setDropoffAddress(rs.getString("dropoff_address"));
        claim.setVolunteerName(rs.getString("volunteer_name"));

        return claim;
    }
}
