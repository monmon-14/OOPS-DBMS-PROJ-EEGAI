package com.fooddonation.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Utility class for cryptographic salt generation and SHA-256 password hashing.
 */
public class PasswordUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a 16-byte cryptographically secure random salt formatted as a 32-character hex string.
     */
    public static String generateSalt() {
        byte[] saltBytes = new byte[16];
        SECURE_RANDOM.nextBytes(saltBytes);
        return bytesToHex(saltBytes);
    }

    /**
     * Computes the SHA-256 hash of (rawPassword + salt).
     *
     * @param rawPassword The clear-text password
     * @param salt        The random hex salt string
     * @return 64-character lowercase hex SHA-256 hash
     */
    public static String hashPassword(String rawPassword, String salt) {
        if (rawPassword == null || salt == null) {
            throw new IllegalArgumentException("Password and salt cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = rawPassword + salt;
            byte[] encodedhash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in current JVM", e);
        }
    }

    /**
     * Verifies rawPassword against expected stored hash using the user's salt in constant time.
     */
    public static boolean verifyPassword(String rawPassword, String salt, String expectedHash) {
        if (rawPassword == null || salt == null || expectedHash == null) {
            return false;
        }
        String computedHash = hashPassword(rawPassword, salt);
        return MessageDigest.isEqual(
                computedHash.getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Helper to convert raw byte array into lowercase hexadecimal string representation.
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
