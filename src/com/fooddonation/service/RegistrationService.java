package com.fooddonation.service;

import com.fooddonation.dao.UserDAO;
import com.fooddonation.model.Donor;
import com.fooddonation.model.NGO;
import com.fooddonation.model.User;
import com.fooddonation.model.Volunteer;
import com.fooddonation.util.PasswordUtil;

import java.awt.Color;
import java.util.regex.Pattern;

/**
 * Service handling user registration, thorough multi-rule validation,
 * live password strength evaluation, cryptographic salt generation, and multi-table persistence.
 */
public class RegistrationService {

    private final UserDAO userDAO;

    // Validation Regex Patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern LETTER_PATTERN = Pattern.compile(".*[a-zA-Z].*");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

    public RegistrationService() {
        this.userDAO = new UserDAO();
    }

    public RegistrationService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Comprehensive registration method for all 3 roles.
     *
     * @param role              "Donor", "NGO", or "Volunteer"
     * @param name              Full Name or Organization Name
     * @param email             Email Address
     * @param phone             10-digit Phone Number
     * @param address           City or Physical Address
     * @param password          Raw Password
     * @param confirmPassword   Password confirmation
     * @param donorType         Specific to Donor (Restaurant / Hotel / Individual)
     * @param regNumber         Specific to NGO (Registration / Tax ID)
     * @param serviceArea       Specific to NGO (Borough, City, or District)
     * @param vehicleType       Specific to Volunteer (None / Bike / Car)
     * @param availability      Specific to Volunteer (Flexible, Weekdays, etc.)
     * @return RegistrationResult indicating success or descriptive validation error
     */
    public RegistrationResult register(
            String role, String name, String email, String phone, String address,
            String password, String confirmPassword,
            String donorType, String regNumber, String serviceArea,
            String vehicleType, String availability) {

        // 1. Validate Common Fields - Not Empty
        if (role == null || role.trim().isEmpty()) {
            return RegistrationResult.failure("Please select an account role (Donor, NGO, or Volunteer).");
        }
        if (name == null || name.trim().isEmpty()) {
            return RegistrationResult.failure("Please enter your Full Name or Organization Name.");
        }
        if (email == null || email.trim().isEmpty()) {
            return RegistrationResult.failure("Please enter your email address.");
        }
        if (phone == null || phone.trim().isEmpty()) {
            return RegistrationResult.failure("Please enter your contact phone number.");
        }
        if (address == null || address.trim().isEmpty()) {
            return RegistrationResult.failure("Please enter your City or Address.");
        }
        if (password == null || password.isEmpty()) {
            return RegistrationResult.failure("Please enter a password.");
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return RegistrationResult.failure("Please confirm your password.");
        }

        // 2. Validate Email Regex Format (OOP Concept: String Utilities)
        String normalizedEmail = email.trim().toLowerCase();
        if (!com.fooddonation.util.StringUtil.isValidEmail(normalizedEmail)) {
            return RegistrationResult.failure("Invalid email format (e.g., user@example.com).");
        }

        // 3. Validate 10-Digit Phone
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() != 10) {
            return RegistrationResult.failure("Phone number must contain exactly 10 digits.");
        }

