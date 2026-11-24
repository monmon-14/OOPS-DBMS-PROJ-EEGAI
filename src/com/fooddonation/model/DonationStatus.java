package com.fooddonation.model;

import java.awt.Color;

/**
 * Enumeration representing the operational lifecycle of a Food Donation.
 */
public enum DonationStatus {
    AVAILABLE("Available", new Color(0xE8, 0xEF, 0xE8), new Color(0x4E, 0x65, 0x4B)),   // Pastel Sage
    CLAIMED("Claimed", new Color(0xFB, 0xF0, 0xDE), new Color(0x8C, 0x53, 0x19)),       // Warm Cream Amber
    PICKED_UP("Picked Up", new Color(0xEE, 0xE7, 0xDB), new Color(0x5A, 0x47, 0x3E)),   // Warm Cream Beige
    DELIVERED("Delivered", new Color(0xDF, 0xEB, 0xDE), new Color(0x3B, 0x52, 0x38)),   // Muted Sage Green
    EXPIRED("Expired", new Color(0xEE, 0xE9, 0xE2), new Color(0x7A, 0x71, 0x68)),       // Soft Warm Taupe
    CANCELLED("Cancelled", new Color(0xF9, 0xEB, 0xEF), new Color(0x7E, 0x1E, 0x34));   // Pastel Burgundy

    private final String displayName;
    private final Color badgeBgColor;
    private final Color badgeTextColor;

    DonationStatus(String displayName, Color badgeBgColor, Color badgeTextColor) {
        this.displayName = displayName;
        this.badgeBgColor = badgeBgColor;
        this.badgeTextColor = badgeTextColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getBadgeBgColor() {
        return badgeBgColor;
    }

    public Color getBadgeTextColor() {
        return badgeTextColor;
    }

    public static DonationStatus fromString(String text) {
        if (text == null) return AVAILABLE;
        for (DonationStatus s : DonationStatus.values()) {
            if (s.displayName.equalsIgnoreCase(text.trim()) || s.name().equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        return AVAILABLE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
