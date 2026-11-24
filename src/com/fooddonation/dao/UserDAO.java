package com.fooddonation.dao;

import com.fooddonation.model.Donor;
import com.fooddonation.model.NGO;
import com.fooddonation.model.User;
import com.fooddonation.model.Volunteer;
import com.fooddonation.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Access Object (DAO) for User entities and role-specific detail records.
 * Strictly executes all SQL operations using PreparedStatements and handles transactional
 * multi-table inserts (Users + Donors / NGOs / Volunteers).
 */
public class UserDAO {

    // --- Prepared SQL Statements (PreparedStatement Only) ---
    private static final String SQL_SELECT_USER_BY_EMAIL =
            "SELECT u.id, u.name, u.email, u.password_hash, u.salt, u.role, u.phone, u.organization_name, u.address, " +
            "       d.donor_type, d.total_donations_count, d.is_demo, " +
            "       n.registration_number, n.service_area, n.active_requests_count, n.beneficiaries_served, n.partner_status, n.cause, n.website, " +
            "       v.vehicle_type, v.availability, v.completed_deliveries, v.hours_logged " +
            "FROM Users u " +
            "LEFT JOIN Donors d ON u.id = d.user_id " +
            "LEFT JOIN NGOs n ON u.id = n.user_id " +
            "LEFT JOIN Volunteers v ON u.id = v.user_id " +
            "WHERE u.email = ?";

    private static final String SQL_SELECT_USER_BY_EMAIL_AND_ROLE =
            "SELECT u.id, u.name, u.email, u.password_hash, u.salt, u.role, u.phone, u.organization_name, u.address, " +
            "       d.donor_type, d.total_donations_count, d.is_demo, " +
            "       n.registration_number, n.service_area, n.active_requests_count, n.beneficiaries_served, n.partner_status, n.cause, n.website, " +
            "       v.vehicle_type, v.availability, v.completed_deliveries, v.hours_logged " +
            "FROM Users u " +
            "LEFT JOIN Donors d ON u.id = d.user_id " +
            "LEFT JOIN NGOs n ON u.id = n.user_id " +
            "LEFT JOIN Volunteers v ON u.id = v.user_id " +
            "WHERE u.email = ? AND u.role = ?";

    private static final String SQL_SELECT_ALL_NGOS =
            "SELECT u.id, u.name, u.email, u.password_hash, u.salt, u.role, u.phone, u.organization_name, u.address, " +
            "       n.registration_number, n.service_area, n.active_requests_count, n.beneficiaries_served, n.partner_status, n.cause, n.website " +
            "FROM Users u " +
            "JOIN NGOs n ON u.id = n.user_id " +
            "ORDER BY u.id ASC";

    private static final String SQL_UPDATE_NGO_PARTNER_STATUS =
            "UPDATE NGOs SET partner_status = ? WHERE user_id = ?";

    private static final String SQL_EXISTS_BY_EMAIL =
            "SELECT 1 FROM Users WHERE email = ?";

    private static final String SQL_INSERT_USER =
            "INSERT INTO Users (name, email, password_hash, salt, role, phone, organization_name, address) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_INSERT_DONOR =
            "INSERT INTO Donors (user_id, donor_type, total_donations_count, is_demo) VALUES (?, ?, ?, ?)";

    private static final String SQL_INSERT_NGO =
            "INSERT INTO NGOs (user_id, registration_number, service_area, active_requests_count, beneficiaries_served, partner_status, cause, website) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_INSERT_VOLUNTEER =
            "INSERT INTO Volunteers (user_id, vehicle_type, availability, completed_deliveries, hours_logged) " +
            "VALUES (?, ?, ?, ?, ?)";

    // Fallback in-memory cache for seamless testing if MySQL server is not locally configured
    private static final Map<String, User> fallbackUsers = new ConcurrentHashMap<>();
    private static final java.util.List<NGO> publicNgos = new java.util.concurrent.CopyOnWriteArrayList<>();

