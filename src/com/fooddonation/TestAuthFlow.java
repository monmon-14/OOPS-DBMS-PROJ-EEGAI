package com.fooddonation;

import com.fooddonation.dao.UserDAO;
import com.fooddonation.model.Donor;
import com.fooddonation.model.NGO;
import com.fooddonation.model.User;
import com.fooddonation.model.Volunteer;
import com.fooddonation.service.AuthService;
import com.fooddonation.service.RegistrationService;
import com.fooddonation.service.UserSession;
import com.fooddonation.util.PasswordUtil;

/**
 * Automated Test Runner verifying OOP model polymorphism,
 * RegistrationService validation rules, dynamic role persistence,
 * live password strength meter calculation, and end-to-end login.
 */
public class TestAuthFlow {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Running Food Donation App Automated Tests (Auth + Reg)");
        System.out.println("==================================================");

        UserDAO userDAO = new UserDAO();
        AuthService authService = new AuthService(userDAO);
        RegistrationService regService = new RegistrationService(userDAO);

        int testsPassed = 0;
        int totalTests = 0;

        // --- Test 1: Empty Fields Validation ---
        totalTests++;
        var r1 = regService.register("Donor", "", "test@test.com", "9876543210", "City", "Pass1234", "Pass1234", "Restaurant", "", "", "", "");
        if (!r1.isSuccess() && r1.getErrorMessage().contains("Name")) {
            System.out.println("✅ Test 1 Passed: Empty Name rejected properly.");
            testsPassed++;
        } else {
            System.err.println("❌ Test 1 Failed: " + r1.getErrorMessage());
        }

        // --- Test 2: Invalid Email Regex ---
        totalTests++;
        var r2 = regService.register("Donor", "Bistro", "not-an-email", "9876543210", "City", "Pass1234", "Pass1234", "Restaurant", "", "", "", "");
        if (!r2.isSuccess() && r2.getErrorMessage().contains("email format")) {
            System.out.println("✅ Test 2 Passed: Malformed email rejected by regex.");
            testsPassed++;
        } else {
            System.err.println("❌ Test 2 Failed: " + r2.getErrorMessage());
        }

        // --- Test 3: Phone not 10 digits ---
        totalTests++;
        var r3 = regService.register("Donor", "Bistro", "test@bistro.com", "12345", "City", "Pass1234", "Pass1234", "Restaurant", "", "", "", "");
        if (!r3.isSuccess() && r3.getErrorMessage().contains("10 digits")) {
            System.out.println("✅ Test 3 Passed: Non-10-digit phone rejected.");
            testsPassed++;
        } else {
            System.err.println("❌ Test 3 Failed: " + r3.getErrorMessage());
        }

        // --- Test 4: Password < 8 characters ---
        totalTests++;
        var r4 = regService.register("Donor", "Bistro", "test@bistro.com", "9876543210", "City", "Pass1", "Pass1", "Restaurant", "", "", "", "");
        if (!r4.isSuccess() && r4.getErrorMessage().contains("at least 8 characters")) {
            System.out.println("✅ Test 4 Passed: Short password rejected.");
            testsPassed++;
        } else {
            System.err.println("❌ Test 4 Failed: " + r4.getErrorMessage());
        }

        // --- Test 5: Password missing digits ---
        totalTests++;
        var r5 = regService.register("Donor", "Bistro", "test@bistro.com", "9876543210", "City", "PasswordOnly", "PasswordOnly", "Restaurant", "", "", "", "");
        if (!r5.isSuccess() && r5.getErrorMessage().contains("numeric digit")) {
            System.out.println("✅ Test 5 Passed: Password lacking digits rejected.");
            testsPassed++;
        } else {
            System.err.println("❌ Test 5 Failed: " + r5.getErrorMessage());
        }

        // --- Test 6: Passwords mismatch ---
        totalTests++;
        var r6 = regService.register("Donor", "Bistro", "test@bistro.com", "9876543210", "City", "Pass1234", "Mismatch99", "Restaurant", "", "", "", "");
        if (!r6.isSuccess() && r6.getErrorMessage().contains("do not match")) {
            System.out.println("✅ Test 6 Passed: Passwords mismatch detected.");
            testsPassed++;
        } else {
            System.err.println("❌ Test 6 Failed: " + r6.getErrorMessage());
        }

