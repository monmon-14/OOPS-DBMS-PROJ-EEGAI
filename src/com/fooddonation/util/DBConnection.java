package com.fooddonation.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Database Connection Manager for the Food Donation Application.
 * Handles MySQL JDBC connections, configuration management via db.properties / environment,
 * and provides automatic database initialization and seeding.
 */
public class DBConnection {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 3306;
    private static final String DEFAULT_DB = "food_donation_db";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "root"; // Edit here or in db.properties

    private static String url;
    private static String user;
    private static String password;
    private static boolean isInitialized = false;
    private static boolean useMockFallback = false;

    static {
        loadConfiguration();
        try {
            // Register MySQL Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] MySQL JDBC Driver not found on classpath: " + e.getMessage());
        }
    }

    /**
     * Loads connection settings from db.properties, system properties, or sensible defaults.
     */
    public static void loadConfiguration() {
        Properties props = new Properties();

        // 1. Try reading from db.properties file if present
        File propFile = new File("db.properties");
        if (propFile.exists()) {
            try (InputStream in = new FileInputStream(propFile)) {
                props.load(in);
            } catch (Exception e) {
                System.err.println("[DBConnection] Warning: Could not read db.properties: " + e.getMessage());
            }
        }

        // 2. Read values with fallback precedence: System Property > db.properties > Default
        String host = System.getProperty("db.host", props.getProperty("db.host", DEFAULT_HOST));
        String port = System.getProperty("db.port", props.getProperty("db.port", String.valueOf(DEFAULT_PORT)));
        String dbName = System.getProperty("db.name", props.getProperty("db.name", DEFAULT_DB));
        user = System.getProperty("db.user", props.getProperty("db.user", DEFAULT_USER));
        password = System.getProperty("db.password", props.getProperty("db.password", DEFAULT_PASS));

        url = System.getProperty("db.url", props.getProperty("db.url",
                String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true",
                        host, port, dbName)));
    }

    /**
     * Obtains a standard SQL Connection to the MySQL database.
     *
     * @return active java.sql.Connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        if (useMockFallback) {
            throw new SQLException("Operating in In-Memory Demo Fallback Mode (No active MySQL connection).");
        }
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Checks if MySQL server is reachable and credentials are valid.
     */
    public static boolean testConnection() {
        if (useMockFallback) {
            return false;
        }
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Automatically initializes the database schema and sample records
     * if they do not already exist in the connected MySQL instance.
     */
    public static synchronized void initializeDatabase() {
        if (isInitialized || useMockFallback) {
            return;
        }

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Create Users table
            String createUsersSQL = "CREATE TABLE IF NOT EXISTS Users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(150) NOT NULL UNIQUE, " +
                    "password_hash VARCHAR(64) NOT NULL, " +
                    "salt VARCHAR(32) NOT NULL, " +
                    "role ENUM('Donor', 'NGO', 'Volunteer') NOT NULL, " +
                    "phone VARCHAR(20) NOT NULL, " +
                    "organization_name VARCHAR(150), " +
                    "address VARCHAR(255) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "INDEX idx_user_email (email), " +
                    "INDEX idx_user_role (role)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            stmt.execute(createUsersSQL);

            // Create Donors table
            String createDonorsSQL = "CREATE TABLE IF NOT EXISTS Donors (" +
                    "user_id INT PRIMARY KEY, " +
                    "donor_type ENUM('Restaurant', 'Hotel', 'Individual') NOT NULL, " +
                    "total_donations_count INT DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_donor_user FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            stmt.execute(createDonorsSQL);

            // Create NGOs table
            String createNgosSQL = "CREATE TABLE IF NOT EXISTS NGOs (" +
                    "user_id INT PRIMARY KEY, " +
                    "registration_number VARCHAR(100) NOT NULL, " +
                    "service_area VARCHAR(150) NOT NULL, " +
                    "active_requests_count INT DEFAULT 0, " +
                    "beneficiaries_served INT DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_ngo_user FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            stmt.execute(createNgosSQL);

            // Create Volunteers table
            String createVolunteersSQL = "CREATE TABLE IF NOT EXISTS Volunteers (" +
                    "user_id INT PRIMARY KEY, " +
                    "vehicle_type ENUM('None', 'Bike', 'Car') NOT NULL, " +
                    "availability VARCHAR(100) NOT NULL, " +
                    "completed_deliveries INT DEFAULT 0, " +
                    "hours_logged DECIMAL(6, 2) DEFAULT 0.0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_volunteer_user FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            stmt.execute(createVolunteersSQL);

            // Create Donations table
            String createDonationsSQL = "CREATE TABLE IF NOT EXISTS Donations (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "donor_id INT NOT NULL, " +
                    "food_name VARCHAR(150) NOT NULL, " +
                    "category ENUM('Veg', 'Non-Veg') NOT NULL, " +
                    "quantity INT NOT NULL, " +
                    "prep_time DATETIME NOT NULL, " +
                    "expiry_time DATETIME NOT NULL, " +
                    "pickup_address VARCHAR(255) NOT NULL, " +
                    "status ENUM('Available', 'Claimed', 'Picked Up', 'Delivered', 'Expired', 'Cancelled') NOT NULL DEFAULT 'Available', " +
                    "notes TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_donation_donor FOREIGN KEY (donor_id) REFERENCES Users(id) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            stmt.execute(createDonationsSQL);

            // Create Locations table
            String createLocationsSQL = "CREATE TABLE IF NOT EXISTS Locations (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "address VARCHAR(255) NOT NULL, " +
                    "city VARCHAR(100) NOT NULL, " +
                    "area VARCHAR(100) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_location_user FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            stmt.execute(createLocationsSQL);

            // Create Claims table
            String createClaimsSQL = "CREATE TABLE IF NOT EXISTS Claims (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "donation_id INT NOT NULL, " +
                    "ngo_id INT NOT NULL, " +
                    "volunteer_id INT DEFAULT NULL, " +
                    "status ENUM('Claimed', 'Volunteer Assigned', 'Picked Up', 'Delivered', 'Cancelled') NOT NULL DEFAULT 'Claimed', " +
                    "claimed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_claim_donation FOREIGN KEY (donation_id) REFERENCES Donations(id) ON DELETE CASCADE, " +
                    "CONSTRAINT fk_claim_ngo FOREIGN KEY (ngo_id) REFERENCES Users(id) ON DELETE CASCADE, " +
                    "CONSTRAINT fk_claim_volunteer FOREIGN KEY (volunteer_id) REFERENCES Users(id) ON DELETE SET NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            stmt.execute(createClaimsSQL);

            // Create Deliveries table
            String createDeliveriesSQL = "CREATE TABLE IF NOT EXISTS Deliveries (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "claim_id INT NOT NULL, " +
                    "volunteer_id INT NOT NULL, " +
                    "status ENUM('Accepted', 'Picked Up', 'Delivered', 'Cancelled') NOT NULL DEFAULT 'Accepted', " +
                    "picked_at DATETIME DEFAULT NULL, " +
                    "delivered_at DATETIME DEFAULT NULL, " +
                    "meals_delivered INT DEFAULT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_delivery_claim FOREIGN KEY (claim_id) REFERENCES Claims(id) ON DELETE CASCADE, " +
                    "CONSTRAINT fk_delivery_volunteer FOREIGN KEY (volunteer_id) REFERENCES Users(id) ON DELETE RESTRICT" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            stmt.execute(createDeliveriesSQL);

            // Seed default accounts if empty
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM Users");
            if (rs.next() && rs.getInt(1) == 0) {
                String commonHash = "ff174ba62abc336deb988df5606f21e2fa634f2faf8cbff8add3371b38e54665";
                String commonSalt = "a1b2c3d4e5f60718293a4b5c6d7e8f90";

                // Seed 3 Donors
                stmt.executeUpdate("INSERT INTO Users (id, name, email, password_hash, salt, role, phone, organization_name, address) VALUES " +
                        "(1, 'Green Garden Bistro', 'bistro@greengarden.com', '" + commonHash + "', '" + commonSalt + "', 'Donor', '9876543210', 'Green Garden Bistro', '124 Culinary Boulevard, Downtown'), " +
                        "(2, 'Grand Pacific Hotel & Banquet', 'catering@grandpacific.com', '" + commonHash + "', '" + commonSalt + "', 'Donor', '9876543211', 'Grand Pacific Hotels Corp', '850 Grand Plaza Avenue, Bellevue'), " +
                        "(3, 'Elena Rostova', 'elena.rostova@gmail.com', '" + commonHash + "', '" + commonSalt + "', 'Donor', '9876543212', 'Artisan Bakery', '312 Olive Way, Capitol Hill')");
                stmt.executeUpdate("INSERT INTO Donors (user_id, donor_type, total_donations_count) VALUES (1, 'Restaurant', 28), (2, 'Hotel', 42), (3, 'Individual', 15)");

                // Seed 3 NGOs
                stmt.executeUpdate("INSERT INTO Users (id, name, email, password_hash, salt, role, phone, organization_name, address) VALUES " +
                        "(4, 'Hope Food Foundation', 'hope.foundation@ngo.org', '" + commonHash + "', '" + commonSalt + "', 'NGO', '9876543213', 'Hope Foundation International', '450 Community Way, Suite 12, Seattle'), " +
                        "(5, 'City Harvest Rescue Alliance', 'contact@cityharvestalliance.org', '" + commonHash + "', '" + commonSalt + "', 'NGO', '9876543214', 'City Harvest Alliance', '720 South Michigan Street, Seattle'), " +
                        "(6, 'Compassion Meals Initiative', 'help@compassionmeals.org', '" + commonHash + "', '" + commonSalt + "', 'NGO', '9876543215', 'Compassion Meals', '108 North Aurora Lane, Shoreline')");
                stmt.executeUpdate("INSERT INTO NGOs (user_id, registration_number, service_area, active_requests_count, beneficiaries_served) VALUES " +
                        "(4, 'NGO-US-2024-8849', 'Greater Metro & Downtown Shelters', 6, 1850), " +
                        "(5, 'NGO-WA-2023-4102', 'South Seattle & Industrial Valley', 4, 1420), " +
                        "(6, 'NGO-WA-2022-1984', 'North District & Suburbs', 3, 980)");

                // Seed 3 Volunteers
                stmt.executeUpdate("INSERT INTO Users (id, name, email, password_hash, salt, role, phone, organization_name, address) VALUES " +
                        "(7, 'Alex Green', 'alex.green@volunteer.org', '" + commonHash + "', '" + commonSalt + "', 'Volunteer', '9876543216', 'Independent Volunteer Responder', '1544 5th Avenue, Greenwood Zone B'), " +
                        "(8, 'Maya Lin', 'maya.lin@courier.net', '" + commonHash + "', '" + commonSalt + "', 'Volunteer', '9876543217', 'Eco-Bicycle Rapid Courier', '88 Pike Street, Pike Place Market'), " +
                        "(9, 'David Chen', 'david.chen@volunteer.org', '" + commonHash + "', '" + commonSalt + "', 'Volunteer', '9876543218', 'Community First Responders', '2300 Eastlake Avenue East, Seattle')");
                stmt.executeUpdate("INSERT INTO Volunteers (user_id, vehicle_type, availability, completed_deliveries, hours_logged) VALUES " +
                        "(7, 'Car', 'Flexible (Evenings & Weekends)', 24, 56.5), " +
                        "(8, 'Bike', 'Morning & Lunch Peak Hours', 19, 38.0), " +
                        "(9, 'Car', 'Weekends & Emergency Dispatch', 12, 28.5)");

                // Seed 10 Sample Donations
                stmt.executeUpdate("INSERT INTO Donations (id, donor_id, food_name, category, quantity, prep_time, expiry_time, pickup_address, status, notes) VALUES " +
                        "(1, 1, 'Fresh Artisan Bread & Baguettes', 'Veg', 35, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 6 HOUR), '124 Culinary Boulevard, Downtown', 'Available', 'Freshly baked morning loaves.'), " +
                        "(2, 3, 'Organic Vegetable Medley & Garden Salad', 'Veg', 40, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 5 HOUR), '312 Olive Way, Capitol Hill', 'Available', 'Fresh heirloom greens.'), " +
                        "(3, 2, 'Steamed Basmati Rice & Dal Tadka', 'Veg', 60, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_ADD(NOW(), INTERVAL 4 HOUR), '850 Grand Plaza Avenue, Bellevue', 'Available', 'Banquet surplus.'), " +
                        "(4, 2, 'Grilled Chicken & Quinoa Bowls', 'Non-Veg', 45, DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_ADD(NOW(), INTERVAL 3 HOUR), '850 Grand Plaza Avenue, Bellevue', 'Claimed', 'Claimed by Hope Food Foundation.'), " +
                        "(5, 1, 'Hearty Minestrone Soup & Garlic Rolls', 'Veg', 50, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_ADD(NOW(), INTERVAL 3 HOUR), '124 Culinary Boulevard, Downtown', 'Claimed', 'Claimed by City Harvest Alliance.'), " +
                        "(6, 3, 'Steamed Tofu & Vegetable Stir-Fry', 'Veg', 30, DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR), '312 Olive Way, Capitol Hill', 'Picked Up', 'In transit with Maya Lin.'), " +
                        "(7, 1, 'Fresh Ciabatta & Herb Rolls', 'Veg', 25, DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR), '124 Culinary Boulevard, Downtown', 'Picked Up', 'In transit with Alex Green.'), " +
                        "(8, 2, 'Assorted Gourmet Wraps & Sandwiches', 'Non-Veg', 50, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 18 HOUR), '850 Grand Plaza Avenue, Bellevue', 'Delivered', 'Delivered to North Shoreline Youth Shelter.'), " +
                        "(9, 1, 'Royal Vegetable Biryani Feast', 'Veg', 80, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), '124 Culinary Boulevard, Downtown', 'Delivered', 'Delivered to Hope Community Shelter.'), " +
                        "(10, 2, 'Seasonal Fresh Fruit Parfaits & Yogurt', 'Veg', 30, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), '850 Grand Plaza Avenue, Bellevue', 'Expired', 'Exceeded safe consumption window.')");

                // Seed Claims
                stmt.executeUpdate("INSERT INTO Claims (id, donation_id, ngo_id, volunteer_id, status, claimed_at) VALUES " +
                        "(1, 4, 4, NULL, 'Claimed', DATE_SUB(NOW(), INTERVAL 3 HOUR)), " +
                        "(2, 5, 5, 7, 'Volunteer Assigned', DATE_SUB(NOW(), INTERVAL 2 HOUR)), " +
                        "(3, 6, 5, 8, 'Picked Up', DATE_SUB(NOW(), INTERVAL 3 HOUR)), " +
                        "(4, 7, 4, 7, 'Picked Up', DATE_SUB(NOW(), INTERVAL 4 HOUR)), " +
                        "(5, 8, 6, 9, 'Delivered', DATE_SUB(NOW(), INTERVAL 25 HOUR)), " +
                        "(6, 9, 4, 7, 'Delivered', DATE_SUB(NOW(), INTERVAL 49 HOUR))");

                // Seed Deliveries
                stmt.executeUpdate("INSERT INTO Deliveries (id, claim_id, volunteer_id, status, picked_at, delivered_at, meals_delivered) VALUES " +
                        "(1, 2, 7, 'Accepted', NULL, NULL, NULL), " +
                        "(2, 3, 8, 'Picked Up', DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, NULL), " +
                        "(3, 4, 7, 'Picked Up', DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, NULL), " +
                        "(4, 5, 9, 'Delivered', DATE_SUB(NOW(), INTERVAL 24 HOUR), DATE_SUB(NOW(), INTERVAL 23 HOUR), 50), " +
                        "(5, 6, 7, 'Delivered', DATE_SUB(NOW(), INTERVAL 48 HOUR), DATE_SUB(NOW(), INTERVAL 47 HOUR), 80)");

                System.out.println("[DBConnection] Seeded complete 9 Users, 10 Donations, Claims, and Deliveries successfully.");
            }
            isInitialized = true;
            System.out.println("[DBConnection] Complete database schema and donations verified and ready.");
        } catch (SQLException e) {
            System.err.println("[DBConnection] Notice: MySQL initialization deferred: " + e.getMessage());
        }
    }

    // --- Configuration Getters and Setters ---
    public static String getUrl() {
        return url;
    }

    public static void setUrl(String newUrl) {
        url = newUrl;
    }

    public static String getUser() {
        return user;
    }

    public static void setUser(String newUser) {
        user = newUser;
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String newPass) {
        password = newPass;
    }

    public static boolean isMockFallback() {
        return useMockFallback;
    }

    public static void setMockFallback(boolean fallback) {
        useMockFallback = fallback;
    }
}