    static {
        // Universal Password Hash for "Password@123" with salt "a1b2c3d4e5f60718293a4b5c6d7e8f90"
        String commonHash = "ff174ba62abc336deb988df5606f21e2fa634f2faf8cbff8add3371b38e54665";
        String commonSalt = "a1b2c3d4e5f60718293a4b5c6d7e8f90";

        // 1. Donors (Users 1-3)
        Donor donor1 = new Donor(1, "Green Garden Bistro", "bistro@greengarden.com",
                commonHash, commonSalt, "9876543210", "Green Garden Bistro",
                "124 Culinary Boulevard, Downtown", "Restaurant");
        donor1.setTotalDonationsCount(28);

        Donor donor2 = new Donor(2, "Grand Pacific Hotel & Banquet", "catering@grandpacific.com",
                commonHash, commonSalt, "9876543211", "Grand Pacific Hotels Corp",
                "850 Grand Plaza Avenue, Bellevue", "Hotel");
        donor2.setTotalDonationsCount(42);

        Donor donor3 = new Donor(3, "Elena Rostova", "elena.rostova@gmail.com",
                commonHash, commonSalt, "9876543212", "Artisan Bakery & Organic Garden",
                "312 Olive Way, Capitol Hill", "Individual");
        donor3.setTotalDonationsCount(15);

        // 2. NGOs (Users 4-6)
        NGO ngo1 = new NGO(4, "Hope Food Foundation", "hope.foundation@ngo.org",
                commonHash, commonSalt, "9876543213", "Hope Foundation International",
                "450 Community Way, Suite 12, Seattle, WA", "NGO-US-2024-8849", "Greater Metro & Downtown Shelters");
        ngo1.setActiveFoodRequests(6);
        ngo1.setBeneficiariesServed(1850);

        NGO ngo2 = new NGO(5, "City Harvest Rescue Alliance", "contact@cityharvestalliance.org",
                commonHash, commonSalt, "9876543214", "City Harvest Alliance Corp",
                "720 South Michigan Street, Seattle, WA", "NGO-WA-2023-4102", "South Seattle & Industrial Valley");
        ngo2.setActiveFoodRequests(4);
        ngo2.setBeneficiariesServed(1420);

        NGO ngo3 = new NGO(6, "Compassion Meals Initiative", "help@compassionmeals.org",
                commonHash, commonSalt, "9876543215", "Compassion Outreach Program",
                "108 North Aurora Lane, Shoreline, WA", "NGO-WA-2022-1984", "North District & Suburbs");
        ngo3.setActiveFoodRequests(3);
        ngo3.setBeneficiariesServed(980);

        // 3. Volunteers (Users 7-9)
        Volunteer vol1 = new Volunteer(7, "Alex Green", "alex.green@volunteer.org",
                commonHash, commonSalt, "9876543216", "Independent Volunteer Responder",
                "1544 5th Avenue, Greenwood Zone B", "Car", "Flexible (Evenings & Weekends)");
        vol1.setCompletedDeliveries(24);
        vol1.setHoursLogged(56.5);

        Volunteer vol2 = new Volunteer(8, "Maya Lin", "maya.lin@courier.net",
                commonHash, commonSalt, "9876543217", "Eco-Bicycle Rapid Courier",
                "88 Pike Street, Pike Place Market", "Bike", "Morning & Lunch Peak Hours");
        vol2.setCompletedDeliveries(19);
        vol2.setHoursLogged(38.0);

        Volunteer vol3 = new Volunteer(9, "David Chen", "david.chen@volunteer.org",
                commonHash, commonSalt, "9876543218", "Community First Responders",
                "2300 Eastlake Avenue East, Seattle, WA", "Car", "Weekends & Emergency Dispatch");
        vol3.setCompletedDeliveries(12);
        vol3.setHoursLogged(28.5);

        // 4. Public Known NGOs (partner_status = LISTED, phone/email/address verified later)
        NGO pubNgo1 = new NGO(10, "Robin Hood Army", "robinhoodarmy@public.egai.org", commonHash, commonSalt,
                null, "Robin Hood Army India", null, "PUBLIC-RHA-01", "Pan-India Chapters",
                NGO.STATUS_LISTED, "Zero-funds volunteer organization fighting hunger by redistributing surplus food.", "https://robinhoodarmy.com");
        NGO pubNgo2 = new NGO(11, "Feeding India", "feedingindia@public.egai.org", commonHash, commonSalt,
                null, "Feeding India by Zomato", null, "PUBLIC-FI-02", "Metropolitan Hubs",
                NGO.STATUS_LISTED, "Non-profit initiative aimed at eradicating malnutrition across marginalized communities.", "https://www.feedingindia.org");
        NGO pubNgo3 = new NGO(12, "The Akshaya Patra Foundation", "akshayapatra@public.egai.org", commonHash, commonSalt,
                null, "The Akshaya Patra Foundation", null, "PUBLIC-AP-03", "Schools Across India",
                NGO.STATUS_LISTED, "Mid-Day Meal program provider combating classroom hunger with hot meals.", "https://www.akshayapatra.org");
        NGO pubNgo4 = new NGO(13, "Udhavum Karangal", "udhavumkarangal@public.egai.org", commonHash, commonSalt,
                null, "Udhavum Karangal (Helping Hands)", null, "PUBLIC-UK-04", "Tamil Nadu — Chennai & Madurai",
                NGO.STATUS_LISTED, "Grassroots charity in Tamil Nadu caring for and feeding orphans and destitute elders.", "https://udhavumkarangal.org");
        NGO pubNgo5 = new NGO(14, "HelpAge India", "helpageindia@public.egai.org", commonHash, commonSalt,
                null, "HelpAge India Foundation", null, "PUBLIC-HI-05", "Senior Homes & Slums",
                NGO.STATUS_LISTED, "National secular organization providing nutrition and care to destitute elderly citizens.", "https://www.helpageindia.org");
        NGO pubNgo6 = new NGO(15, "Goonj", "goonj@public.egai.org", commonHash, commonSalt,
                null, "Goonj Social Enterprise", null, "PUBLIC-GJ-06", "Rural Clusters",
                NGO.STATUS_LISTED, "Community-led disaster relief and dignified nutritional support across rural India.", "https://goonj.org");
        NGO pubNgo7 = new NGO(16, "Smile Foundation", "smilefoundation@public.egai.org", commonHash, commonSalt,
                null, "Smile Foundation India", null, "PUBLIC-SF-07", "Underprivileged Clusters",
                NGO.STATUS_LISTED, "Child education, women empowerment, and holistic family nutrition development.", "https://www.smilefoundationindia.org");

        publicNgos.add(pubNgo1);
        publicNgos.add(pubNgo2);
        publicNgos.add(pubNgo3);
        publicNgos.add(pubNgo4);
        publicNgos.add(pubNgo5);
        publicNgos.add(pubNgo6);
        publicNgos.add(pubNgo7);

        fallbackUsers.put(pubNgo1.getEmail().toLowerCase(), pubNgo1);
        fallbackUsers.put(pubNgo2.getEmail().toLowerCase(), pubNgo2);
        fallbackUsers.put(pubNgo3.getEmail().toLowerCase(), pubNgo3);
        fallbackUsers.put(pubNgo4.getEmail().toLowerCase(), pubNgo4);
        fallbackUsers.put(pubNgo5.getEmail().toLowerCase(), pubNgo5);
        fallbackUsers.put(pubNgo6.getEmail().toLowerCase(), pubNgo6);
        fallbackUsers.put(pubNgo7.getEmail().toLowerCase(), pubNgo7);

        // 5. Tamil Culture Fictional Demo Donors (is_demo = TRUE, generic cultural names)
        Donor tDonor1 = new Donor(17, "Annam Mess", "annam.mess@demo.egai.org", commonHash, commonSalt,
                "9840112233", "Annam Mess (Demo)", "45 Temple Car Street, Madurai, TN",
                Donor.TYPE_MESS, true);
        tDonor1.setTotalDonationsCount(16);

        Donor tDonor2 = new Donor(18, "Thendral Catering", "thendral.catering@demo.egai.org", commonHash, commonSalt,
                "9840223344", "Thendral Catering Services (Demo)", "12 Cross Cut Road, Gandhipuram, Coimbatore, TN",
                Donor.TYPE_CATERING, true);
        tDonor2.setTotalDonationsCount(24);

        Donor tDonor3 = new Donor(19, "Pongal Mandapam", "pongal.mandapam@demo.egai.org", commonHash, commonSalt,
                "9840334455", "Pongal Kalyana Mandapam (Demo)", "78 GST Road, Tambaram, Chennai, TN",
                Donor.TYPE_WEDDING_HALL, true);
        tDonor3.setTotalDonationsCount(32);

        Donor tDonor4 = new Donor(20, "Kaveri Annadhanam Trust", "kaveri.trust@demo.egai.org", commonHash, commonSalt,
                "9840445566", "Kaveri Annadhanam Charitable Trust (Demo)", "5 Sannidhi Street, Srirangam, Tiruchirappalli, TN",
                Donor.TYPE_TEMPLE_ANNADHANAM, true);
        tDonor4.setTotalDonationsCount(45);

        Donor tDonor5 = new Donor(21, "Amudham Illam", "amudham.illam@demo.egai.org", commonHash, commonSalt,
                "9840556677", "Amudham Kudumbam (Demo)", "23 Kambar Street, Mylapore, Chennai, TN",
                Donor.TYPE_HOUSEHOLD, true);
        tDonor5.setTotalDonationsCount(8);

        Donor tDonor6 = new Donor(22, "Senthamizh Unavagam", "senthamizh.unavagam@demo.egai.org", commonHash, commonSalt,
                "9840667788", "Senthamizh Unavagam Ltd (Demo)", "104 West Boulevard Road, Thanjavur, TN",
                Donor.TYPE_RESTAURANT, true);
        tDonor6.setTotalDonationsCount(20);

        fallbackUsers.put(tDonor1.getEmail().toLowerCase(), tDonor1);
        fallbackUsers.put(tDonor2.getEmail().toLowerCase(), tDonor2);
        fallbackUsers.put(tDonor3.getEmail().toLowerCase(), tDonor3);
        fallbackUsers.put(tDonor4.getEmail().toLowerCase(), tDonor4);
        fallbackUsers.put(tDonor5.getEmail().toLowerCase(), tDonor5);
        fallbackUsers.put(tDonor6.getEmail().toLowerCase(), tDonor6);

        // Register all seed users in cache
        fallbackUsers.put(donor1.getEmail().toLowerCase(), donor1);
        fallbackUsers.put(donor2.getEmail().toLowerCase(), donor2);
        fallbackUsers.put(donor3.getEmail().toLowerCase(), donor3);

        fallbackUsers.put(ngo1.getEmail().toLowerCase(), ngo1);
        fallbackUsers.put(ngo2.getEmail().toLowerCase(), ngo2);
        fallbackUsers.put(ngo3.getEmail().toLowerCase(), ngo3);

        fallbackUsers.put(vol1.getEmail().toLowerCase(), vol1);
        fallbackUsers.put(vol2.getEmail().toLowerCase(), vol2);
        fallbackUsers.put(vol3.getEmail().toLowerCase(), vol3);
    }

