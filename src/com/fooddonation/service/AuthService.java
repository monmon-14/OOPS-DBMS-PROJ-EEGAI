package com.fooddonation.service;

import com.fooddonation.dao.UserDAO;
import com.fooddonation.exception.InvalidCredentialsException;
import com.fooddonation.model.NGO;
import com.fooddonation.model.User;
import com.fooddonation.model.Volunteer;
import com.fooddonation.util.PasswordUtil;
import com.fooddonation.util.StringUtil;

/**
 * Service layer responsible for user authentication, credential validation,
 * cryptographic verification, and session state initialization.
 */
public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Authenticates a user with email, raw password, and selected role.
     *
     * OOP Concept: Custom Exceptions (try/catch handling for InvalidCredentialsException)
     * OOP Concept: String Utilities (StringUtil.isValidEmail verification)
     *
     * @param email        Entered email
     * @param rawPassword  Entered plaintext password
     * @param selectedRole Selected toggle role ("NGO" or "Volunteer")
     * @return AuthResult containing status, error message, or authenticated User
     */
    public AuthResult login(String email, String rawPassword, String selectedRole) {
        try {
            // 1. Validate empty or blank inputs using StringUtil
            if (email == null || email.trim().isEmpty()) {
                throw new InvalidCredentialsException("Please enter your email address.");
            }
            if (!StringUtil.isValidEmail(email)) {
                throw new InvalidCredentialsException("Please enter a valid formatted email address.");
            }
            if (rawPassword == null || rawPassword.isEmpty()) {
                throw new InvalidCredentialsException("Please enter your password.");
            }
            if (selectedRole == null || selectedRole.trim().isEmpty()) {
                throw new InvalidCredentialsException("Please select your role (Donor, NGO, or Volunteer).");
            }

            String normalizedEmail = email.trim().toLowerCase();

            // 2. Query user from database via UserDAO (PreparedStatement)
            User user = userDAO.findByEmail(normalizedEmail);

            if (user == null) {
                throw new InvalidCredentialsException("No account registered with this email address.");
            }

            // 3. Verify password hash using stored salt (SHA-256 + salt)
            String storedSalt = user.getSalt();
            String storedHash = user.getPasswordHash();

            boolean isPasswordValid = PasswordUtil.verifyPassword(rawPassword, storedSalt, storedHash);
            if (!isPasswordValid) {
                throw new InvalidCredentialsException("Incorrect password. Please verify and try again.");
            }

            // 4. Verify role against Users table role column
            if (!user.getRole().equalsIgnoreCase(selectedRole.trim())) {
                throw new InvalidCredentialsException(String.format(
                        "Role mismatch: This email is registered as an '%s'. Please switch to the '%s' portal to log in.",
                        user.getRole(), user.getRole()
                ));
            }

            // 5. Success: Store authenticated User in singleton UserSession
            UserSession.getInstance().setCurrentUser(user);

            return AuthResult.success(user);

        } catch (InvalidCredentialsException e) {
            System.err.println("[AuthService] InvalidCredentialsException handled: " + e.getMessage());
            return AuthResult.failure(e.getMessage());
        } catch (Exception e) {
            System.err.println("[AuthService] Unexpected login error: " + e.getMessage());
            return AuthResult.failure("Login failed: " + e.getMessage());
        }
    }

    /**
     * Registers a new User with automatic salt generation and SHA-256 password hashing.
     */
    public AuthResult register(String name, String email, String password, String role, 
                               String phone, String orgName, String address) {
        if (name == null || name.trim().isEmpty()) {
            return AuthResult.failure("Name / Organization Name is required.");
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            return AuthResult.failure("Please provide a valid email address.");
        }
        if (password == null || password.length() < 6) {
            return AuthResult.failure("Password must be at least 6 characters long.");
        }
        if (role == null || (!role.equalsIgnoreCase("NGO") && !role.equalsIgnoreCase("Volunteer"))) {
            return AuthResult.failure("Invalid role selected.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userDAO.emailExists(normalizedEmail)) {
            return AuthResult.failure("An account with this email already exists.");
        }

        // Generate cryptographic salt and hash password
        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hashPassword(password, salt);

        User newUser;
        if ("NGO".equalsIgnoreCase(role)) {
            newUser = new NGO(0, name.trim(), normalizedEmail, passwordHash, salt, phone, orgName, address);
        } else {
            newUser = new Volunteer(0, name.trim(), normalizedEmail, passwordHash, salt, phone, orgName, address);
        }

        boolean saved = userDAO.insertUser(newUser, passwordHash, salt);
        if (saved) {
            return AuthResult.success(newUser);
        } else {
            return AuthResult.failure("Failed to save user account. Please check database connection.");
        }
    }

    /**
     * Data Transfer Object representing the outcome of an authentication attempt.
     */
    public static class AuthResult {
        private final boolean success;
        private final String errorMessage;
        private final User user;

        private AuthResult(boolean success, String errorMessage, User user) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.user = user;
        }

        public static AuthResult success(User user) {
            return new AuthResult(true, null, user);
        }

        public static AuthResult failure(String errorMessage) {
            return new AuthResult(false, errorMessage, null);
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
