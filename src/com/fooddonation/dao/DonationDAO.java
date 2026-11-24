package com.fooddonation.dao;

import com.fooddonation.model.DonationStatus;
import com.fooddonation.model.FoodDonation;
import com.fooddonation.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Data Access Object (DAO) for FoodDonation entities.
 * Strictly uses PreparedStatements for all database queries and updates.
 */
public class DonationDAO {

    private static final String SQL_INSERT_DONATION =
            "INSERT INTO Donations (donor_id, food_name, category, quantity, prep_time, expiry_time, pickup_address, status, notes, created_at, occasion, is_demo) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BY_DONOR =
            "SELECT id, donor_id, food_name, category, quantity, prep_time, expiry_time, pickup_address, status, notes, created_at, occasion, is_demo " +
            "FROM Donations WHERE donor_id = ? ORDER BY created_at DESC";

    private static final String SQL_SELECT_RECENT_BY_DONOR =
            "SELECT id, donor_id, food_name, category, quantity, prep_time, expiry_time, pickup_address, status, notes, created_at, occasion, is_demo " +
            "FROM Donations WHERE donor_id = ? ORDER BY created_at DESC LIMIT ?";

    private static final String SQL_CANCEL_DONATION =
            "UPDATE Donations SET status = 'Cancelled' WHERE id = ? AND donor_id = ? AND status = 'Available'";

    private static final String SQL_EXPIRE_OLD_DONATIONS =
            "UPDATE Donations SET status = 'Expired' WHERE status = 'Available' AND expiry_time <= NOW()";

    private static final String SQL_DONOR_STATS =
            "SELECT COUNT(*) AS total_listed, " +
            "       COUNT(CASE WHEN status = 'Claimed' THEN 1 END) AS total_claimed, " +
            "       COUNT(CASE WHEN status = 'Delivered' THEN 1 END) AS total_delivered, " +
            "       COALESCE(SUM(CASE WHEN status = 'Delivered' THEN quantity ELSE 0 END), 0) AS meals_donated " +
            "FROM Donations WHERE donor_id = ?";

    // In-memory fallback list for offline or zero-configuration operation
    private static final List<FoodDonation> fallbackDonations = new CopyOnWriteArrayList<>();

