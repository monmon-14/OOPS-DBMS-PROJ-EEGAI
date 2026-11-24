-- ====================================================================
-- ஈகை (Eegai) - NGO Partner Seeding & Tamil Culture Donor Script
-- File: seed_ngos.sql
-- ====================================================================

USE food_donation_db;

-- 1. Ensure partner_status, cause, and website columns exist on NGOs table
ALTER TABLE NGOs 
    ADD COLUMN IF NOT EXISTS partner_status ENUM('LISTED', 'PARTNERED') DEFAULT 'LISTED',
    ADD COLUMN IF NOT EXISTS cause VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS website VARCHAR(255) NULL;

-- Ensure Users table allows NULL for phone, address when listed from public records
ALTER TABLE Users
    MODIFY COLUMN phone VARCHAR(20) NULL,
    MODIFY COLUMN address VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS is_demo BOOLEAN DEFAULT FALSE;

-- Ensure Donors table has is_demo column
ALTER TABLE Donors
    ADD COLUMN IF NOT EXISTS is_demo BOOLEAN DEFAULT FALSE,
    MODIFY COLUMN donor_type VARCHAR(100) NOT NULL;

-- Ensure FoodDonations table supports occasion and is_demo
ALTER TABLE food_donations
    ADD COLUMN IF NOT EXISTS occasion VARCHAR(100) NULL DEFAULT 'None',
    ADD COLUMN IF NOT EXISTS is_demo BOOLEAN DEFAULT FALSE;

-- --------------------------------------------------------------------
-- 2. Seed Public Listed NGOs (name, cause, website only; phone/email/address NULL)
-- --------------------------------------------------------------------
INSERT INTO Users (id, name, email, password_hash, salt, role, phone, organization_name, address, is_demo)
VALUES 
    (101, 'Robin Hood Army', NULL, 'N/A', 'N/A', 'NGO', NULL, 'Robin Hood Army', NULL, FALSE),
    (102, 'Feeding India', NULL, 'N/A', 'N/A', 'NGO', NULL, 'Feeding India (Zomato Feeding India)', NULL, FALSE),
    (103, 'The Akshaya Patra Foundation', NULL, 'N/A', 'N/A', 'NGO', NULL, 'The Akshaya Patra Foundation', NULL, FALSE),
    (104, 'Udhavum Karangal', NULL, 'N/A', 'N/A', 'NGO', NULL, 'Udhavum Karangal (Helping Hands)', NULL, FALSE),
    (105, 'HelpAge India', NULL, 'N/A', 'N/A', 'NGO', NULL, 'HelpAge India', NULL, FALSE),
    (106, 'Goonj', NULL, 'N/A', 'N/A', 'NGO', NULL, 'Goonj India', NULL, FALSE),
    (107, 'Smile Foundation', NULL, 'N/A', 'N/A', 'NGO', NULL, 'Smile Foundation India', NULL, FALSE)
ON DUPLICATE KEY UPDATE name = VALUES(name), organization_name = VALUES(organization_name);

INSERT INTO NGOs (user_id, registration_number, service_area, active_requests_count, beneficiaries_served, partner_status, cause, website)
VALUES
    (101, 'RHA-IN-01', 'Pan-India / Metro Hubs', 0, 0, 'LISTED', 'Zero-funds volunteer organization serving surplus food from restaurants to the hungry.', 'https://robinhoodarmy.com'),
    (102, 'FI-IN-02', 'Delhi, Chennai, Mumbai, Tier-1 Cities', 0, 0, 'LISTED', 'Eradicating hunger and malnutrition through structured food rescue and daily meals.', 'https://www.feedingindia.org'),
    (103, 'AP-IN-03', 'Chennai, Madurai, Pan-India Schools', 0, 0, 'LISTED', 'Mid-Day Meal program provider combating classroom hunger with nutritious hot meals.', 'https://www.akshayapatra.org'),
    (104, 'UK-TN-04', 'Chennai, Thiruvanmiyur, Coimbatore', 0, 0, 'LISTED', 'Sheltering and feeding abandoned elders, destitute children, and mentally challenged.', 'https://www.udhavumkarangal.org'),
    (105, 'HI-IN-05', 'Chennai, Tiruchirappalli, Salem', 0, 0, 'LISTED', 'Elderly care, nutrition, healthcare, and humanitarian disaster relief for seniors.', 'https://www.helpageindia.org'),
    (106, 'GJ-IN-06', 'Rural Tamil Nadu & Disaster Relief Hubs', 0, 0, 'LISTED', 'Transforming urban surplus and bridging rural food & essentials deficit sustainably.', 'https://goonj.org'),
    (107, 'SF-IN-07', 'Chennai, Kanchipuram, Villupuram', 0, 0, 'LISTED', 'Promoting child education, nutrition, health, and women empowerment.', 'https://www.smilefoundationindia.org')
