package com.fooddonation.model;

/**
 * Concrete model class representing a Volunteer Driver / Food Courier.
 * Extends the abstract User class with vehicle type and availability metrics.
 */
public class Volunteer extends User {
    private String vehicleType;  // "None", "Bike", "Car"
    private String availability; // "Weekdays", "Weekends", "Flexible", etc.
    private int completedDeliveries;
    private double hoursLogged;

    public Volunteer() {
        super();
        this.role = "Volunteer";
        this.vehicleType = "Car";
        this.availability = "Flexible";
    }

    public Volunteer(int id, String name, String email, String passwordHash, String salt, 
                     String phone, String organizationName, String address) {
        super(id, name, email, passwordHash, salt, "Volunteer", phone, organizationName, address);
        this.vehicleType = "Car";
        this.availability = "Flexible";
        this.completedDeliveries = 18;
        this.hoursLogged = 42.5;
    }

    public Volunteer(int id, String name, String email, String passwordHash, String salt, 
                     String phone, String organizationName, String address, 
                     String vehicleType, String availability) {
        super(id, name, email, passwordHash, salt, "Volunteer", phone, organizationName, address);
        this.vehicleType = (vehicleType != null) ? vehicleType : "Car";
        this.availability = (availability != null) ? availability : "Flexible";
        this.completedDeliveries = 0;
        this.hoursLogged = 0.0;
    }

    @Override
    public String getRoleBadge() {
        return "Community Food Courier (" + vehicleType + ")";
    }

    @Override
    public String getRoleDescription() {
        return "Volunteer responder delivering food via " + vehicleType + " (" + availability + ").";
    }

    @Override
    public String getDashboardGreeting() {
        return "Ready to rescue surplus meals today, " + getName() + "?";
    }

    // OOP Concept: Method Overriding (Polymorphic dashboard launcher for Volunteer role)
    @Override
    public void showDashboard() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                com.fooddonation.ui.VolunteerDashboard dashboard = new com.fooddonation.ui.VolunteerDashboard();
                dashboard.setVisible(true);
            } catch (Exception e) {
                System.err.println("[Volunteer] Error launching dashboard: " + e.getMessage());
            }
        });
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public int getCompletedDeliveries() {
        return completedDeliveries;
    }

    public void setCompletedDeliveries(int completedDeliveries) {
        this.completedDeliveries = completedDeliveries;
    }

    public double getHoursLogged() {
        return hoursLogged;
    }

    public void setHoursLogged(double hoursLogged) {
        this.hoursLogged = hoursLogged;
    }
}
