-- ====================================================================
-- Food Donation System - Delivery Tracking & Locations Schema
-- Real-time courier progression, timestamps, and geographic locations
-- ====================================================================

USE food_donation_db;

-- --------------------------------------------------------------------
-- 1. Table structure for `Locations`
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Locations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    area VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_location_user FOREIGN KEY (user_id)
        REFERENCES Users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_location_user (user_id),
    INDEX idx_location_city (city),
    INDEX idx_location_area (area)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- 2. Table structure for `Deliveries`
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Deliveries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    claim_id INT NOT NULL,
    volunteer_id INT NOT NULL,
    status ENUM('Accepted', 'Picked Up', 'Delivered') NOT NULL DEFAULT 'Accepted',
    picked_at TIMESTAMP NULL DEFAULT NULL,
    delivered_at TIMESTAMP NULL DEFAULT NULL,
    meals_delivered INT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_claim FOREIGN KEY (claim_id)
        REFERENCES Claims(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_delivery_volunteer FOREIGN KEY (volunteer_id)
        REFERENCES Users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_delivery_claim (claim_id),
    INDEX idx_delivery_volunteer (volunteer_id),
    INDEX idx_delivery_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- Seed Geographic Locations
-- --------------------------------------------------------------------
INSERT INTO Locations (id, user_id, address, city, area)
VALUES 
    (1, 1, '124 Culinary Boulevard, Downtown', 'Seattle', 'Downtown Commercial District'),
    (2, 2, '450 Community Way, Suite 12', 'Seattle', 'Central Metro Shelter Hub'),
    (3, 3, '789 Pine Street', 'Seattle', 'Capitol Hill')
ON DUPLICATE KEY UPDATE address=VALUES(address), city=VALUES(city), area=VALUES(area);

-- --------------------------------------------------------------------
-- Seed Demonstration Deliveries
-- --------------------------------------------------------------------

-- Delivery 1: Historical delivered rescue for Claim 2 (Roasted Vegetable Medley)
INSERT INTO Deliveries (id, claim_id, volunteer_id, status, picked_at, delivered_at, meals_delivered, created_at)
VALUES (
    1, 
    2, 
    3, 
    'Delivered', 
    DATE_SUB(NOW(), INTERVAL 25 HOUR), 
    DATE_SUB(NOW(), INTERVAL 24 HOUR), 
    60, 
    DATE_SUB(NOW(), INTERVAL 26 HOUR)
)
ON DUPLICATE KEY UPDATE status=VALUES(status), meals_delivered=VALUES(meals_delivered);

-- --------------------------------------------------------------------
-- Transaction Script: Advance Delivery Status & Synchronize Tables
-- --------------------------------------------------------------------
/*
-- Advance to 'Picked Up':
START TRANSACTION;
UPDATE Deliveries SET status = 'Picked Up', picked_at = NOW() WHERE id = ? AND status = 'Accepted';
UPDATE Claims SET status = 'Picked Up' WHERE id = (SELECT claim_id FROM Deliveries WHERE id = ?);
UPDATE Donations SET status = 'Picked Up' WHERE id = (
    SELECT donation_id FROM Claims WHERE id = (SELECT claim_id FROM Deliveries WHERE id = ?)
);
COMMIT;

-- Advance to 'Delivered':
START TRANSACTION;
UPDATE Deliveries SET status = 'Delivered', delivered_at = NOW(), meals_delivered = ? WHERE id = ? AND status = 'Picked Up';
UPDATE Claims SET status = 'Delivered' WHERE id = (SELECT claim_id FROM Deliveries WHERE id = ?);
UPDATE Donations SET status = 'Delivered', quantity = ? WHERE id = (
    SELECT donation_id FROM Claims WHERE id = (SELECT claim_id FROM Deliveries WHERE id = ?)
);
COMMIT;
*/