ON DUPLICATE KEY UPDATE 
    partner_status = VALUES(partner_status),
    cause = VALUES(cause),
    website = VALUES(website);

-- --------------------------------------------------------------------
-- 3. Seed Fictional Demo Donors with Tamil Culture Bilingual Labels (is_demo = TRUE)
-- --------------------------------------------------------------------
-- Password: Password@123 (Hash/Salt precomputed for demo logins)
INSERT INTO Users (id, name, email, password_hash, salt, role, phone, organization_name, address, is_demo)
VALUES
    (201, 'Annam Mess', 'annam.mess@demo.eegai.org', 'b7f08d32f5fcf0cfd8e578c773950bfae7e6514bc1223594fa35a4d95a127a6e', 'salt_eegai_demo_01', 'Donor', '9840112233', 'அன்னம் மெஸ் (Annam Mess)', 'Mylapore, Chennai', TRUE),
    (202, 'Thendral Catering', 'thendral.caterers@demo.eegai.org', 'b7f08d32f5fcf0cfd8e578c773950bfae7e6514bc1223594fa35a4d95a127a6e', 'salt_eegai_demo_02', 'Donor', '9840223344', 'தென்றல் கேட்டரிங் (Thendral Catering)', 'KK Nagar, Madurai', TRUE),
    (203, 'Pongal Mandapam', 'pongal.mandapam@demo.eegai.org', 'b7f08d32f5fcf0cfd8e578c773950bfae7e6514bc1223594fa35a4d95a127a6e', 'salt_eegai_demo_03', 'Donor', '9840334455', 'பொங்கல் திருமண மண்டபம் (Pongal Mandapam)', 'Gandhipuram, Coimbatore', TRUE),
    (204, 'Kapaleeshwarar Annadhanam Trust', 'annadhanam.kapali@demo.eegai.org', 'b7f08d32f5fcf0cfd8e578c773950bfae7e6514bc1223594fa35a4d95a127a6e', 'salt_eegai_demo_04', 'Donor', '9840445566', 'கோவில் அன்னதானம் (Temple Annadhanam Trust)', 'Mylapore Tank, Chennai', TRUE),
    (205, 'Amudham Traditional Restaurant', 'amudham.veg@demo.eegai.org', 'b7f08d32f5fcf0cfd8e578c773950bfae7e6514bc1223594fa35a4d95a127a6e', 'salt_eegai_demo_05', 'Donor', '9840556677', 'அமுதம் உணவகம் (Amudham Restaurant)', 'Thillai Nagar, Tiruchirappalli', TRUE),
    (206, 'Sundaram Kudumbam', 'sundaram.home@demo.eegai.org', 'b7f08d32f5fcf0cfd8e578c773950bfae7e6514bc1223594fa35a4d95a127a6e', 'salt_eegai_demo_06', 'Donor', '9840667788', 'சுந்தரம் இல்லம் (Sundaram Household)', 'R.S. Puram, Coimbatore', TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name), organization_name = VALUES(organization_name);

INSERT INTO Donors (user_id, donor_type, total_donations_count, is_demo)
VALUES
    (201, 'மெஸ் (Mess)', 14, TRUE),
    (202, 'கேட்டரிங் (Catering)', 22, TRUE),
    (203, 'திருமண மண்டபம் (Wedding Hall)', 35, TRUE),
    (204, 'கோவில் அன்னதானம் (Temple Annadhanam)', 50, TRUE),
    (205, 'உணவகம் (Restaurant)', 19, TRUE),
    (206, 'வீடு (Household)', 6, TRUE)
ON DUPLICATE KEY UPDATE 
    donor_type = VALUES(donor_type),
    is_demo = VALUES(is_demo);
