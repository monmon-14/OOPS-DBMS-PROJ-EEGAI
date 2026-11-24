-- ====================================================================
-- Food Donation System - Schema Migration Script
-- Updates `Users` table and introduces relational detail tables:
-- `Donors`, `NGOs`, and `Volunteers` with Foreign Keys
-- ====================================================================

USE food_donation_db;

-- 1. Update `Users` table role enum to include 'Donor'
ALTER TABLE Users 
    MODIFY COLUMN role ENUM('Donor', 'NGO', 'Volunteer') NOT NULL COMMENT 'Role indicator for RBAC and polymorphism';

-- Ensure phone and address columns exist with appropriate lengths
ALTER TABLE Users 
    MODIFY COLUMN phone VARCHAR(20) NOT NULL,
    MODIFY COLUMN address VARCHAR(255) NOT NULL;

-- --------------------------------------------------------------------
-- 2. Detail Table for Donors (Restaurants, Hotels, Individuals)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Donors (
    user_id INT PRIMARY KEY,
    donor_type ENUM('Restaurant', 'Hotel', 'Individual') NOT NULL,
    total_donations_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_donor_user FOREIGN KEY (user_id) 
        REFERENCES Users(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- 3. Detail Table for NGOs (Non-Governmental Organizations)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS NGOs (
    user_id INT PRIMARY KEY,
    registration_number VARCHAR(100) NOT NULL COMMENT 'NGO official registration / tax exemption number',
    service_area VARCHAR(150) NOT NULL COMMENT 'Primary operating borough, city, or district',
    active_requests_count INT DEFAULT 0,
    beneficiaries_served INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ngo_user FOREIGN KEY (user_id) 
        REFERENCES Users(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- 4. Detail Table for Volunteers (Food Rescue Couriers)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Volunteers (
    user_id INT PRIMARY KEY,
    vehicle_type ENUM('None', 'Bike', 'Car') NOT NULL,
    availability VARCHAR(100) NOT NULL COMMENT 'e.g., Weekdays, Weekends, Flexible',
    completed_deliveries INT DEFAULT 0,
    hours_logged DECIMAL(6, 2) DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_volunteer_user FOREIGN KEY (user_id) 
        REFERENCES Users(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- 5. Seed Demonstration Records for all 3 roles
-- Default Password for all seed accounts: Password@123
-- SHA-256 hash with salt: Password@123
-- --------------------------------------------------------------------

-- Donor Account: Fresh Bistro Restaurant
-- Salt: b2c3d4e5f60718293a4b5c6d7e8f90a1
-- SHA-256("Password@123" + salt): a0d8b3c4e2f17890bcde1234567890abcdef1234567890abcdef1234567890ab (sample)
INSERT INTO Users (name, email, password_hash, salt, role, phone, organization_name, address)
VALUES (
    'Green Garden Bistro',
    'bistro@greengarden.com',
    'ff174ba62abc336deb988df5606f21e2fa634f2faf8cbff8add3371b38e54665',
    'a1b2c3d4e5f60718293a4b5c6d7e8f90',
    'Donor',
    '9876543210',
    'Green Garden Hospitality LLC',
    '124 Culinary Boulevard, Downtown'
) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);

INSERT INTO Donors (user_id, donor_type, total_donations_count)
VALUES (LAST_INSERT_ID(), 'Restaurant', 24)
ON DUPLICATE KEY UPDATE donor_type=VALUES(donor_type);

-- Seed Hope Food Foundation detail record in NGOs table
INSERT INTO NGOs (user_id, registration_number, service_area, active_requests_count, beneficiaries_served)
SELECT id, 'NGO-US-2024-8849', 'Greater Metro & Downtown Shelters', 4, 1250
FROM Users WHERE email = 'hope.foundation@ngo.org'
ON DUPLICATE KEY UPDATE registration_number=VALUES(registration_number);

-- Seed Alex Green detail record in Volunteers table
INSERT INTO Volunteers (user_id, vehicle_type, availability, completed_deliveries, hours_logged)
SELECT id, 'Car', 'Flexible (Evenings & Weekends)', 18, 42.5
FROM Users WHERE email = 'alex.green@volunteer.org'
ON DUPLICATE KEY UPDATE vehicle_type=VALUES(vehicle_type);
