package com.fooddonation.model;

/**
 * Concrete model class representing a Non-Governmental Organization (NGO).
 * Extends the abstract User class with NGO-specific registration and metrics.
 */
public class NGO extends User {
    public static final String STATUS_LISTED = "LISTED";
    public static final String STATUS_PARTNERED = "PARTNERED";

    private String registrationNumber; // Official NGO registration / tax exemption number
    private String serviceArea;        // Operating borough, city, or community district
    private int activeFoodRequests;
    private int beneficiariesServed;
    private String partnerStatus = STATUS_LISTED; // "LISTED" or "PARTNERED" (default LISTED)
    private String cause;              // Mission / public cause description
    private String website;            // Official public website URL

    public NGO() {
        super();
        this.role = "NGO";
    }

    public NGO(int id, String name, String email, String passwordHash, String salt, 
               String phone, String organizationName, String address) {
        super(id, name, email, passwordHash, salt, "NGO", phone, organizationName, address);
        this.registrationNumber = "NGO-REG-DEFAULT";
        this.serviceArea = "Metro Area";
        this.activeFoodRequests = 4;
        this.beneficiariesServed = 1250;
        this.partnerStatus = STATUS_LISTED;
    }

    public NGO(int id, String name, String email, String passwordHash, String salt, 
               String phone, String organizationName, String address, 
               String registrationNumber, String serviceArea) {
        super(id, name, email, passwordHash, salt, "NGO", phone, organizationName, address);
        this.registrationNumber = registrationNumber;
        this.serviceArea = serviceArea;
        this.activeFoodRequests = 4;
        this.beneficiariesServed = 1250;
        this.partnerStatus = STATUS_PARTNERED; // Default existing seeded verified NGOs as PARTNERED
    }

    public NGO(int id, String name, String email, String passwordHash, String salt, 
               String phone, String organizationName, String address, 
               String registrationNumber, String serviceArea, String partnerStatus, String cause, String website) {
        super(id, name, email, passwordHash, salt, "NGO", phone, organizationName, address);
        this.registrationNumber = registrationNumber;
        this.serviceArea = serviceArea;
        this.activeFoodRequests = 0;
        this.beneficiariesServed = 0;
        this.partnerStatus = (partnerStatus != null) ? partnerStatus : STATUS_LISTED;
        this.cause = cause;
        this.website = website;
    }

    public boolean isPartnered() {
        return STATUS_PARTNERED.equalsIgnoreCase(partnerStatus);
    }

    public String getPartnerBadge() {
        if (isPartnered()) {
            return "Partnered NGO";
        } else {
            return "Listed from public information, not yet partnered";
        }
    }

    @Override
    public String getRoleBadge() {
        if (isPartnered()) {
            return "Verified Partner NGO (" + registrationNumber + ")";
        } else {
            return "Listed NGO (Public Info — Not Yet Partnered)";
        }
    }

    @Override
    public String getRoleDescription() {
        return "Organization receiving and distributing surplus food in " + serviceArea + ".";
    }

    @Override
    public String getDashboardGreeting() {
        return "Welcome to the NGO Dispatch Center, " + getName() + "!";
    }

    // OOP Concept: Method Overriding (Polymorphic dashboard launcher)
    @Override
    public void showDashboard() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                com.fooddonation.ui.NGODashboard dashboard = new com.fooddonation.ui.NGODashboard();
                dashboard.setVisible(true);
            } catch (Exception e) {
                System.err.println("[NGO] Error launching dashboard: " + e.getMessage());
            }
        });
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getServiceArea() {
        return serviceArea;
    }

    public void setServiceArea(String serviceArea) {
        this.serviceArea = serviceArea;
    }

    public int getActiveFoodRequests() {
        return activeFoodRequests;
    }

    public void setActiveFoodRequests(int activeFoodRequests) {
        this.activeFoodRequests = activeFoodRequests;
    }

    public int getBeneficiariesServed() {
        return beneficiariesServed;
    }

    public void setBeneficiariesServed(int beneficiariesServed) {
        this.beneficiariesServed = beneficiariesServed;
    }

    public String getPartnerStatus() {
        return partnerStatus;
    }

    public void setPartnerStatus(String partnerStatus) {
        this.partnerStatus = (partnerStatus != null) ? partnerStatus : STATUS_LISTED;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}