        // --- Test 7: Duplicate Email rejected ---
        totalTests++;
        var r7 = regService.register("NGO", "Duplicate Org", "hope.foundation@ngo.org", "9876543210", "City", "Pass1234", "Pass1234", "", "REG123", "Area", "", "");
        if (!r7.isSuccess() && r7.getErrorMessage().contains("already exists")) {
            System.out.println("✅ Test 7 Passed: Duplicate email rejected.");
            testsPassed++;
        } else {
            System.err.println("❌ Test 7 Failed: " + r7.getErrorMessage());
        }

        // --- Test 8: Live Password Strength Meter Calculations ---
        totalTests++;
        var sWeak = RegistrationService.assessPasswordStrength("abc");
        var sMedium = RegistrationService.assessPasswordStrength("pass1234");
        var sStrong = RegistrationService.assessPasswordStrength("Pass1234!");
        var sVeryStrong = RegistrationService.assessPasswordStrength("SuperSecure#2026!Long");
        if (sWeak.getLevel() == 1 && sMedium.getLevel() == 2 && sStrong.getLevel() == 3 && sVeryStrong.getLevel() == 4) {
            System.out.println("✅ Test 8 Passed: Live password strength progression verified (Weak -> Medium -> Strong -> Very Strong).");
            testsPassed++;
        } else {
            System.err.println("❌ Test 8 Failed: Strength calculation mismatch.");
        }

        // --- Test 9: Successful Donor Registration ---
        totalTests++;
        var rDonor = regService.register("Donor", "Sunset Bakery", "sunset@bakery.org", "9876543215", "San Francisco, CA",
                "Baker#2026", "Baker#2026", "Restaurant", "", "", "", "");
        if (rDonor.isSuccess() && rDonor.getUser() instanceof Donor) {
            Donor d = (Donor) rDonor.getUser();
            System.out.println("✅ Test 9 Passed: Donor registered with DonorType '" + d.getDonorType() + "' & SHA-256 + salt.");
            testsPassed++;
        } else {
            System.err.println("❌ Test 9 Failed: " + rDonor.getErrorMessage());
        }

        // --- Test 10: Successful NGO Registration with Reg Number & Area ---
        totalTests++;
        var rNgo = regService.register("NGO", "Shelter Care", "shelter@care.org", "9876543216", "Seattle, WA",
                "Shelter#2026", "Shelter#2026", "", "NGO-WA-9011", "South Seattle District", "", "");
        if (rNgo.isSuccess() && rNgo.getUser() instanceof NGO) {
            NGO n = (NGO) rNgo.getUser();
            System.out.println("✅ Test 10 Passed: NGO registered with RegNum '" + n.getRegistrationNumber() + "' & Area '" + n.getServiceArea() + "'.");
            testsPassed++;
        } else {
            System.err.println("❌ Test 10 Failed: " + rNgo.getErrorMessage());
        }

        // --- Test 11: Successful Volunteer Registration with Vehicle & Availability ---
        totalTests++;
        var rVol = regService.register("Volunteer", "Jordan Courier", "jordan@delivery.org", "9876543217", "Downtown",
                "Courier#2026", "Courier#2026", "", "", "", "Bike", "Weekends Only");
        if (rVol.isSuccess() && rVol.getUser() instanceof Volunteer) {
            Volunteer v = (Volunteer) rVol.getUser();
            System.out.println("✅ Test 11 Passed: Volunteer registered with Vehicle '" + v.getVehicleType() + "' & Availability '" + v.getAvailability() + "'.");
            testsPassed++;
        } else {
            System.err.println("❌ Test 11 Failed: " + rVol.getErrorMessage());
        }

        // --- Test 12: End-to-End Login with newly registered Donor ---
        totalTests++;
        var loginRes = authService.login("sunset@bakery.org", "Baker#2026", "Donor");
        if (loginRes.isSuccess() && loginRes.getUser() instanceof Donor) {
            UserSession.getInstance().setCurrentUser(loginRes.getUser());
            System.out.println("✅ Test 12 Passed: Newly registered user successfully authenticated & stored in UserSession.");
            testsPassed++;
        } else {
            System.err.println("❌ Test 12 Failed: " + loginRes.getErrorMessage());
        }

        System.out.println("==================================================");
        System.out.println("Results: " + testsPassed + " / " + totalTests + " tests passed successfully!");
        System.out.println("==================================================");

        if (testsPassed != totalTests) {
            System.exit(1);
        }
    }
}
