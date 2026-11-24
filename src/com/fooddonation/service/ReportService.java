package com.fooddonation.service;

import com.fooddonation.dao.ReportDAO;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service orchestrating reporting analytics, leaderboard aggregation,
 * date-filtered statistics, and CSV data export.
 *
 * SQL Queries Used (Implemented in ReportDAO):
 * --------------------------------------------------------------------------------------
 * 1. Summary Metrics:
 *    SELECT COALESCE(SUM(CASE WHEN d.status = 'Delivered' THEN d.quantity ELSE 0 END), 0) AS total_meals_distributed,
 *           COUNT(CASE WHEN d.status = 'Delivered' THEN 1 END) AS delivered_count,
 *           COUNT(CASE WHEN d.status = 'Expired' THEN 1 END) AS expired_count,
 *           COUNT(*) AS total_donations
 *    FROM Donations d WHERE (? IS NULL OR d.created_at >= ?) AND (? IS NULL OR d.created_at <= ?);
 *
 * 2. Top Donors:
 *    SELECT u.id AS donor_id, u.name AS donor_name, COALESCE(dn.donor_type, 'Commercial Kitchen') AS donor_type,
 *           COUNT(d.id) AS total_donations_count, COALESCE(SUM(CASE WHEN d.status = 'Delivered' THEN d.quantity ELSE 0 END), 0) AS total_meals_delivered
 *    FROM Users u LEFT JOIN Donors dn ON u.id = dn.user_id JOIN Donations d ON u.id = d.donor_id
 *    WHERE (? IS NULL OR d.created_at >= ?) AND (? IS NULL OR d.created_at <= ?)
 *    GROUP BY u.id, u.name, dn.donor_type HAVING total_meals_delivered > 0 ORDER BY total_meals_delivered DESC LIMIT ?;
 *
 * 3. Top NGOs:
 *    SELECT u.id AS ngo_id, u.name AS ngo_name, COALESCE(n.registration_number, 'Verified NGO') AS reg_number,
 *           COALESCE(n.service_area, 'Metro Area') AS service_area, COUNT(c.id) AS claims_count,
 *           COALESCE(SUM(CASE WHEN c.status = 'Delivered' THEN d.quantity ELSE 0 END), 0) AS total_meals_received
 *    FROM Users u LEFT JOIN NGOs n ON u.id = n.user_id JOIN Claims c ON u.id = c.ngo_id JOIN Donations d ON c.donation_id = d.id
 *    WHERE (? IS NULL OR c.claimed_at >= ?) AND (? IS NULL OR c.claimed_at <= ?)
 *    GROUP BY u.id, u.name, n.registration_number, n.service_area HAVING total_meals_received > 0 ORDER BY total_meals_received DESC LIMIT ?;
 *
 * 4. Monthly Donations:
 *    SELECT DATE_FORMAT(d.created_at, '%Y-%m') AS month_key, DATE_FORMAT(d.created_at, '%b %Y') AS month_label,
 *           COUNT(*) AS total_listings, COALESCE(SUM(CASE WHEN d.status = 'Delivered' THEN d.quantity ELSE 0 END), 0) AS delivered_meals,
 *           COALESCE(SUM(CASE WHEN d.status = 'Expired' THEN d.quantity ELSE 0 END), 0) AS expired_meals
 *    FROM Donations d WHERE (? IS NULL OR d.created_at >= ?) AND (? IS NULL OR d.created_at <= ?)
 *    GROUP BY month_key, month_label ORDER BY month_key ASC LIMIT 12;
 * --------------------------------------------------------------------------------------
 */
public class ReportService {

    private final ReportDAO reportDAO;

    public ReportService() {
        this(new ReportDAO());
    }

    public ReportService(ReportDAO reportDAO) {
        this.reportDAO = reportDAO;
    }

    /**
     * Retrieves overall food rescue stats for the date window.
     */
    public ReportDAO.OverallStatsReport getOverallStats(LocalDate startDate, LocalDate endDate) {
        return getOverallStats(startDate, endDate, null);
    }

    public ReportDAO.OverallStatsReport getOverallStats(LocalDate startDate, LocalDate endDate, Boolean isDemo) {
        return reportDAO.getOverallStats(startDate, endDate, isDemo);
    }

    /**
     * Retrieves top donors ranked by meals donated.
     */
    public List<ReportDAO.DonorRankItem> getTopDonors(int limit, LocalDate startDate, LocalDate endDate) {
        return getTopDonors(limit, startDate, endDate, null);
    }

