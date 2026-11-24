package com.fooddonation.model;

import java.awt.Color;

/**
 * Enumeration representing the progressive lifecycle stages of food courier delivery.
 * Advances sequentially: Accepted -> Picked Up -> Delivered.
 */
public enum DeliveryStatus {
    ACCEPTED("Accepted", 0, new Color(0xE8, 0xEF, 0xE8), new Color(0x4E, 0x65, 0x4B), "", "Courier assigned & en route to kitchen"),
    PICKED_UP("Picked Up", 1, new Color(0xFB, 0xF0, 0xDE), new Color(0x8C, 0x53, 0x19), "", "Meals collected & in transit to shelter"),
    DELIVERED("Delivered", 2, new Color(0xDF, 0xEB, 0xDE), new Color(0x3B, 0x52, 0x38), "", "Safely delivered & distributed to community");

    private final String displayName;
    private final int stepIndex;
    private final Color badgeBgColor;
    private final Color badgeTextColor;
    private final String icon;
    private final String description;

    DeliveryStatus(String displayName, int stepIndex, Color badgeBgColor, Color badgeTextColor, String icon, String description) {
        this.displayName = displayName;
        this.stepIndex = stepIndex;
        this.badgeBgColor = badgeBgColor;
        this.badgeTextColor = badgeTextColor;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public Color getBadgeBgColor() {
        return badgeBgColor;
    }

    public Color getBadgeTextColor() {
        return badgeTextColor;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determines the next sequential step in the delivery pipeline.
     * Returns null if already at the final Delivered stage.
     */
    public DeliveryStatus getNextStatus() {
        switch (this) {
            case ACCEPTED:
                return PICKED_UP;
            case PICKED_UP:
                return DELIVERED;
            case DELIVERED:
            default:
                return null;
        }
    }

    public String getAdvanceButtonText() {
        switch (this) {
            case ACCEPTED:
                return "Advance: Mark Picked Up";
            case PICKED_UP:
                return "Advance: Mark Delivered";
            case DELIVERED:
            default:
                return "Delivery Completed";
        }
    }

    public static DeliveryStatus fromString(String text) {
        if (text == null) return ACCEPTED;
        for (DeliveryStatus s : DeliveryStatus.values()) {
            if (s.displayName.equalsIgnoreCase(text.trim()) || s.name().equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        return ACCEPTED;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
