package com.fooddonation.util;

import java.util.regex.Pattern;

/**
 * Utility class encapsulating reusable string operations for validation and search.
 *
 * OOP Concept: Utility Class & Encapsulation (Static helper methods with private constructor)
 */
public final class StringUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$"
    );

    // Private constructor prevents instantiation
    private StringUtil() {
    }

    /**
     * OOP Concept: String Utilities (Regular expression email validation)
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * OOP Concept: String Utilities (Password strength validation)
     * Requirements: at least 8 characters, at least 1 uppercase, 1 lowercase, 1 digit, and 1 special char.
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isWhitespace(c)) hasSpecial = true;
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * OOP Concept: String Utilities (Case-insensitive city search / containment matching)
     */
    public static boolean containsCityIgnoreCase(String address, String city) {
        if (address == null || city == null) return false;
        String cleanCity = city.trim().toLowerCase();
        if (cleanCity.isEmpty()) return true;
        return address.toLowerCase().contains(cleanCity);
    }
}