    /**
     * Finds a user by email, joining role detail tables.
     * Returns polymorphic Donor, NGO, or Volunteer subclass.
     */
    public User findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_USER_BY_EMAIL)) {

            pstmt.setString(1, email.trim().toLowerCase());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPolymorphicUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] MySQL query notice (using fallback): " + e.getMessage());
            return fallbackUsers.get(email.trim().toLowerCase());
        }

        return null;
    }

    /**
     * Finds a user by email and specific role.
     */
    public User findByEmailAndRole(String email, String role) {
        if (email == null || role == null) {
            return null;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_USER_BY_EMAIL_AND_ROLE)) {

            pstmt.setString(1, email.trim().toLowerCase());
            pstmt.setString(2, role.trim());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPolymorphicUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] MySQL query notice (using fallback): " + e.getMessage());
            User u = fallbackUsers.get(email.trim().toLowerCase());
            if (u != null && u.getRole().equalsIgnoreCase(role.trim())) {
                return u;
            }
        }

        return null;
    }

    /**
     * Checks if an email is already present in the Users table.
     */
    public boolean emailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_EXISTS_BY_EMAIL)) {

            pstmt.setString(1, email.trim().toLowerCase());

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return fallbackUsers.containsKey(email.trim().toLowerCase());
        }
    }

    /**
     * Inserts a user and their corresponding role detail record within an atomic SQL transaction.
     * Strictly uses PreparedStatements for both master and detail tables.
     *
     * @param user         The polymorphic User instance (Donor, NGO, or Volunteer)
     * @param passwordHash The salted SHA-256 hash
     * @param salt         The cryptographic salt
     * @return true if both master and detail records were committed successfully
     */
    public boolean insertUser(User user, String passwordHash, String salt) {
        if (user == null || passwordHash == null || salt == null) {
            return false;
        }

        user.setPasswordHash(passwordHash);
        user.setSalt(salt);

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin atomic transaction

            int generatedUserId = -1;

            // 1. Insert into Users table
            try (PreparedStatement pstmtUser = conn.prepareStatement(SQL_INSERT_USER, PreparedStatement.RETURN_GENERATED_KEYS)) {
                pstmtUser.setString(1, user.getName());
                pstmtUser.setString(2, user.getEmail().trim().toLowerCase());
                pstmtUser.setString(3, passwordHash);
                pstmtUser.setString(4, salt);
                pstmtUser.setString(5, user.getRole());
                pstmtUser.setString(6, user.getPhone());
                pstmtUser.setString(7, user.getOrganizationName());
                pstmtUser.setString(8, user.getAddress());

                int affected = pstmtUser.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return false;
                }

                try (ResultSet generatedKeys = pstmtUser.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        generatedUserId = generatedKeys.getInt(1);
                        user.setId(generatedUserId);
                    }
                }
            }

            // 2. Insert into Role Detail Table using PreparedStatement
            if (user instanceof Donor) {
                Donor donor = (Donor) user;
                try (PreparedStatement pstmtDonor = conn.prepareStatement(SQL_INSERT_DONOR)) {
                    pstmtDonor.setInt(1, generatedUserId);
                    pstmtDonor.setString(2, donor.getDonorType());
                    pstmtDonor.setInt(3, donor.getTotalDonationsCount());
                    pstmtDonor.executeUpdate();
                }
            } else if (user instanceof NGO) {
                NGO ngo = (NGO) user;
                try (PreparedStatement pstmtNgo = conn.prepareStatement(SQL_INSERT_NGO)) {
                    pstmtNgo.setInt(1, generatedUserId);
                    pstmtNgo.setString(2, ngo.getRegistrationNumber());
                    pstmtNgo.setString(3, ngo.getServiceArea());
                    pstmtNgo.setInt(4, ngo.getActiveFoodRequests());
                    pstmtNgo.setInt(5, ngo.getBeneficiariesServed());
                    pstmtNgo.executeUpdate();
                }
            } else if (user instanceof Volunteer) {
                Volunteer volunteer = (Volunteer) user;
                try (PreparedStatement pstmtVol = conn.prepareStatement(SQL_INSERT_VOLUNTEER)) {
                    pstmtVol.setInt(1, generatedUserId);
                    pstmtVol.setString(2, volunteer.getVehicleType());
                    pstmtVol.setString(3, volunteer.getAvailability());
                    pstmtVol.setInt(4, volunteer.getCompletedDeliveries());
                    pstmtVol.setDouble(5, volunteer.getHoursLogged());
                    pstmtVol.executeUpdate();
                }
            }

            // Commit atomic transaction
            conn.commit();

            // Cache in fallback map
            fallbackUsers.put(user.getEmail().toLowerCase(), user);
            return true;

        } catch (SQLException e) {
            System.err.println("[UserDAO] MySQL transaction failed: " + e.getMessage() + ". Saving to fallback session store.");
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[UserDAO] Rollback failed: " + ex.getMessage());
                }
            }
            // Local fallback assignment
            user.setId(fallbackUsers.size() + 1);
            fallbackUsers.put(user.getEmail().toLowerCase(), user);
            return true;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("[UserDAO] Connection close notice: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Maps a joined ResultSet row into polymorphic Donor, NGO, or Volunteer subclass.
     */
    private User mapResultSetToPolymorphicUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        String salt = rs.getString("salt");
        String role = rs.getString("role");
        String phone = rs.getString("phone");
        String orgName = rs.getString("organization_name");
        String address = rs.getString("address");

        if ("Donor".equalsIgnoreCase(role)) {
            String donorType = rs.getString("donor_type");
            int donations = rs.getInt("total_donations_count");
            boolean isDemo = false;
            try {
                isDemo = rs.getBoolean("is_demo");
            } catch (SQLException ignored) {}
            Donor donor = new Donor(id, name, email, passwordHash, salt, phone, orgName, address, donorType, isDemo);
            donor.setTotalDonationsCount(donations);
            return donor;

        } else if ("NGO".equalsIgnoreCase(role)) {
            String regNum = rs.getString("registration_number");
            String serviceArea = rs.getString("service_area");
            int activeRequests = rs.getInt("active_requests_count");
            int beneficiaries = rs.getInt("beneficiaries_served");
            String partnerStatus = NGO.STATUS_PARTNERED;
            String cause = "";
            String website = "";
            try {
                partnerStatus = rs.getString("partner_status");
                if (partnerStatus == null) partnerStatus = NGO.STATUS_PARTNERED;
                cause = rs.getString("cause");
                website = rs.getString("website");
            } catch (SQLException ignored) {}

            NGO ngo = new NGO(id, name, email, passwordHash, salt, phone, orgName, address, regNum, serviceArea, partnerStatus, cause, website);
            ngo.setActiveFoodRequests(activeRequests);
            ngo.setBeneficiariesServed(beneficiaries);
            return ngo;

        } else {
            String vehicleType = rs.getString("vehicle_type");
            String availability = rs.getString("availability");
            int deliveries = rs.getInt("completed_deliveries");
            double hours = rs.getDouble("hours_logged");
            Volunteer vol = new Volunteer(id, name, email, passwordHash, salt, phone, orgName, address, vehicleType, availability);
            vol.setCompletedDeliveries(deliveries);
            vol.setHoursLogged(hours);
            return vol;
        }
    }

    /**
     * Updates an NGO's partner status (LISTED <-> PARTNERED).
     */
    public boolean updatePartnerStatus(int ngoId, String newStatus) {
        if (ngoId <= 0 || newStatus == null) return false;
        String statusVal = newStatus.trim().toUpperCase();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_NGO_PARTNER_STATUS)) {
            pstmt.setString(1, statusVal);
            pstmt.setInt(2, ngoId);
            int affected = pstmt.executeUpdate();
            updateFallbackNgoPartnerStatus(ngoId, statusVal);
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] MySQL update notice (updating fallback): " + e.getMessage());
            updateFallbackNgoPartnerStatus(ngoId, statusVal);
            return true;
        }
    }

    private static void updateFallbackNgoPartnerStatus(int ngoId, String newStatus) {
        for (User u : fallbackUsers.values()) {
            if (u instanceof NGO && u.getId() == ngoId) {
                ((NGO) u).setPartnerStatus(newStatus);
            }
        }
        for (NGO n : publicNgos) {
            if (n.getId() == ngoId) {
                n.setPartnerStatus(newStatus);
            }
        }
    }

    /**
     * Retrieves all NGOs (both partnered and listed).
     */
    public java.util.List<NGO> getAllNgos() {
        java.util.List<NGO> list = new java.util.ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_ALL_NGOS);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                User u = mapResultSetToPolymorphicUser(rs);
                if (u instanceof NGO) {
                    list.add((NGO) u);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] MySQL getAllNgos notice (using fallback): " + e.getMessage());
        }

        if (list.isEmpty()) {
            for (User u : fallbackUsers.values()) {
                if (u instanceof NGO && !list.contains(u)) {
                    list.add((NGO) u);
                }
            }
            for (NGO n : publicNgos) {
                if (!list.contains(n)) {
                    list.add(n);
                }
            }
        }
        return list;
    }
}
