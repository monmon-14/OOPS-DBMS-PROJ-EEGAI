package com.fooddonation.model;

/**
 * Abstract Base Class representing a User in the Food Donation System.
 * Encapsulates common user attributes and defines polymorphic behavior
 * for specialized roles (Donor, NGO, and Volunteer).
 */
public abstract class User {
    protected int id;
    protected String name;
    protected String email;
    protected String passwordHash;
    protected String salt;
    protected String role; // "Donor", "NGO", or "Volunteer"
    protected String phone;
    protected String organizationName;
    protected String address;

    public User() {
    }

    public User(int id, String name, String email, String passwordHash, String salt, 
                String role, String phone, String organizationName, String address) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
        this.phone = phone;
        this.organizationName = organizationName;
        this.address = address;
    }

    // --- Abstract methods enforcing role-specific behavior (Polymorphism) ---
    public abstract String getRoleBadge();
    public abstract String getRoleDescription();
    public abstract String getDashboardGreeting();

    // OOP Concept: Abstract Method (Enforces polymorphic dashboard presentation across roles)
    public abstract void showDashboard();

    // --- Getters and Setters ---
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