    public List<ReportDAO.DonorRankItem> getTopDonors(int limit, LocalDate startDate, LocalDate endDate, Boolean isDemo) {
        return reportDAO.getTopDonors(limit, startDate, endDate, isDemo);
    }

    /**
     * Retrieves top NGOs ranked by meals received.
     */
    public List<ReportDAO.NgoRankItem> getTopNgos(int limit, LocalDate startDate, LocalDate endDate) {
        return getTopNgos(limit, startDate, endDate, null);
    }

    public List<ReportDAO.NgoRankItem> getTopNgos(int limit, LocalDate startDate, LocalDate endDate, String partnerStatus) {
        return reportDAO.getTopNgos(limit, startDate, endDate, partnerStatus);
    }

    /**
     * Retrieves monthly bar chart trend points.
     */
    public List<ReportDAO.MonthlyDataPoint> getMonthlyDonations(LocalDate startDate, LocalDate endDate) {
        return getMonthlyDonations(startDate, endDate, null);
    }

    public List<ReportDAO.MonthlyDataPoint> getMonthlyDonations(LocalDate startDate, LocalDate endDate, Boolean isDemo) {
        return reportDAO.getMonthlyDonations(startDate, endDate, isDemo);
    }

    // =========================================================================
    // OOP Concept: 1D and 2D Arrays
    // =========================================================================

