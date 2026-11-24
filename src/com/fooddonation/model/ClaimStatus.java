package com.fooddonation.model;

import java.awt.Color;

/**
 * Enumeration representing the fulfillment status of an NGO Claim.
 */
public enum ClaimStatus {
    CLAIMED("Claimed", new Color(0xFB, 0xF0, 0xDE), new Color(0x8C, 0x53, 0x19)),              // Warm Cream Amber
    ASSIGNED("Volunteer Assigned", new Color(0xE8, 0xEF, 0xE8), new Color(0x4E, 0x65, 0x4B)),  // Pastel Sage
    PICKED_UP("Picked Up", new Color(0xEE, 0xE7, 0xDB), new Color(0x5A, 0x47, 0x3E)),          // Warm Beige
    DELIVERED("Delivered", new Color(0xDF, 0xEB, 0xDE), new Color(0x3B, 0x52, 0x38)),          // Muted Sage Green
    CANCELLED("Cancelled", new Color(0xF9, 0xEB, 0xEF), new Color(0x7E, 0x1E, 0x34));          // Pastel Burgundy

    private final String displayName;
    private final Color badgeBgColor;
    private final Color badgeTextColor;

    ClaimStatus(String displayName, Color badgeBgColor, Color badgeTextColor) {
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

    public static ClaimStatus fromString(String text) {
        if (text == null) return CLAIMED;
        for (ClaimStatus s : ClaimStatus.values()) {
            if (s.displayName.equalsIgnoreCase(text.trim()) || s.name().equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        return CLAIMED;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
