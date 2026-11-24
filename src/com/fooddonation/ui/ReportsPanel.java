package com.fooddonation.ui;

import com.fooddonation.dao.ReportDAO;
import com.fooddonation.service.ReportService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Universal Analytical Reports Panel for all system roles (Donor, NGO, Volunteer).
 * Displays real-time food rescue KPI cards, custom-painted monthly trend bar charts,
 * leaderboard rankings for Top Donors and Top NGOs, date range filtering,
 * and comprehensive CSV export.
 */
public class ReportsPanel extends JPanel {

    private final ReportService reportService;

    // Filter controls
    private JComboBox<String> dateRangeCombo;
    private JComboBox<String> demoFilterCombo;
    private JComboBox<String> partnerStatusCombo;
    private JLabel dateRangeSummaryLabel;
    private JButton refreshBtn;
    private JButton exportCsvBtn;

    // KPI Cards
    private JLabel mealsDistributedValue;
    private JLabel foodSavedKgValue;
    private JLabel deliveredVsExpiredValue;
    private JLabel partnerCountValue;
    private JLabel successRateBadge;

    // Visualization Panels
    private MonthlyBarChartPanel barChartPanel;
    private TopDonorsPanel topDonorsPanel;
    private TopNgosPanel topNgosPanel;
    private DeliveredRatioBarPanel ratioBarPanel;

    // Current filter bounds
    private LocalDate currentStartDate = null;
    private LocalDate currentEndDate = null;

    public ReportsPanel() {
        this(new ReportService());
    }

    public ReportsPanel(ReportService reportService) {
        this.reportService = (reportService != null) ? reportService : new ReportService();
        initUI();
        loadReportData();
    }

    /**
     * Shows the ReportsPanel inside a standalone dialog window.
     */
    public static void showReportsDialog(Window owner) {
        JDialog dialog = new JDialog(owner, "ஈகை — Community Impact & Food Rescue Reports", Dialog.ModalityType.MODELESS);
        dialog.setSize(1100, 750);
        dialog.setMinimumSize(new Dimension(980, 640));
        dialog.setLocationRelativeTo(owner);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        GlitterTrailGlassPane glassPane = new GlitterTrailGlassPane(dialog);
        dialog.setGlassPane(glassPane);
        glassPane.setVisible(true);

        dialog.setContentPane(new ReportsPanel());
        dialog.setVisible(true);
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BG_OFFWHITE);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        // 1. Top Filter & Action Bar
        JPanel topBar = createTopFilterBar();
        add(topBar, BorderLayout.NORTH);

        // 2. Center Content: Scrollable KPI cards + Bar Chart + Rankings
        JPanel contentContainer = new JPanel();
        contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
        contentContainer.setOpaque(false);

        // Section A: 4 KPI Stat Cards
        JPanel kpiCardsGrid = createKpiCardsPanel();
        contentContainer.add(kpiCardsGrid);
        contentContainer.add(Box.createVerticalStrut(18));

        // Section B: Monthly Donations Bar Chart Card
        JPanel chartCard = createChartCard();
        contentContainer.add(chartCard);
        contentContainer.add(Box.createVerticalStrut(18));

        // Section C: Split Leaderboards (Top Donors & Top NGOs)
        JPanel leaderboardsSplit = createLeaderboardsPanel();
        contentContainer.add(leaderboardsSplit);