    /**
     * OOP Concept: 1D Array (int[12] monthlyMeals feeding chart trends across 12 calendar months)
     * Control Flow: Uses for-each loop to iterate over monthly points.
     */
    public int[] getMonthlyMeals(LocalDate startDate, LocalDate endDate, Boolean isDemo) {
        int[] monthlyMeals = new int[12]; // 1D array representing Jan(0) to Dec(11)
        List<ReportDAO.MonthlyDataPoint> points = getMonthlyDonations(startDate, endDate, isDemo);

        // OOP Concept: Control Flow (for-each loop)
        for (ReportDAO.MonthlyDataPoint pt : points) {
            String key = pt.getMonthKey(); // Format: "YYYY-MM"
            if (key != null && key.length() >= 7) {
                try {
                    int month = Integer.parseInt(key.substring(5, 7)); // 1-12
                    if (month >= 1 && month <= 12) {
                        monthlyMeals[month - 1] += pt.getDeliveredMeals();
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return monthlyMeals;
    }

    /**
     * OOP Concept: 2D Array (int[4][2] weeklyByCategory feeding matrix chart [4 Weeks x 2 Categories: Veg, Non-Veg])
     * Control Flow: Uses standard for loop to populate array cells.
     */
    public int[][] getWeeklyByCategory(LocalDate startDate, LocalDate endDate, Boolean isDemo) {
        int[][] weeklyByCategory = new int[4][2]; // 2D array: rows = Week 1..4, cols = [0: Veg, 1: Non-Veg]

        // OOP Concept: Control Flow (standard for loop)
        for (int week = 0; week < 4; week++) {
            weeklyByCategory[week][0] = 60 + (week * 25); // Simulated Veg volume for week
            weeklyByCategory[week][1] = 30 + (week * 15); // Simulated Non-Veg volume for week
        }
        return weeklyByCategory;
    }

    /**
     * Resolves date range bounds based on preset choice.
     */
    public static LocalDate[] resolveDateRange(String preset) {
        LocalDate now = LocalDate.now();
        if (preset == null || preset.equalsIgnoreCase("All Time")) {
            return new LocalDate[]{null, null};
        } else if (preset.equalsIgnoreCase("Last 7 Days")) {
            return new LocalDate[]{now.minusDays(7), now};
        } else if (preset.equalsIgnoreCase("Last 30 Days")) {
            return new LocalDate[]{now.minusDays(30), now};
        } else if (preset.equalsIgnoreCase("Last 90 Days")) {
            return new LocalDate[]{now.minusDays(90), now};
        } else if (preset.equalsIgnoreCase("This Year")) {
            return new LocalDate[]{now.withDayOfYear(1), now};
        }
        return new LocalDate[]{null, null};
    }

    /**
     * Exports full analytical report to a CSV file.
     * Includes KPI summaries, monthly breakdown, top donors, and top NGOs.
     */
    public boolean exportToCsv(File targetFile, LocalDate startDate, LocalDate endDate) {
        return exportToCsv(targetFile, startDate, endDate, null, "All");
    }

    public boolean exportToCsv(File targetFile, LocalDate startDate, LocalDate endDate, Boolean isDemo, String partnerStatus) {
        if (targetFile == null) return false;

        try {
            if (targetFile.getParentFile() != null && !targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }

            ReportDAO.OverallStatsReport overall = getOverallStats(startDate, endDate, isDemo);
            List<ReportDAO.MonthlyDataPoint> monthly = getMonthlyDonations(startDate, endDate, isDemo);
            List<ReportDAO.DonorRankItem> donors = getTopDonors(10, startDate, endDate, isDemo);
            List<ReportDAO.NgoRankItem> ngos = getTopNgos(10, startDate, endDate, partnerStatus);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String rangeLabel = (startDate == null && endDate == null)
                    ? "All Time"
                    : ((startDate != null ? startDate.toString() : "Beginning") + " to " + (endDate != null ? endDate.toString() : "Present"));

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(targetFile), StandardCharsets.UTF_8))) {

                // UTF-8 BOM for Microsoft Excel compatibility
                writer.write('\ufeff');

                // 1. Report Header
                writer.write("# ஈகை (Eegai) — Community Food Donation & Rescue Report\n");
                writer.write("# Generated At," + java.time.LocalDateTime.now().format(dtf) + "\n");
                writer.write("# Date Range Filter," + rangeLabel + "\n");
                writer.write("# Demo Data Filter," + (isDemo == null ? "All Data" : (isDemo ? "Only Demo Data" : "Excluded Demo Data")) + "\n");
                writer.write("# NGO Partner Status Filter," + (partnerStatus != null ? partnerStatus : "All") + "\n\n");

                // 2. Executive KPI Summary
                writer.write("--- EXECUTIVE SUMMARY ---\n");
                writer.write("Metric,Value,Unit/Notes\n");
                writer.write("Total Meals Distributed," + overall.getTotalMealsDistributed() + ",meals\n");
                writer.write("Total Food Saved," + String.format("%.2f", overall.getFoodSavedKg()) + ",kg (meals * 0.4)\n");
                writer.write("Donations Delivered," + overall.getDonationsDelivered() + ",listings\n");
                writer.write("Donations Expired," + overall.getDonationsExpired() + ",listings\n");
                writer.write("Delivery Success Rate," + String.format("%.1f%%", overall.getSuccessRate()) + ",delivered / (delivered + expired)\n");
                writer.write("Total Listings Processed," + overall.getTotalDonations() + ",listings\n\n");

                // 3. Monthly Trends
                writer.write("--- MONTHLY DONATION TRENDS ---\n");
                writer.write("Month Key,Month,Total Listings,Delivered Meals,Expired Meals,Food Saved (kg)\n");
                for (ReportDAO.MonthlyDataPoint m : monthly) {
                    double kg = m.getDeliveredMeals() * 0.4;
                    writer.write(escapeCsv(m.getMonthKey()) + "," +
                            escapeCsv(m.getMonthLabel()) + "," +
                            m.getTotalListings() + "," +
                            m.getDeliveredMeals() + "," +
                            m.getExpiredMeals() + "," +
                            String.format("%.2f", kg) + "\n");
                }
                writer.write("\n");

                // 4. Top Donors Leaderboard
                writer.write("--- TOP DONORS LEADERBOARD ---\n");
                writer.write("Rank,Donor Name,Donor Type,Donations Count,Total Meals Donated,Food Saved (kg)\n");
                int rank = 1;
                for (ReportDAO.DonorRankItem d : donors) {
                    writer.write(rank++ + "," +
                            escapeCsv(d.getDonorName()) + "," +
                            escapeCsv(d.getDonorType()) + "," +
                            d.getDonationsCount() + "," +
                            d.getTotalMealsDelivered() + "," +
                            String.format("%.2f", d.getFoodSavedKg()) + "\n");
                }
                writer.write("\n");

                // 5. Top NGOs Leaderboard
                writer.write("--- TOP NGOS LEADERBOARD ---\n");
                writer.write("Rank,NGO Name,Registration Number,Service Area,Claims Count,Meals Received,Food Saved (kg)\n");
                rank = 1;
                for (ReportDAO.NgoRankItem n : ngos) {
                    writer.write(rank++ + "," +
                            escapeCsv(n.getNgoName()) + "," +
                            escapeCsv(n.getRegistrationNumber()) + "," +
                            escapeCsv(n.getServiceArea()) + "," +
                            n.getClaimsCount() + "," +
                            n.getTotalMealsReceived() + "," +
                            String.format("%.2f", n.getFoodSavedKg()) + "\n");
                }

                writer.flush();
                return true;
            }
        } catch (Exception e) {
            System.err.println("[ReportService] Error exporting CSV: " + e.getMessage());
            return false;
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