    static {
        // Seed default sample donations for demo Donor (id = 1, Green Garden Bistro)
        LocalDateTime now = LocalDateTime.now();

        // 10 Sample Donations in mixed statuses
        fallbackDonations.add(new FoodDonation(1, 1, "Fresh Artisan Bread & Baguettes", "Veg", 35,
                now.minusHours(2), now.plusHours(6), "124 Culinary Boulevard, Downtown",
                DonationStatus.AVAILABLE, "Freshly baked morning sourdough loaves and french baguettes in kraft bags.", now.minusHours(2), null, false));

        fallbackDonations.add(new FoodDonation(2, 3, "Organic Vegetable Medley & Garden Salad", "Veg", 40,
                now.minusHours(1), now.plusHours(5), "312 Olive Way, Capitol Hill",
                DonationStatus.AVAILABLE, "Fresh heirloom greens, roasted carrots, and vinaigrette dressings in catering tubs.", now.minusHours(1), null, false));

        fallbackDonations.add(new FoodDonation(3, 2, "Steamed Basmati Rice & Dal Tadka", "Veg", 60,
                now.minusHours(3), now.plusHours(4), "850 Grand Plaza Avenue, Bellevue",
                DonationStatus.AVAILABLE, "Banquet surplus. Kept warm in sealed stainless food containers. High nutrition.", now.minusHours(3), null, false));

        fallbackDonations.add(new FoodDonation(4, 2, "Grilled Chicken & Quinoa Bowls", "Non-Veg", 45,
                now.minusHours(4), now.plusHours(3), "850 Grand Plaza Avenue, Bellevue",
                DonationStatus.CLAIMED, "Claimed by Hope Food Foundation. Ready at loading dock B.", now.minusHours(4), null, false));

        fallbackDonations.add(new FoodDonation(5, 1, "Hearty Minestrone Soup & Garlic Rolls", "Veg", 50,
                now.minusHours(3), now.plusHours(3), "124 Culinary Boulevard, Downtown",
                DonationStatus.CLAIMED, "Claimed by City Harvest Alliance. Alex Green assigned for delivery transport.", now.minusHours(3), null, false));

        fallbackDonations.add(new FoodDonation(6, 3, "Steamed Tofu & Vegetable Stir-Fry", "Veg", 30,
                now.minusHours(4), now.plusHours(2), "312 Olive Way, Capitol Hill",
                DonationStatus.PICKED_UP, "Picked up by courier Maya Lin via cargo bicycle. En route to shelter.", now.minusHours(4), null, false));

        fallbackDonations.add(new FoodDonation(7, 1, "Fresh Ciabatta & Herb Rolls", "Veg", 25,
                now.minusHours(5), now.plusHours(2), "124 Culinary Boulevard, Downtown",
                DonationStatus.PICKED_UP, "Picked up from kitchen dispatch counter. Courier in transit.", now.minusHours(5), null, false));

        fallbackDonations.add(new FoodDonation(8, 2, "Assorted Gourmet Wraps & Sandwiches", "Non-Veg", 50,
                now.minusDays(1), now.minusHours(18), "850 Grand Plaza Avenue, Bellevue",
                DonationStatus.DELIVERED, "Delivered and safely distributed to North Shoreline Youth Shelter.", now.minusHours(26), null, false));

        fallbackDonations.add(new FoodDonation(9, 1, "Royal Vegetable Biryani Feast", "Veg", 80,
                now.minusDays(2), now.minusDays(1), "124 Culinary Boulevard, Downtown",
                DonationStatus.DELIVERED, "Successfully served 80 warm nutritious meals at Hope Community Shelter.", now.minusHours(50), null, false));

        fallbackDonations.add(new FoodDonation(10, 2, "Seasonal Fresh Fruit Parfaits & Yogurt", "Veg", 30,
                now.minusDays(3), now.minusDays(2), "850 Grand Plaza Avenue, Bellevue",
                DonationStatus.EXPIRED, "Exceeded safe consumption window. Automatically flagged and expired.", now.minusDays(3), null, false));

        // 10 Tamil Heritage Food Listings in English (Fictional Demo Donors, is_demo = TRUE)
        FoodDonation td1 = new FoodDonation(11, 19, "Sweet & Ven Pongal with Ghee", "Veg", 75,
                now.minusHours(2), now.plusHours(6), "78 GST Road, Tambaram, Chennai, TN",
                DonationStatus.AVAILABLE, "Traditional Pongal festival feast prepared with ghee, cashews, and jaggery.", now.minusHours(2), "Pongal Festival", true);
        td1.setDonorName("Pongal Mandapam");

        FoodDonation td2 = new FoodDonation(12, 17, "Steamed Rice Idlis with Sambar", "Veg", 60,
                now.minusHours(1), now.plusHours(5), "45 Temple Car Street, Madurai, TN",
                DonationStatus.AVAILABLE, "Hot steamed soft idlis with fresh coconut chutney and drumstick sambar.", now.minusHours(1), "Regular", true);
        td2.setDonorName("Annam Mess");

        FoodDonation td3 = new FoodDonation(13, 22, "Crispy Roast Dosas with Chutney", "Veg", 40,
                now.minusHours(2), now.plusHours(4), "104 West Boulevard Road, Thanjavur, TN",
                DonationStatus.AVAILABLE, "Crispy roast dosas packed securely in banana leaves and thermal packs.", now.minusHours(2), "Regular", true);
        td3.setDonorName("Senthamizh Mess");

        FoodDonation td4 = new FoodDonation(14, 21, "Sambar Sadham with Ghee", "Veg", 35,
                now.minusHours(2), now.plusHours(5), "23 Kambar Street, Mylapore, Chennai, TN",
                DonationStatus.AVAILABLE, "Karthigai Deepam prasadam cooked with mixed native vegetables and pure ghee.", now.minusHours(2), "Karthigai Deepam", true);
        td4.setDonorName("Amudham Restaurant");

        FoodDonation td5 = new FoodDonation(15, 17, "Cool Curd Rice with Tadka", "Veg", 50,
                now.minusHours(3), now.plusHours(6), "45 Temple Car Street, Madurai, TN",
                DonationStatus.AVAILABLE, "Creamy temple-style curd rice tempered with mustard, green chilies, and ginger.", now.minusHours(3), "Regular", true);
        td5.setDonorName("Annam Mess");

        FoodDonation td6 = new FoodDonation(16, 18, "Tangy Lemon Rice with Peanuts", "Veg", 45,
                now.minusHours(1), now.plusHours(7), "12 Cross Cut Road, Gandhipuram, Coimbatore, TN",
                DonationStatus.AVAILABLE, "Deepavali festive variety rice with roasted peanuts and curry leaves.", now.minusHours(1), "Deepavali", true);
        td6.setDonorName("Thendral Catering");

        FoodDonation td7 = new FoodDonation(17, 20, "Temple Puliyotharai Prasadam", "Veg", 100,
                now.minusHours(4), now.plusHours(8), "5 Sannidhi Street, Srirangam, Tiruchirappalli, TN",
                DonationStatus.AVAILABLE, "Authentic temple festival annadhanam puliyodharai prepared with sesame oil.", now.minusHours(4), "Temple Festival", true);
        td7.setDonorName("Kaveri Annadhanam Trust");

        FoodDonation td8 = new FoodDonation(18, 19, "Seeraga Samba Feast Biryani", "Non-Veg", 90,
                now.minusHours(3), now.plusHours(4), "78 GST Road, Tambaram, Chennai, TN",
                DonationStatus.AVAILABLE, "Traditional wedding feast aromatic seeraga samba biryani with onion raita.", now.minusHours(3), "Wedding Feast", true);
        td8.setDonorName("Pongal Mandapam");

        FoodDonation td9 = new FoodDonation(19, 22, "Traditional Full Meals on Banana Leaf", "Veg", 65,
                now.minusHours(2), now.plusHours(5), "104 West Boulevard Road, Thanjavur, TN",
                DonationStatus.AVAILABLE, "Full traditional meals: ponni rice, sambar, rasam, kootu, poriyal, payasam.", now.minusHours(2), "Regular", true);
        td9.setDonorName("Senthamizh Mess");

        FoodDonation td10 = new FoodDonation(20, 18, "Whole Wheat Chapatis & Kurma", "Veg", 50,
                now.minusHours(1), now.plusHours(6), "12 Cross Cut Road, Gandhipuram, Coimbatore, TN",
                DonationStatus.AVAILABLE, "Soft whole-wheat tawa chapatis with vegetable Nilgiri kurma.", now.minusHours(1), "Regular", true);
        td10.setDonorName("Thendral Catering");

        fallbackDonations.add(td1);
        fallbackDonations.add(td2);
        fallbackDonations.add(td3);
        fallbackDonations.add(td4);
        fallbackDonations.add(td5);
        fallbackDonations.add(td6);
        fallbackDonations.add(td7);
        fallbackDonations.add(td8);
        fallbackDonations.add(td9);
        fallbackDonations.add(td10);
    }