        JScrollPane scrollPane = new JScrollPane(contentContainer);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
    }

    // =========================================================================
    // 1. TOP FILTER & CSV EXPORT TOOLBAR
    // =========================================================================
    private JPanel createTopFilterBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        // Left Header
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        JLabel title = new JLabel("Community Impact & Food Rescue Analytics");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(UITheme.TEXT_DARK);

        dateRangeSummaryLabel = new JLabel("All historical records included in metrics.");
        dateRangeSummaryLabel.setFont(UITheme.FONT_SUBTITLE);
        dateRangeSummaryLabel.setForeground(UITheme.TEXT_MUTED);

        leftPanel.add(title);
        leftPanel.add(Box.createVerticalStrut(2));
        leftPanel.add(dateRangeSummaryLabel);
        bar.add(leftPanel, BorderLayout.WEST);

        // Right Controls: Preset Combo + Demo Filter + NGO Filter + Refresh + Export CSV
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);

        JLabel rangeLbl = new JLabel("Date:");
        rangeLbl.setFont(UITheme.FONT_LABEL);
        rangeLbl.setForeground(UITheme.TEXT_DARK);

        dateRangeCombo = new JComboBox<>(new String[]{"All Time", "Last 7 Days", "Last 30 Days", "Last 90 Days", "This Year"});
        dateRangeCombo.setFont(UITheme.FONT_INPUT);
        dateRangeCombo.setBackground(Color.WHITE);
        dateRangeCombo.setPreferredSize(new Dimension(110, 34));
        dateRangeCombo.addActionListener(e -> handleDateRangeChanged());

        JLabel demoLbl = new JLabel("Data:");
        demoLbl.setFont(UITheme.FONT_LABEL);
        demoLbl.setForeground(UITheme.TEXT_DARK);

        demoFilterCombo = new JComboBox<>(new String[]{"All Data", "Exclude Demo", "Only Demo"});
        demoFilterCombo.setFont(UITheme.FONT_INPUT);
        demoFilterCombo.setBackground(Color.WHITE);
        demoFilterCombo.setPreferredSize(new Dimension(120, 34));
        demoFilterCombo.addActionListener(e -> loadReportData());

        JLabel partnerLbl = new JLabel("NGOs:");
        partnerLbl.setFont(UITheme.FONT_LABEL);
        partnerLbl.setForeground(UITheme.TEXT_DARK);

        partnerStatusCombo = new JComboBox<>(new String[]{"All NGOs", "Partnered Only", "Listed Only"});
        partnerStatusCombo.setFont(UITheme.FONT_INPUT);
        partnerStatusCombo.setBackground(Color.WHITE);
        partnerStatusCombo.setPreferredSize(new Dimension(120, 34));
        partnerStatusCombo.addActionListener(e -> loadReportData());

        refreshBtn = new JButton("Refresh");
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setBackground(Color.WHITE);
        refreshBtn.setForeground(UITheme.TEXT_DARK);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true),
                new EmptyBorder(7, 12, 7, 12)
        ));
        refreshBtn.addActionListener(e -> loadReportData());

        exportCsvBtn = new JButton("Export CSV");
        exportCsvBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        exportCsvBtn.setBackground(UITheme.PRIMARY);
        exportCsvBtn.setForeground(Color.WHITE);
        exportCsvBtn.setFocusPainted(false);
        exportCsvBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exportCsvBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.PRIMARY_DARK, 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));
        exportCsvBtn.addActionListener(e -> handleExportCsv());

        controls.add(rangeLbl);
        controls.add(dateRangeCombo);
        controls.add(demoLbl);
        controls.add(demoFilterCombo);
        controls.add(partnerLbl);
        controls.add(partnerStatusCombo);
        controls.add(refreshBtn);
        controls.add(exportCsvBtn);
        bar.add(controls, BorderLayout.EAST);

        return bar;
    }

    private void handleDateRangeChanged() {
        String preset = (String) dateRangeCombo.getSelectedItem();
        LocalDate[] bounds = ReportService.resolveDateRange(preset);
        this.currentStartDate = bounds[0];
        this.currentEndDate = bounds[1];

        if (currentStartDate == null && currentEndDate == null) {
            dateRangeSummaryLabel.setText("All historical records included in metrics.");
        } else {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            dateRangeSummaryLabel.setText("Filtered from " + currentStartDate.format(dtf) + " to " + currentEndDate.format(dtf));
        }

        loadReportData();
    }

    private void handleExportCsv() {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(java.time.LocalDateTime.now());
        String defaultFileName = "Egai_Food_Rescue_Report_" + timestamp + ".csv";

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Analytical Food Rescue Report (CSV)");
        fileChooser.setSelectedFile(new File(defaultFileName));
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Comma-Separated Values (*.csv)", "csv"));

        int userChoice = fileChooser.showSaveDialog(this);
        if (userChoice == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().toLowerCase().endsWith(".csv")) {
                selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".csv");
            }

            Boolean isDemo = resolveDemoFilter();
            String partnerStatus = resolvePartnerStatusFilter();

            File finalTarget = selectedFile;
            new Thread(() -> {
                boolean success = reportService.exportToCsv(finalTarget, currentStartDate, currentEndDate, isDemo, partnerStatus);
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(this,
                                "<html><div style='font-family: Segoe UI, Nirmala UI; width: 340px;'>" +
                                        "<h3 style='color: #15803D; margin-bottom: 4px;'>CSV Export Successful!</h3>" +
                                        "<p>Report saved to:<br><b>" + finalTarget.getAbsolutePath() + "</b></p>" +
                                        "<p style='color: #64748B;'>Includes executive summary, monthly trends, top donors, and NGOs.</p>" +
                                        "</div></html>",
                                "Report Exported",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to write CSV file. Please check folder permissions.", "Export Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).start();
        }
    }

    private Boolean resolveDemoFilter() {
        if (demoFilterCombo == null) return null;
        String sel = (String) demoFilterCombo.getSelectedItem();
        if ("Exclude Demo".equalsIgnoreCase(sel)) return Boolean.FALSE;
        if ("Only Demo".equalsIgnoreCase(sel)) return Boolean.TRUE;
        return null;
    }

    private String resolvePartnerStatusFilter() {
        if (partnerStatusCombo == null) return "All";
        String sel = (String) partnerStatusCombo.getSelectedItem();
        if ("Partnered Only".equalsIgnoreCase(sel)) return "PARTNERED";
        if ("Listed Only".equalsIgnoreCase(sel)) return "LISTED";
        return "All";
    }

    // =========================================================================
    // 2. KPI STAT CARDS
    // =========================================================================
    private JPanel createKpiCardsPanel() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        mealsDistributedValue = new JLabel("0");
        foodSavedKgValue = new JLabel("0.0 kg");
        deliveredVsExpiredValue = new JLabel("0 / 0");
        partnerCountValue = new JLabel("0");

        successRateBadge = new JLabel("100% Rate");
        successRateBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        successRateBadge.setForeground(UITheme.PRIMARY);
        successRateBadge.setBackground(UITheme.PRIMARY_BG_TINT);
        successRateBadge.setOpaque(true);
        successRateBadge.setBorder(BorderFactory.createLineBorder(new Color(0xA9, 0xBF, 0xA8), 1, true));

        grid.add(createStatCard("Total Meals Distributed", mealsDistributedValue, "Rescued and served to shelters", new Color(0x72, 0x8C, 0x6E), null));
        grid.add(createStatCard("Food Waste Prevented", foodSavedKgValue, "Calculated as meals × 0.4 kg", new Color(0x6B, 0x1D, 0x2F), null));
        grid.add(createStatCard("Delivered vs Expired", deliveredVsExpiredValue, "Rescue fulfillment count", new Color(0x8C, 0x6A, 0x3E), successRateBadge));
        grid.add(createStatCard("Active Community Hubs", partnerCountValue, "Participating Donors & NGOs", new Color(0x5A, 0x47, 0x3E), null));

        return grid;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, String subtitle, Color accentColor, JComponent accessory) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.SURFACE_WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(UITheme.BORDER_GRAY);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
                g2.setColor(accentColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, 5, getHeight(), 6, 6));
                g2.dispose();
            }
        };
        card.setLayout(new BorderLayout(0, 4));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 18, 12, 14));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLbl.setForeground(UITheme.TEXT_MUTED);
        topRow.add(titleLbl, BorderLayout.WEST);

        if (accessory != null) {
            topRow.add(accessory, BorderLayout.EAST);
        }

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(UITheme.TEXT_DARK);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLbl.setForeground(UITheme.TEXT_MUTED);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.add(valueLabel);
        centerPanel.add(Box.createVerticalStrut(2));
        centerPanel.add(subLbl);

        card.add(topRow, BorderLayout.NORTH);
        card.add(centerPanel, BorderLayout.CENTER);

        return card;
    }

    // =========================================================================
    // 3. MONTHLY DONATIONS BAR CHART CARD
    // =========================================================================
    private JPanel createChartCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.SURFACE_WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(UITheme.BORDER_GRAY);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        // Header Line: Title & Chart Legend
        JPanel headerLine = new JPanel(new BorderLayout());
        headerLine.setOpaque(false);

        JLabel chartTitle = new JLabel("Monthly Donations & Distribution Trends");
        chartTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        chartTitle.setForeground(UITheme.TEXT_DARK);

        // Legend
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        legendPanel.setOpaque(false);

        legendPanel.add(createLegendItem("Delivered Meals", new Color(0x15, 0x80, 0x3D)));
        legendPanel.add(createLegendItem("Expired Meals", new Color(0xEF, 0x44, 0x44)));

        headerLine.add(chartTitle, BorderLayout.WEST);
        headerLine.add(legendPanel, BorderLayout.EAST);
        card.add(headerLine, BorderLayout.NORTH);

        // Custom Bar Chart Canvas
        barChartPanel = new MonthlyBarChartPanel();
        barChartPanel.setPreferredSize(new Dimension(0, 210));
        card.add(barChartPanel, BorderLayout.CENTER);

        // Ratio Bar at the bottom of the card
        ratioBarPanel = new DeliveredRatioBarPanel();
        ratioBarPanel.setPreferredSize(new Dimension(0, 28));
        card.add(ratioBarPanel, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createLegendItem(String label, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);

        JPanel swatch = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(color);
                g2.fillRoundRect(0, 3, 12, 10, 4, 4);
                g2.dispose();
            }
        };
        swatch.setPreferredSize(new Dimension(14, 16));
        swatch.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setForeground(UITheme.TEXT_MUTED);

        p.add(swatch);
        p.add(lbl);
        return p;
    }

    // =========================================================================
    // 4. TOP DONORS & TOP NGOS SPLIT PANELS
    // =========================================================================
    private JPanel createLeaderboardsPanel() {
        JPanel split = new JPanel(new GridLayout(1, 2, 16, 0));
        split.setOpaque(false);

        topDonorsPanel = new TopDonorsPanel();
        topNgosPanel = new TopNgosPanel();

        split.add(topDonorsPanel);
        split.add(topNgosPanel);
        return split;
    }

    // =========================================================================
    // DATA REFRESH LOGIC
    // =========================================================================
    public void loadReportData() {
        final Boolean isDemo = resolveDemoFilter();
        final String partnerStatus = resolvePartnerStatusFilter();

        new Thread(() -> {
            ReportDAO.OverallStatsReport stats = reportService.getOverallStats(currentStartDate, currentEndDate, isDemo);
            List<ReportDAO.MonthlyDataPoint> monthlyData = reportService.getMonthlyDonations(currentStartDate, currentEndDate, isDemo);
            List<ReportDAO.DonorRankItem> topDonors = reportService.getTopDonors(5, currentStartDate, currentEndDate, isDemo);
            List<ReportDAO.NgoRankItem> topNgos = reportService.getTopNgos(5, currentStartDate, currentEndDate, partnerStatus);

            // OOP Concept: 1D and 2D Arrays (Calculating array metrics from ReportService)
            int[] monthlyMeals1D = reportService.getMonthlyMeals(currentStartDate, currentEndDate, isDemo);
            int[][] weeklyByCategory2D = reportService.getWeeklyByCategory(currentStartDate, currentEndDate, isDemo);

            SwingUtilities.invokeLater(() -> {
                // Update KPI Cards
                mealsDistributedValue.setText(String.format("%,d", stats.getTotalMealsDistributed()));
                foodSavedKgValue.setText(String.format("%,.1f kg", stats.getFoodSavedKg()));
                deliveredVsExpiredValue.setText(stats.getDonationsDelivered() + " / " + stats.getDonationsExpired());
                successRateBadge.setText(String.format("%.1f%% Success", stats.getSuccessRate()));
                partnerCountValue.setText(topDonors.size() + " Donors • " + topNgos.size() + " NGOs");

                // Update Bar Chart
                barChartPanel.setData(monthlyData);

                // Update Delivered vs Expired Ratio Bar
                ratioBarPanel.setCounts(stats.getDonationsDelivered(), stats.getDonationsExpired(), stats.getTotalMealsDistributed());

                // Update Leaderboards
                topDonorsPanel.setData(topDonors);
                topNgosPanel.setData(topNgos);

                revalidate();
                repaint();
            });
        }).start();
    }

    // =========================================================================
    // CUSTOM PAINTED MONTHLY BAR CHART COMPONENT
    // =========================================================================
    public static class MonthlyBarChartPanel extends JPanel {
        private List<ReportDAO.MonthlyDataPoint> data;

        public void setData(List<ReportDAO.MonthlyDataPoint> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            int leftPadding = 50;
            int rightPadding = 24;
            int topPadding = 24;
            int bottomPadding = 36;

            int chartW = w - leftPadding - rightPadding;
            int chartH = h - topPadding - bottomPadding;

            // Find maximum value for scaling
            int maxVal = 100;
            for (ReportDAO.MonthlyDataPoint p : data) {
                int total = p.getDeliveredMeals() + p.getExpiredMeals();
                if (total > maxVal) maxVal = total;
            }
            maxVal = ((maxVal / 100) + 1) * 100; // Round up to clean hundred

            // 1. Draw Horizontal Gridlines & Y-Axis values
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            FontMetrics fm = g2.getFontMetrics();

            int gridSteps = 4;
            for (int i = 0; i <= gridSteps; i++) {
                int val = (maxVal / gridSteps) * i;
                int y = topPadding + chartH - (int) ((val / (double) maxVal) * chartH);

                // Grid line
                g2.setColor(new Color(0xE0, 0xD7, 0xC9));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawLine(leftPadding, y, w - rightPadding, y);

                // Axis label
                g2.setColor(UITheme.TEXT_LIGHT);
                String labelStr = String.valueOf(val);
                g2.drawString(labelStr, leftPadding - fm.stringWidth(labelStr) - 8, y + 4);
            }

            // 2. Draw Bars and X-Axis labels
            int n = data.size();
            int colWidth = chartW / n;
            int barWidth = Math.min(38, (int) (colWidth * 0.55));

            for (int i = 0; i < n; i++) {
                ReportDAO.MonthlyDataPoint p = data.get(i);
                int cx = leftPadding + (i * colWidth) + (colWidth / 2);

                int deliveredHeight = (int) ((p.getDeliveredMeals() / (double) maxVal) * chartH);
                int expiredHeight = (int) ((p.getExpiredMeals() / (double) maxVal) * chartH);

                int barX = cx - (barWidth / 2);
                int deliveredY = topPadding + chartH - deliveredHeight;

                // Delivered Meals Bar (Muted Sage Green Gradient)
                if (deliveredHeight > 0) {
                    GradientPaint gp = new GradientPaint(
                            barX, deliveredY, new Color(0x9B, 0xB3, 0x9A), // Pastel Sage Light
                            barX, topPadding + chartH, new Color(0x72, 0x8C, 0x6E) // Muted Sage Green
                    );
                    g2.setPaint(gp);
                    g2.fill(new RoundRectangle2D.Float(barX, deliveredY, barWidth, deliveredHeight, 8, 8));
                }

                // Expired Meals Bar (Burgundy tip stacked on top if present)
                if (expiredHeight > 0) {
                    int expiredY = deliveredY - expiredHeight;
                    g2.setColor(new Color(0x6B, 0x1D, 0x2F, 190));
                    g2.fill(new RoundRectangle2D.Float(barX, expiredY, barWidth, expiredHeight, 6, 6));
                }

                // Value Callout on top of bar
                g2.setColor(UITheme.TEXT_DARK);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                String totalText = String.valueOf(p.getDeliveredMeals());
                int tw = g2.getFontMetrics().stringWidth(totalText);
                g2.drawString(totalText, cx - (tw / 2), deliveredY - 6);

                // Month Label below X axis
                g2.setColor(UITheme.TEXT_MUTED);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                String mLabel = p.getMonthLabel();
                int lw = g2.getFontMetrics().stringWidth(mLabel);
                g2.drawString(mLabel, cx - (lw / 2), topPadding + chartH + 18);
            }

            g2.dispose();
        }
    }

    // =========================================================================
    // DELIVERED VS EXPIRED PROPORTIONAL RATIO BAR
    // =========================================================================
    public static class DeliveredRatioBarPanel extends JPanel {
        private int delivered = 0;
        private int expired = 0;
        private int totalMeals = 0;

        public void setCounts(int delivered, int expired, int totalMeals) {
            this.delivered = delivered;
            this.expired = expired;
            this.totalMeals = totalMeals;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int barH = 12;
            int barY = 4;

            int sum = delivered + expired;
            double deliveredRatio = (sum > 0) ? (delivered / (double) sum) : 1.0;
            int deliveredW = (int) (w * deliveredRatio);

            // Background / Expired portion (Pastel Burgundy)
            g2.setColor(new Color(0xF9, 0xEB, 0xEF));
            g2.fill(new RoundRectangle2D.Float(0, barY, w, barH, 6, 6));

            // Delivered portion (Muted Sage Green)
            g2.setColor(new Color(0x72, 0x8C, 0x6E));
            g2.fill(new RoundRectangle2D.Float(0, barY, deliveredW, barH, 6, 6));

            // Subtitle text below bar
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(UITheme.TEXT_MUTED);
            String ratioText = String.format("Fulfillment Ratio: %.1f%% of all claimed listings successfully delivered to shelters.", deliveredRatio * 100);
            g2.drawString(ratioText, 2, barY + barH + 11);

            g2.dispose();
        }
    }

    // =========================================================================
    // TOP DONORS LEADERBOARD PANEL
    // =========================================================================
    public static class TopDonorsPanel extends JPanel {
        private JPanel rowsContainer;

        public TopDonorsPanel() {
            setLayout(new BorderLayout(0, 10));
            setBackground(UITheme.SURFACE_WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true),
                    new EmptyBorder(16, 18, 16, 18)
            ));

            JLabel title = new JLabel("Top Donor Champions");
            title.setFont(new Font("Segoe UI", Font.BOLD, 14));
            title.setForeground(UITheme.TEXT_DARK);

            rowsContainer = new JPanel();
            rowsContainer.setLayout(new BoxLayout(rowsContainer, BoxLayout.Y_AXIS));
            rowsContainer.setOpaque(false);

            add(title, BorderLayout.NORTH);
            add(rowsContainer, BorderLayout.CENTER);
        }

        public void setData(List<ReportDAO.DonorRankItem> list) {
            rowsContainer.removeAll();
            if (list == null || list.isEmpty()) {
                JLabel emptyLbl = new JLabel("No donation data recorded for period.");
                emptyLbl.setFont(UITheme.FONT_SMALL);
                emptyLbl.setForeground(UITheme.TEXT_MUTED);
                rowsContainer.add(emptyLbl);
            } else {
                int maxMeals = list.get(0).getTotalMealsDelivered();
                if (maxMeals <= 0) maxMeals = 1;

                int rank = 1;
                for (ReportDAO.DonorRankItem d : list) {
                    rowsContainer.add(createDonorRow(rank++, d, maxMeals));
                    rowsContainer.add(Box.createVerticalStrut(8));
                }
            }
            revalidate();
            repaint();
        }

        private JPanel createDonorRow(int rank, ReportDAO.DonorRankItem donor, int maxMeals) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

            // Left rank circle
            JLabel rankLbl = new JLabel(String.valueOf(rank), SwingConstants.CENTER);
            rankLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            rankLbl.setForeground(rank == 1 ? new Color(0xB4, 0x53, 0x09) : UITheme.TEXT_MUTED);
            rankLbl.setBackground(rank == 1 ? new Color(0xFE, 0xF3, 0xC7) : new Color(0xF1, 0xF5, 0xF9));
            rankLbl.setOpaque(true);
            rankLbl.setPreferredSize(new Dimension(24, 24));
            rankLbl.setBorder(BorderFactory.createLineBorder(rank == 1 ? new Color(0xFD, 0xE6, 0x8A) : UITheme.BORDER_GRAY, 1, true));

            // Middle name + type + progress bar (Bilingual Tamil / English display)
            JPanel midPanel = new JPanel();
            midPanel.setLayout(new BoxLayout(midPanel, BoxLayout.Y_AXIS));
            midPanel.setOpaque(false);

            String donorType = donor.getDonorType();
            String nameText;
            if (donorType != null && donorType.contains("(") && donorType.contains(")")) {
                int open = donorType.indexOf('(');
                int close = donorType.lastIndexOf(')');
                String tamilT = donorType.substring(0, open).trim();
                String engT = donorType.substring(open + 1, close).trim();
                nameText = "<html><div style='font-family: Nirmala UI, Segoe UI, sans-serif; line-height: 1.15;'>" +
                        "<b>" + donor.getDonorName() + "</b> — " + tamilT + "<br>" +
                        "<span style='color: #64748B; font-size: 10px;'>" + engT + "</span></div></html>";
            } else {
                nameText = "<html><b>" + donor.getDonorName() + "</b> <span style='color:#64748B; font-size:10px;'>(" + donorType + ")</span></html>";
            }

            JLabel nameLbl = new JLabel(nameText);
            nameLbl.setFont(UITheme.getTamilFont(Font.PLAIN, 12));
            nameLbl.setForeground(UITheme.TEXT_DARK);

            JProgressBar pbar = new JProgressBar(0, maxMeals);
            pbar.setValue(donor.getTotalMealsDelivered());
            pbar.setForeground(new Color(0x10, 0xB9, 0x81));
            pbar.setBackground(new Color(0xF1, 0xF5, 0xF9));
            pbar.setPreferredSize(new Dimension(100, 6));
            pbar.setBorderPainted(false);

            midPanel.add(nameLbl);
            midPanel.add(Box.createVerticalStrut(3));
            midPanel.add(pbar);

            // Right score badge
            JLabel mealsLbl = new JLabel(donor.getTotalMealsDelivered() + " meals");
            mealsLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            mealsLbl.setForeground(UITheme.PRIMARY);

            row.add(rankLbl, BorderLayout.WEST);
            row.add(midPanel, BorderLayout.CENTER);
            row.add(mealsLbl, BorderLayout.EAST);
            return row;
        }
    }

    // =========================================================================
    // TOP NGOS LEADERBOARD PANEL
    // =========================================================================
    public static class TopNgosPanel extends JPanel {
        private JPanel rowsContainer;

        public TopNgosPanel() {
            setLayout(new BorderLayout(0, 10));
            setBackground(UITheme.SURFACE_WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true),
                    new EmptyBorder(16, 18, 16, 18)
            ));

            JLabel title = new JLabel("Top NGO Distribution Partners");
            title.setFont(new Font("Segoe UI", Font.BOLD, 14));
            title.setForeground(UITheme.TEXT_DARK);

            rowsContainer = new JPanel();
            rowsContainer.setLayout(new BoxLayout(rowsContainer, BoxLayout.Y_AXIS));
            rowsContainer.setOpaque(false);

            add(title, BorderLayout.NORTH);
            add(rowsContainer, BorderLayout.CENTER);
        }

        public void setData(List<ReportDAO.NgoRankItem> list) {
            rowsContainer.removeAll();
            if (list == null || list.isEmpty()) {
                JLabel emptyLbl = new JLabel("No NGO claim data recorded for period.");
                emptyLbl.setFont(UITheme.FONT_SMALL);
                emptyLbl.setForeground(UITheme.TEXT_MUTED);
                rowsContainer.add(emptyLbl);
            } else {
                int maxMeals = list.get(0).getTotalMealsReceived();
                if (maxMeals <= 0) maxMeals = 1;

                int rank = 1;
                for (ReportDAO.NgoRankItem n : list) {
                    rowsContainer.add(createNgoRow(rank++, n, maxMeals));
                    rowsContainer.add(Box.createVerticalStrut(8));
                }
            }
            revalidate();
            repaint();
        }

        private JPanel createNgoRow(int rank, ReportDAO.NgoRankItem ngo, int maxMeals) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

            // Left rank circle
            JLabel rankLbl = new JLabel(String.valueOf(rank), SwingConstants.CENTER);
            rankLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            rankLbl.setForeground(rank == 1 ? new Color(0x1D, 0x4E, 0xD8) : UITheme.TEXT_MUTED);
            rankLbl.setBackground(rank == 1 ? new Color(0xDB, 0xEA, 0xFE) : new Color(0xF1, 0xF5, 0xF9));
            rankLbl.setOpaque(true);
            rankLbl.setPreferredSize(new Dimension(24, 24));
            rankLbl.setBorder(BorderFactory.createLineBorder(rank == 1 ? new Color(0xBF, 0xDB, 0xFE) : UITheme.BORDER_GRAY, 1, true));

            // Middle name + area + progress bar (Bilingual / Badged format)
            JPanel midPanel = new JPanel();
            midPanel.setLayout(new BoxLayout(midPanel, BoxLayout.Y_AXIS));
            midPanel.setOpaque(false);

            String ngoName = ngo.getNgoName();
            String labelHtml;
            if (ngoName.contains("[Listed]")) {
                labelHtml = "<html><div style='font-family: Nirmala UI, Segoe UI, sans-serif; line-height: 1.15;'>" +
                        "<b>" + ngoName.replace("[Listed]", "").trim() + "</b> " +
                        "<span style='background:#FEF3C7; color:#92400E; font-size:9px; padding:1px 4px; border-radius:3px;'>Listed</span><br>" +
                        "<span style='color: #64748B; font-size: 10px;'>Public Information • " + ngo.getServiceArea() + "</span></div></html>";
            } else {
                labelHtml = "<html><div style='font-family: Nirmala UI, Segoe UI, sans-serif; line-height: 1.15;'>" +
                        "<b>" + ngoName + "</b> " +
                        "<span style='background:#DCFCE7; color:#15803D; font-size:9px; padding:1px 4px; border-radius:3px;'>Partnered</span><br>" +
                        "<span style='color: #64748B; font-size: 10px;'>" + ngo.getServiceArea() + "</span></div></html>";
            }

            JLabel nameLbl = new JLabel(labelHtml);
            nameLbl.setFont(UITheme.getTamilFont(Font.PLAIN, 12));
            nameLbl.setForeground(UITheme.TEXT_DARK);

            JProgressBar pbar = new JProgressBar(0, maxMeals);
            pbar.setValue(ngo.getTotalMealsReceived());
            pbar.setForeground(new Color(0x3B, 0x82, 0xF6));
            pbar.setBackground(new Color(0xF1, 0xF5, 0xF9));
            pbar.setPreferredSize(new Dimension(100, 6));
            pbar.setBorderPainted(false);

            midPanel.add(nameLbl);
            midPanel.add(Box.createVerticalStrut(3));
            midPanel.add(pbar);

            // Right score badge
            JLabel mealsLbl = new JLabel(ngo.getTotalMealsReceived() + " meals");
            mealsLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            mealsLbl.setForeground(new Color(0x25, 0x63, 0xEB));

            row.add(rankLbl, BorderLayout.WEST);
            row.add(midPanel, BorderLayout.CENTER);
            row.add(mealsLbl, BorderLayout.EAST);
            return row;
        }
    }
}
