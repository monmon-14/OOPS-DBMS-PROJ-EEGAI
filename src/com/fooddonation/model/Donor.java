package com.fooddonation.model;

/**
 * Concrete model class representing a Food Donor (Restaurant, Hotel, or Individual).
 * Extends the abstract User class with donor-specific attributes.
 */
public class Donor extends User {
    // Donor Classification Types (English)
    public static final String TYPE_RESTAURANT = "Restaurant";
    public static final String TYPE_MESS = "Traditional Mess";
    public static final String TYPE_WEDDING_HALL = "Wedding Hall";
    public static final String TYPE_TEMPLE_ANNADHANAM = "Temple Annadhanam";
    public static final String TYPE_CATERING = "Catering";
    public static final String TYPE_HOUSEHOLD = "Household";

    public static final String[] BILINGUAL_DONOR_TYPES = {
            TYPE_RESTAURANT,
            TYPE_MESS,
            TYPE_WEDDING_HALL,
            TYPE_TEMPLE_ANNADHANAM,
            TYPE_CATERING,
            TYPE_HOUSEHOLD
    };

    private String donorType; // e.g., "உணவகம் (Restaurant)", "மெஸ் (Mess)", etc.
    private int totalDonationsCount;
    private boolean isDemo = false; // Flag indicating fictional Tamil culture demo donor

    public Donor() {
        super();
        this.role = "Donor";
        this.donorType = TYPE_RESTAURANT;
    }

    public Donor(int id, String name, String email, String passwordHash, String salt, 
                 String phone, String organizationName, String address, String donorType) {
        super(id, name, email, passwordHash, salt, "Donor", phone, organizationName, address);
        this.donorType = (donorType != null) ? donorType : TYPE_RESTAURANT;
        this.totalDonationsCount = 0;
        this.isDemo = false;
    }

    public Donor(int id, String name, String email, String passwordHash, String salt, 
                 String phone, String organizationName, String address, String donorType, boolean isDemo) {
        super(id, name, email, passwordHash, salt, "Donor", phone, organizationName, address);
        this.donorType = (donorType != null) ? donorType : TYPE_RESTAURANT;
        this.totalDonationsCount = 0;
        this.isDemo = isDemo;
    }

    @Override
    public String getRoleBadge() {
        String badge = "Verified Food Donor (" + donorType + ")";
        if (isDemo) {
            badge += " [Demo]";
        }
        return badge;
    }

    public String getDemoBadge() {
        return isDemo ? "Demo" : "Verified";
    }

    public boolean isDemo() {
        return isDemo;
    }

    public void setDemo(boolean demo) {
        isDemo = demo;
    }

    @Override
    public String getRoleDescription() {
        return "Commercial kitchen or individual donating surplus meals to prevent hunger and food waste.";
    }

    @Override
    public String getDashboardGreeting() {
        return "Thank you for sharing meals, " + getName() + "!";
    }

    // OOP Concept: Method Overriding (Polymorphic dashboard launcher)
    @Override
    public void showDashboard() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                com.fooddonation.ui.DonorDashboard dashboard = new com.fooddonation.ui.DonorDashboard();
                dashboard.setVisible(true);
            } catch (Exception e) {
                System.err.println("[Donor] Error launching dashboard: " + e.getMessage());
            }
        });
    }

    public String getDonorType() {
        return donorType;
    }

    public void setDonorType(String donorType) {
        this.donorType = donorType;
    }

    public int getTotalDonationsCount() {
        return totalDonationsCount;
    }

    public void setTotalDonationsCount(int totalDonationsCount) {
        this.totalDonationsCount = totalDonationsCount;
    }
}