    /**
     * Inserts a new FoodDonation listing using PreparedStatement.
     */
    public boolean insertDonation(FoodDonation donation) {
        if (donation == null) return false;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_DONATION, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, donation.getDonorId());
            pstmt.setString(2, donation.getFoodName());
            pstmt.setString(3, donation.getCategory());
            pstmt.setInt(4, donation.getQuantity());
            pstmt.setTimestamp(5, Timestamp.valueOf(donation.getPrepTime()));
            pstmt.setTimestamp(6, Timestamp.valueOf(donation.getExpiryTime()));
            pstmt.setString(7, donation.getPickupAddress());
            pstmt.setString(8, donation.getStatus().getDisplayName());
            pstmt.setString(9, donation.getNotes());
            pstmt.setTimestamp(10, Timestamp.valueOf(donation.getCreatedAt()));
            pstmt.setString(11, donation.getOccasion());
            pstmt.setBoolean(12, donation.isDemo());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        donation.setId(keys.getInt(1));
                    }
                }
                fallbackDonations.add(0, donation);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[DonationDAO] MySQL insert notice (using fallback): " + e.getMessage());
            donation.setId(fallbackDonations.size() + 1);
            fallbackDonations.add(0, donation);
            return true;
        }

        return false;
    }

    /**
     * Fetches all food donations listed by a specific donor using PreparedStatement.
     */
    public List<FoodDonation> findByDonorId(int donorId) {
        List<FoodDonation> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_BY_DONOR)) {

            pstmt.setInt(1, donorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDonation(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            System.err.println("[DonationDAO] MySQL select notice (using fallback): " + e.getMessage());
            List<FoodDonation> fb = new ArrayList<>();
            for (FoodDonation d : fallbackDonations) {
                if (d.getDonorId() == donorId) {
                    fb.add(d);
                }
            }
            return fb;
        }
    }

    /**
     * Fetches recent food donations for a donor up to limit rows using PreparedStatement.
     */
    public List<FoodDonation> findRecentByDonorId(int donorId, int limit) {
        List<FoodDonation> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_RECENT_BY_DONOR)) {

            pstmt.setInt(1, donorId);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDonation(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            List<FoodDonation> fb = new ArrayList<>();
            int count = 0;
            for (FoodDonation d : fallbackDonations) {
                if (d.getDonorId() == donorId) {
                    fb.add(d);
                    count++;
                    if (count >= limit) break;
                }
            }
            return fb;
        }
    }

    /**
     * Cancels an available donation listing using PreparedStatement.
     * Only permitted if status is currently 'Available'.
     */
    public boolean cancelDonation(int donationId, int donorId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_CANCEL_DONATION)) {

            pstmt.setInt(1, donationId);
            pstmt.setInt(2, donorId);

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                updateFallbackStatus(donationId, DonationStatus.CANCELLED);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[DonationDAO] MySQL cancel notice (using fallback): " + e.getMessage());
            return updateFallbackStatus(donationId, DonationStatus.CANCELLED);
        }

        // Check fallback if affected was 0 or connection failed
        return updateFallbackStatus(donationId, DonationStatus.CANCELLED);
    }

    /**
     * Auto-marks listings whose expiry_time has passed as 'Expired'.
     */
    public int expireOldDonations() {
        int count = 0;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_EXPIRE_OLD_DONATIONS)) {

            count = pstmt.executeUpdate();
        } catch (SQLException e) {
            // Expire in fallback
            LocalDateTime now = LocalDateTime.now();
            for (FoodDonation d : fallbackDonations) {
                if (d.getStatus() == DonationStatus.AVAILABLE && d.getExpiryTime().isBefore(now)) {
                    d.setStatus(DonationStatus.EXPIRED);
                    count++;
                }
            }
            return count;
        }

        // Also sync fallback cache
        LocalDateTime now = LocalDateTime.now();
        for (FoodDonation d : fallbackDonations) {
            if (d.getStatus() == DonationStatus.AVAILABLE && d.getExpiryTime().isBefore(now)) {
                d.setStatus(DonationStatus.EXPIRED);
            }
        }

        return count;
    }

    /**
     * Aggregates real-time donor statistics using PreparedStatement.
     */
    public DonorStats getDonorStats(int donorId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DONOR_STATS)) {

            pstmt.setInt(1, donorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new DonorStats(
                            rs.getInt("total_listed"),
                            rs.getInt("total_claimed"),
                            rs.getInt("total_delivered"),
                            rs.getInt("meals_donated")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[DonationDAO] MySQL stats notice (using fallback): " + e.getMessage());
        }

        // In-memory calculation fallback
        int listed = 0;
        int claimed = 0;
        int delivered = 0;
        int meals = 0;

        for (FoodDonation d : fallbackDonations) {
            if (d.getDonorId() == donorId) {
                listed++;
                if (d.getStatus() == DonationStatus.CLAIMED) claimed++;
                if (d.getStatus() == DonationStatus.DELIVERED) {
                    delivered++;
                    meals += d.getQuantity();
                }
            }
        }

        return new DonorStats(listed, claimed, delivered, meals);
    }

    public static FoodDonation findFallbackById(int id) {
        for (FoodDonation d : fallbackDonations) {
            if (d.getId() == id) return d;
        }
        return null;
    }

    private boolean updateFallbackStatus(int donationId, DonationStatus newStatus) {
        for (FoodDonation d : fallbackDonations) {
            if (d.getId() == donationId && d.getStatus() == DonationStatus.AVAILABLE) {
                d.setStatus(newStatus);
                return true;
            }
        }
        return false;
    }

    /**
     * Fetches all available and unexpired food donations,
     * supporting dynamic category filtering and city/address search.
     * Strictly uses PreparedStatement.
     */
    public List<FoodDonation> findAvailable(String categoryFilter, String citySearch) {
        List<FoodDonation> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT d.id, d.donor_id, d.food_name, d.category, d.quantity, d.prep_time, d.expiry_time, " +
                "       d.pickup_address, d.status, d.notes, d.created_at, d.occasion, d.is_demo, u.name AS donor_name " +
                "FROM Donations d " +
                "JOIN Users u ON d.donor_id = u.id " +
                "WHERE d.status = 'Available' AND d.expiry_time > NOW() "
        );

        boolean hasCat = categoryFilter != null && !categoryFilter.equalsIgnoreCase("All") && !categoryFilter.trim().isEmpty();
        boolean hasCity = citySearch != null && !citySearch.trim().isEmpty();

        if (hasCat) {
            sql.append("AND d.category = ? ");
        }
        if (hasCity) {
            sql.append("AND LOWER(d.pickup_address) LIKE ? ");
        }
        sql.append("ORDER BY d.expiry_time ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            int paramIdx = 1;
            if (hasCat) {
                pstmt.setString(paramIdx++, categoryFilter.trim());
            }
            if (hasCity) {
                pstmt.setString(paramIdx++, "%" + citySearch.trim().toLowerCase() + "%");
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDonation(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            System.err.println("[DonationDAO] MySQL findAvailable notice (using fallback): " + e.getMessage());
            // Filter fallback donations
            LocalDateTime now = LocalDateTime.now();
            List<FoodDonation> fb = new ArrayList<>();
            for (FoodDonation d : fallbackDonations) {
                if (d.getStatus() == DonationStatus.AVAILABLE && d.getExpiryTime().isAfter(now)) {
                    if (hasCat && !d.getCategory().equalsIgnoreCase(categoryFilter.trim())) {
                        continue;
                    }
                    if (hasCity && !d.getPickupAddress().toLowerCase().contains(citySearch.trim().toLowerCase())) {
                        continue;
                    }
                    fb.add(d);
                }
            }
            return fb;
        }
    }

    private FoodDonation mapResultSetToDonation(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int donorId = rs.getInt("donor_id");
        String foodName = rs.getString("food_name");
        String category = rs.getString("category");
        int quantity = rs.getInt("quantity");
        LocalDateTime prepTime = rs.getTimestamp("prep_time").toLocalDateTime();
        LocalDateTime expiryTime = rs.getTimestamp("expiry_time").toLocalDateTime();
        String address = rs.getString("pickup_address");
        DonationStatus status = DonationStatus.fromString(rs.getString("status"));
        String notes = rs.getString("notes");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

        FoodDonation d = new FoodDonation(id, donorId, foodName, category, quantity, prepTime, expiryTime, address, status, notes, createdAt);
        try {
            d.setOccasion(rs.getString("occasion"));
            d.setDemo(rs.getBoolean("is_demo"));
        } catch (SQLException ignored) {}

        try {
            String donorName = rs.getString("donor_name");
            if (donorName != null) d.setDonorName(donorName);
        } catch (SQLException ignored) {}

        return d;
    }

    /**
     * DTO for Donor statistics counters.
     */
    public static class DonorStats {
        private final int totalListed;
        private final int totalClaimed;
        private final int totalDelivered;
        private final int mealsDonated;

        public DonorStats(int totalListed, int totalClaimed, int totalDelivered, int mealsDonated) {
            this.totalListed = totalListed;
            this.totalClaimed = totalClaimed;
            this.totalDelivered = totalDelivered;
            this.mealsDonated = mealsDonated;
        }

        public int getTotalListed() { return totalListed; }
        public int getTotalClaimed() { return totalClaimed; }
        public int getTotalDelivered() { return totalDelivered; }
        public int getMealsDonated() { return mealsDonated; }
    }
}