        // 4. Validate Password Requirements: ≥ 8 chars, at least one letter and at least one digit
        if (password.length() < 8) {
            return RegistrationResult.failure("Password must be at least 8 characters long.");
        }
        if (!LETTER_PATTERN.matcher(password).matches()) {
            return RegistrationResult.failure("Password must contain at least one letter (a-z, A-Z).");
        }
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            return RegistrationResult.failure("Password must contain at least one numeric digit (0-9).");
        }

        // 5. Validate Password Match
        if (!password.equals(confirmPassword)) {
            return RegistrationResult.failure("Passwords do not match. Please re-enter.");
        }

        // 6. Validate Unique Email in Database
        if (userDAO.emailExists(normalizedEmail)) {
            return RegistrationResult.failure("An account with email '" + normalizedEmail + "' already exists.");
        }

        // 7. Validate Role-Specific Fields
        String normalizedRole = role.trim();
        User newUser;

        if ("Donor".equalsIgnoreCase(normalizedRole)) {
            if (donorType == null || donorType.trim().isEmpty()) {
                return RegistrationResult.failure("Please select a Donor Type (Restaurant, Hotel, or Individual).");
            }
            newUser = new Donor(0, name.trim(), normalizedEmail, null, null, cleanPhone, name.trim(), address.trim(), donorType.trim());

        } else if ("NGO".equalsIgnoreCase(normalizedRole)) {
            if (regNumber == null || regNumber.trim().isEmpty()) {
                return RegistrationResult.failure("Please enter your official NGO Registration / Tax ID Number.");
            }
            if (serviceArea == null || serviceArea.trim().isEmpty()) {
                return RegistrationResult.failure("Please enter your primary Service Area / Community District.");
            }
            newUser = new NGO(0, name.trim(), normalizedEmail, null, null, cleanPhone, name.trim(), address.trim(), regNumber.trim(), serviceArea.trim());

        } else if ("Volunteer".equalsIgnoreCase(normalizedRole)) {
            if (vehicleType == null || vehicleType.trim().isEmpty()) {
                return RegistrationResult.failure("Please select your Vehicle Type (None, Bike, or Car).");
            }
            if (availability == null || availability.trim().isEmpty()) {
                return RegistrationResult.failure("Please specify your delivery availability.");
            }
            newUser = new Volunteer(0, name.trim(), normalizedEmail, null, null, cleanPhone, "Volunteer Responder", address.trim(), vehicleType.trim(), availability.trim());

        } else {
            return RegistrationResult.failure("Unrecognized role: " + role);
        }

        // 8. Generate Cryptographic Salt and Hash Password (SHA-256 + salt)
        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hashPassword(password, salt);

        // 9. Persist into Database via UserDAO (PreparedStatement transaction)
        boolean persisted = userDAO.insertUser(newUser, passwordHash, salt);
        if (!persisted) {
            return RegistrationResult.failure("Failed to save user account due to a database error. Please try again.");
        }

        return RegistrationResult.success(newUser);
    }

    /**
     * Evaluates live password strength on a 0-4 point scale.
     */
    public static PasswordStrength assessPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return new PasswordStrength(0, "Enter password", new Color(0x94, 0xA3, 0xB8));
        }

        int score = 0;
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.matches(".*[a-z].*") && password.matches(".*[A-Z].*")) score++;
        if (DIGIT_PATTERN.matcher(password).matches()) score++;
        if (SPECIAL_PATTERN.matcher(password).matches()) score++;

        // Map score to Strength Level
        if (score <= 1) {
            return new PasswordStrength(1, "Weak (Add letters & numbers)", new Color(0xEF, 0x44, 0x44)); // Red
        } else if (score == 2 || score == 3) {
            return new PasswordStrength(2, "Medium (Add symbols & uppercase)", new Color(0xF5, 0x9E, 0x0B)); // Amber
        } else if (score == 4) {
            return new PasswordStrength(3, "Strong password", new Color(0x10, 0xB9, 0x81)); // Fresh Emerald
        } else {
            return new PasswordStrength(4, "Very Strong & Secure", new Color(0x05, 0x96, 0x69)); // Deep Emerald
        }
    }

    /**
     * Representation of password strength evaluation.
     */
    public static class PasswordStrength {
        private final int level; // 0 to 4
        private final String label;
        private final Color color;

        public PasswordStrength(int level, String label, Color color) {
            this.level = level;
            this.label = label;
            this.color = color;
        }

        public int getLevel() {
            return level;
        }

        public String getLabel() {
            return label;
        }

        public Color getColor() {
            return color;
        }
    }

    /**
     * Outcome DTO of a registration attempt.
     */
    public static class RegistrationResult {
        private final boolean success;
        private final String errorMessage;
        private final User user;

        private RegistrationResult(boolean success, String errorMessage, User user) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.user = user;
        }

        public static RegistrationResult success(User user) {
            return new RegistrationResult(true, null, user);
        }

        public static RegistrationResult failure(String errorMessage) {
            return new RegistrationResult(false, errorMessage, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public User getUser() {
            return user;
        }
    }
}
