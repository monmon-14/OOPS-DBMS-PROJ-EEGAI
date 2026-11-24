-- ====================================================================
-- Food Donation System - Donations Table Schema & Stats Queries
-- Target: MySQL 8.0+ / 9.x
-- ====================================================================

USE food_donation_db;

-- --------------------------------------------------------------------
-- Table structure for `Donations`
-- Tracks surplus food listings from creation to final delivery
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Donations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    donor_id INT NOT NULL,
    food_name VARCHAR(150) NOT NULL,
    category ENUM('Veg', 'Non-Veg') NOT NULL,
    quantity INT NOT NULL COMMENT 'Number of meals',
    prep_time DATETIME NOT NULL,
    expiry_time DATETIME NOT NULL,
    pickup_address VARCHAR(255) NOT NULL,
    status ENUM('Available', 'Claimed', 'Picked Up', 'Delivered', 'Expired', 'Cancelled') 
        NOT NULL DEFAULT 'Available',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_donation_donor FOREIGN KEY (donor_id) 
        REFERENCES Users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_donation_donor (donor_id),
    INDEX idx_donation_status (status),
    INDEX idx_donation_expiry (expiry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- Seed Demonstration Donations for Demo Donor (donor_id = 1, Green Garden Bistro)
-- --------------------------------------------------------------------

INSERT INTO Donations (donor_id, food_name, category, quantity, prep_time, expiry_time, pickup_address, status, notes)
VALUES 
(
    1, 
    'Fresh Artisan Bread & Baguettes', 
    'Veg', 
    30, 
    DATE_SUB(NOW(), INTERVAL 2 HOUR), 
    DATE_ADD(NOW(), INTERVAL 6 HOUR), 
    '124 Culinary Boulevard, Downtown', 
    'Available', 
    'Freshly baked artisan loaves. Packed in food-grade kraft paper.'
),
(
    1, 
    'Vegetable Pasta & Marinara Sauce', 
    'Veg', 
    50, 
    DATE_SUB(NOW(), INTERVAL 1 HOUR), 
    DATE_ADD(NOW(), INTERVAL 4 HOUR), 
    '124 Culinary Boulevard, Downtown', 
    'Available', 
    'Warm portions in aluminum trays. Ready for immediate pickup.'
),
(
    1, 
    'Grilled Herb Chicken & Rice Bowls', 
    'Non-Veg', 
    40, 
    DATE_SUB(NOW(), INTERVAL 5 HOUR), 
    DATE_ADD(NOW(), INTERVAL 1 HOUR), 
    '124 Culinary Boulevard, Downtown', 
    'Claimed', 
    'Claimed by Hope Food Foundation. Volunteer courier en route.'
),
(
    1, 
    'Roasted Vegetable Medley', 
    'Veg', 
    60, 
    DATE_SUB(NOW(), INTERVAL 1 DAY), 
    DATE_SUB(NOW(), INTERVAL 18 HOUR), 
    '124 Culinary Boulevard, Downtown', 
    'Delivered', 
    'Successfully distributed at Downtown Community Kitchen.'
),
(
    1, 
    'Garden Fresh Green Salad', 
    'Veg', 
    25, 
    DATE_SUB(NOW(), INTERVAL 2 DAY), 
    DATE_SUB(NOW(), INTERVAL 1 DAY), 
    '124 Culinary Boulevard, Downtown', 
    'Expired', 
    'Past safe shelf-life threshold. Auto-expired.'
);

-- --------------------------------------------------------------------
-- SQL Queries for Donor Analytics & Statistics
-- --------------------------------------------------------------------

-- Query 1: Real-time Donor Statistics KPI summary
SELECT 
    COUNT(*) AS total_listed,
    COUNT(CASE WHEN status = 'Claimed' THEN 1 END) AS total_claimed,
    COUNT(CASE WHEN status = 'Delivered' THEN 1 END) AS total_delivered,
    COALESCE(SUM(CASE WHEN status = 'Delivered' THEN quantity ELSE 0 END), 0) AS meals_donated
FROM Donations
WHERE donor_id = 1;

-- Query 2: Active listings eligible for claiming by NGOs
SELECT id, food_name, category, quantity, prep_time, expiry_time, pickup_address, status
FROM Donations
WHERE donor_id = 1 AND status = 'Available'
ORDER BY expiry_time ASC;

-- Query 3: Auto-expiry check query
UPDATE Donations 
SET status = 'Expired' 
WHERE status = 'Available' AND expiry_time <= NOW();
