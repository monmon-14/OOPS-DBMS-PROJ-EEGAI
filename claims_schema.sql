-- ====================================================================
-- Food Donation System - Claims Table Schema & Concurrency Scripts
-- Links Donated Food to NGOs and Assigned Volunteer Couriers
-- ====================================================================

USE food_donation_db;

-- --------------------------------------------------------------------
-- Table structure for `Claims`
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Claims (
    id INT AUTO_INCREMENT PRIMARY KEY,
    donation_id INT NOT NULL,
    ngo_id INT NOT NULL,
    volunteer_id INT DEFAULT NULL,
    status ENUM('Claimed', 'Volunteer Assigned', 'Picked Up', 'Delivered', 'Cancelled') 
        NOT NULL DEFAULT 'Claimed',
    claimed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_claim_donation FOREIGN KEY (donation_id) 
        REFERENCES Donations(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_claim_ngo FOREIGN KEY (ngo_id) 
        REFERENCES Users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_claim_volunteer FOREIGN KEY (volunteer_id) 
        REFERENCES Users(id) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_claim_donation (donation_id),
    INDEX idx_claim_ngo (ngo_id),
    INDEX idx_claim_volunteer (volunteer_id),
    INDEX idx_claim_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- Seed Demonstration Claims
-- --------------------------------------------------------------------

-- 1. Claimed by NGO 2 (Hope Food Foundation), no volunteer assigned yet (Available for Volunteer Pickup!)
INSERT INTO Claims (id, donation_id, ngo_id, volunteer_id, status, claimed_at)
VALUES (1, 3, 2, NULL, 'Claimed', DATE_SUB(NOW(), INTERVAL 2 HOUR))
ON DUPLICATE KEY UPDATE status=VALUES(status);

-- 2. Claimed by NGO 2, assigned to Volunteer 3 (Alex Green), delivered
INSERT INTO Claims (id, donation_id, ngo_id, volunteer_id, status, claimed_at)
VALUES (2, 4, 2, 3, 'Delivered', DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE status=VALUES(status);

-- --------------------------------------------------------------------
-- Concurrency-Safe Claim Transaction (Prevents Double-Claims & Expired Claims)
-- --------------------------------------------------------------------
/*
START TRANSACTION;

-- Lock and verify donation status
SELECT id, status, expiry_time 
FROM Donations 
WHERE id = ? AND status = 'Available' AND expiry_time > NOW() 
FOR UPDATE;

-- Atomic update: succeeds ONLY if status is still 'Available'
UPDATE Donations 
SET status = 'Claimed' 
WHERE id = ? AND status = 'Available' AND expiry_time > NOW();

-- Insert claim row
INSERT INTO Claims (donation_id, ngo_id, status, claimed_at) 
VALUES (?, ?, 'Claimed', NOW());

COMMIT;
*/
