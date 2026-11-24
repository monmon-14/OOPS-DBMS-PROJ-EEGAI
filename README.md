# Project Architecture & Presentation Guide: ஈகை (Eegai) - Food Donation & Rescue System

This guide presents the entire codebase structured by architectural layers (**Backend Core & OOP**, **DBMS & Data Access Layer**, and **Frontend UI**), complete with detailed annotations explaining every **Object-Oriented Programming (OOP)** and **Database Management Systems (DBMS)** concept used so that you can explain it with confidence during your project presentation.

---

## 🏛️ PART 1: SYSTEM ARCHITECTURE OVERVIEW

```
+---------------------------------------------------------------------------------------+
|                                    PRESENTATION LAYER (FRONTEND)                      |
|   RoleSelectionFrame  |  DonorDashboard  |  NGODashboard  |  VolunteerDashboard       |
|   RegisterFrame       |  LoginFrame      |  ReportsPanel  |  DeliveryTrackingPanel    |
+---------------------------------------------------------------------------------------+
                                           │
                                  Calls Service API
                                           ▼
+---------------------------------------------------------------------------------------+
|                                     BUSINESS LOGIC LAYER                              |
|   AuthService         |  DonationService |  DeliveryService |  ReportService          |
|   RegistrationService |  UserSession     |  PasswordUtil (PBKDF2/SHA-256)             |
+---------------------------------------------------------------------------------------+
                                           │
                                    Passes Domain Models
                                           ▼
+---------------------------------------------------------------------------------------+
|                                DOMAIN MODELS (OOP CONCEPTS)                           |
|   Abstract User (Base) ◄─── Donor (Child) / NGO (Child) / Volunteer (Child)           |
|   FoodDonation / Delivery (implements Trackable) | Claim | Enums (Type Safety)        |
+---------------------------------------------------------------------------------------+
                                           │
                                 Calls DAO Methods
                                           ▼
+---------------------------------------------------------------------------------------+
|                                DATA ACCESS LAYER (DBMS / JDBC)                        |
|   UserDAO             |  DonationDAO     |  ClaimDAO      |  DeliveryDAO / ReportDAO  |
|   PreparedStatements  |  ACID Transaction Control (conn.setAutoCommit(false))         |
+---------------------------------------------------------------------------------------+
                                           │
                                Executes SQL Queries
                                           ▼
+---------------------------------------------------------------------------------------+
|                                 DATABASE (MySQL 8.0+ / InnoDB)                        |
|   Tables: Users, Donations, Claims, Deliveries, Partners (3NF + Foreign Keys + B-Trees)|
+---------------------------------------------------------------------------------------+
```

---

## 💡 PART 2: OOP (OBJECT-ORIENTED PROGRAMMING) TOPICS USED

Here is how the 4 pillars of OOP plus advanced concepts are implemented:

| OOP Concept | Where it is Implemented in the Project | Why / Presentation Explanation |
| :--- | :--- | :--- |
| **1. Abstraction** | `abstract class User`, `abstract void showDashboard()`, `interface Trackable` | Hides implementation details and forces all user roles (Donor, NGO, Volunteer) to provide their own role badge, greeting, and dashboard. |
| **2. Encapsulation** | All models (`User`, `FoodDonation`, `Claim`, `Delivery`) | Fields are `private` or `protected`; access is restricted through validated getters, setters, and business methods (e.g. `isExpired()`, `canBeCancelled()`). |
| **3. Inheritance** | `Donor extends User`, `NGO extends User`, `Volunteer extends User`, Swing UI extending `JFrame` / `JPanel` | Promotes code reusability. Common fields like `id`, `name`, `email`, `phone`, `passwordHash` are inherited from `User`. |
| **4. Polymorphism (Runtime)** | `User.showDashboard()`, `Trackable.getStatus()`, `UserSession.getCurrentUser()` | A variable of type `User` points to a `Donor`, `NGO`, or `Volunteer` at runtime, automatically invoking the child class's overridden method (`dynamic method dispatch`). |
| **5. Polymorphism (Compile-time)** | Constructor overloading in `Donor`, `FoodDonation`, `Delivery`, method overloading in DAOs | Multiple constructors allowing creation with or without IDs, timestamps, or demo flags. |
| **6. Interfaces & Default Methods** | `public interface Trackable` | Defines a contract for entities progressing through lifecycle milestones (`getStatus()`, `updateStatus()`), with `default String getStatusAsString()`. |
| **7. Type Safety via Enums** | `DonationStatus`, `ClaimStatus`, `DeliveryStatus` | Prevents invalid status strings (like misspelling "Availble"), binding step indices, colors, and badge descriptions directly to constants. |

---

## 🗄️ PART 3: DBMS (DATABASE MANAGEMENT SYSTEMS) TOPICS USED

| DBMS Concept | Where it is Implemented in the Project | Why / Presentation Explanation |
| :--- | :--- | :--- |
| **1. Relational Schema & 3NF** | `Users`, `Donations`, `Claims`, `Deliveries` | Normalized to Third Normal Form (3NF) to eliminate data redundancy and insertion/deletion anomalies. |
| **2. Primary & Foreign Keys** | `Users(id)` -> `Donations(donor_id)` -> `Claims(donation_id, ngo_id, volunteer_id)` -> `Deliveries(claim_id)` | Maintains Referential Integrity with `ON DELETE CASCADE` and `ON UPDATE CASCADE`. |
| **3. ACID Transactions** | `ClaimDAO.createClaim()`, `DeliveryDAO.updateStatus()` | Atomicity & Consistency: `conn.setAutoCommit(false)` groups status checks and record insertion; calls `conn.commit()` on success or `conn.rollback()` on error. |
| **4. Concurrency & Double-Claim Prevention** | `SELECT ... FOR UPDATE` & conditional SQL `UPDATE Donations SET status='Claimed' WHERE id=? AND status='Available'` | Prevents race conditions when two NGOs attempt to claim the exact same food listing simultaneously. |
| **5. SQL Injection Prevention** | All DAOs (`UserDAO`, `DonationDAO`, `ClaimDAO`, `DeliveryDAO`, `ReportDAO`) | 100% of queries use parameterized `PreparedStatement` (`?` placeholders) instead of string concatenation. |
| **6. B-Tree Indexing** | `CREATE INDEX idx_donation_status ON Donations(status)` | Dramatically accelerates query response times for frequent searches (`status = 'Available'`, `donor_id = ?`, `expiry_time`). |
| **7. Aggregate Functions & Reporting** | `ReportDAO`, `DonationDAO` | Uses SQL aggregates: `COUNT(*)`, `SUM()`, `CASE WHEN`, `DATE_SUB()`, `GROUP BY`, and `HAVING`. |
| **8. Fault Tolerance / In-Memory Fallback** | `CopyOnWriteArrayList` in all DAOs | The application gracefully falls back to thread-safe local data if MySQL is not running on the evaluator's machine. |

---


   
