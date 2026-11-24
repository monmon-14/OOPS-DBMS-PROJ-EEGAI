package com.fooddonation.ui;

import com.fooddonation.dao.DonationDAO;
import com.fooddonation.model.DonationStatus;
import com.fooddonation.model.Donor;
import com.fooddonation.model.FoodDonation;
import com.fooddonation.model.User;
import com.fooddonation.service.DeliveryService;
import com.fooddonation.service.DonationService;
import com.fooddonation.service.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Full-featured interactive Donor Dashboard with a modern left sidebar,
 * real-time statistical KPI cards, dynamic Add Food form with chronological validation,
 * My Donations JTable with custom status badges, selective cancellation,
 * and automatic background expiration tracking.
 */
public class DonorDashboard extends JFrame {

    private final DonationService donationService;
    private User currentDonor;

    // Navigation & Views
    private JPanel contentCardsPanel;
    private CardLayout cardLayout;
    private JButton navDashboardBtn;
    private JButton navAddFoodBtn;
    private JButton navMyDonationsBtn;
    private JButton navReportsBtn;

    // Dashboard View Components
    private JLabel totalListedValue;
    private JLabel totalClaimedValue;
    private JLabel totalDeliveredValue;
    private JLabel mealsDonatedValue;
    private DefaultTableModel recentTableModel;
    private JTable recentTable;

    // Add Food View Components
    private JComboBox<String> tamilCategoryCombo;
    private UITheme.RoundedTextField foodNameField;
    private JComboBox<String> categoryCombo;
    private JComboBox<String> occasionCombo;
    private JSpinner quantitySpinner;
    private JSpinner prepTimeSpinner;
    private JSpinner expiryTimeSpinner;
    private UITheme.RoundedTextField addressField;
    private JTextArea notesArea;
    private JPanel formAlertPanel;
    private JLabel formAlertLabel;
    private UITheme.RoundedButton submitFoodBtn;

    // My Donations View Components
    private DefaultTableModel myDonationsTableModel;
    private JTable myDonationsTable;
    private JButton cancelDonationBtn;
    private List<FoodDonation> cachedDonations;

    // Background Expiration Auto-Checker Timer
    private Timer autoExpiryTimer;

    public DonorDashboard() {
        this(new DonationService());
    }

    public DonorDashboard(DonationService donationService) {
        this.donationService = donationService;
        this.currentDonor = UserSession.getInstance().getCurrentUser();

        // Fallback for standalone demo run if no session logged in
        if (this.currentDonor == null) {
            this.currentDonor = new Donor(1, "Green Garden Bistro", "bistro@greengarden.com",
                    null, null, "9876543210", "Green Garden Bistro", "124 Culinary Boulevard, Downtown", "Restaurant");
            UserSession.getInstance().setCurrentUser(this.currentDonor);
        }

        initWindow();
        initUI();
        loadAllData();
        startAutoExpiryTimer();
    }

    private void initWindow() {
        setTitle("ஈகை — உணவு கொடையாளர் தளம் (" + currentDonor.getName() + ")");
        setSize(1140, 740);
        setMinimumSize(new Dimension(1020, 680));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Apply Custom Magic Wand Cursor & Glitter Trail Glass Pane
        GlitterTrailGlassPane glassPane = new GlitterTrailGlassPane(this);
        setGlassPane(glassPane);
        glassPane.setVisible(true);
    }

    private void initUI() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(UITheme.BG_OFFWHITE);

        // 1. Modern Left Sidebar (Navigation, Donor Profile, Logout)
        JPanel sidebarPanel = createSidebarPanel();

        // 2. Central Content Area with CardLayout
        cardLayout = new CardLayout();
        contentCardsPanel = new JPanel(cardLayout);
        contentCardsPanel.setOpaque(false);

        // Add 3 Views to CardLayout
        contentCardsPanel.add(createDashboardView(), "DASHBOARD");
        contentCardsPanel.add(createAddFoodView(), "ADD_FOOD");
        contentCardsPanel.add(createMyDonationsView(), "MY_DONATIONS");
        contentCardsPanel.add(new ReportsPanel(), "REPORTS");

        rootPanel.add(sidebarPanel, BorderLayout.WEST);
        rootPanel.add(contentCardsPanel, BorderLayout.CENTER);

        setContentPane(rootPanel);
    }

    /**
     * Builds the Modern Dark-Green Sidebar with navigation tabs and donor profile.
     */
    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0x4A, 0x15, 0x21), // Deep Burgundy
                        0, getHeight(), new Color(0x28, 0x0A, 0x11) // Dark Wine Burgundy
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        sidebar.setLayout(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(new EmptyBorder(24, 16, 24, 16));

        // Top Branding
        JPanel topBrand = new JPanel();
        topBrand.setLayout(new BoxLayout(topBrand, BoxLayout.Y_AXIS));
        topBrand.setOpaque(false);

        JLabel logo = new JLabel("ஈகை");
        logo.setFont(UITheme.getTamilFont(Font.BOLD, 26));
        logo.setForeground(Color.WHITE);

        JLabel subLogo = new JLabel("பகுத்துண்டு பல்லுயிர் ஓம்புதல்");
        subLogo.setFont(UITheme.getTamilFont(Font.BOLD, 11));
        subLogo.setForeground(new Color(0x9B, 0xB3, 0x9A)); // Pastel Sage

        topBrand.add(logo);
        topBrand.add(Box.createVerticalStrut(2));
        topBrand.add(subLogo);
        topBrand.add(Box.createVerticalStrut(24));

        // Donor Profile Badge Card
        JPanel profileCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(new Color(255, 255, 255, 40));
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        profileCard.setLayout(new BoxLayout(profileCard, BoxLayout.Y_AXIS));
        profileCard.setOpaque(false);
        profileCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        boolean isDemoDonor = (currentDonor instanceof Donor) && ((Donor) currentDonor).isDemo();
        profileCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, isDemoDonor ? 90 : 65));

        String donorType = (currentDonor instanceof Donor) ? ((Donor) currentDonor).getDonorType() : "Restaurant";
        JLabel dName = new JLabel(currentDonor.getName());
        dName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dName.setForeground(Color.WHITE);

        JLabel dType = new JLabel(donorType + " Partner");
        dType.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        dType.setForeground(new Color(0xC5, 0xD6, 0xC4)); // Light Pastel Sage

        profileCard.add(dName);
        profileCard.add(Box.createVerticalStrut(3));
        profileCard.add(dType);

        if (isDemoDonor) {
            JLabel demoBadge = new JLabel("<html><span style='background:#78350F; color:#FDE68A; font-size:9px; padding:2px 6px; border-radius:3px;'>Demo Donor (Fictional)</span></html>");
            profileCard.add(Box.createVerticalStrut(4));
            profileCard.add(demoBadge);
        }

        topBrand.add(profileCard);
        topBrand.add(Box.createVerticalStrut(24));

        sidebar.add(topBrand, BorderLayout.NORTH);

        // Center Navigation Buttons
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setOpaque(false);

        navDashboardBtn = createSidebarNavButton("Dashboard", true);
        navAddFoodBtn = createSidebarNavButton("Add Food", false);
        navMyDonationsBtn = createSidebarNavButton("My Donations", false);
        navReportsBtn = createSidebarNavButton("Reports & Analytics", false);

        navDashboardBtn.addActionListener(e -> switchView("DASHBOARD"));
        navAddFoodBtn.addActionListener(e -> switchView("ADD_FOOD"));
        navMyDonationsBtn.addActionListener(e -> switchView("MY_DONATIONS"));
        navReportsBtn.addActionListener(e -> switchView("REPORTS"));

        navPanel.add(navDashboardBtn);
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(navAddFoodBtn);
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(navMyDonationsBtn);
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(navReportsBtn);

        sidebar.add(navPanel, BorderLayout.CENTER);

        // Bottom Logout Button
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        JButton logoutBtn = new JButton("Sign Out");
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutBtn.setForeground(new Color(0xFE, 0xCA, 0xCA));
        logoutBtn.setBackground(new Color(255, 255, 255, 25));
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 40), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        logoutBtn.addActionListener(e -> {
            if (autoExpiryTimer != null) autoExpiryTimer.stop();
            UserSession.getInstance().logout();
            UITheme.fadeTransition(DonorDashboard.this, () -> new RoleSelectionFrame().setVisible(true));
        });

        bottomPanel.add(logoutBtn, BorderLayout.CENTER);
        sidebar.add(bottomPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    private JButton createSidebarNavButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setPreferredSize(new Dimension(208, 42));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(0, 16, 0, 16));

        styleNavButton(btn, active);
        return btn;
    }

    private void styleNavButton(JButton btn, boolean active) {
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (active) {
                    g2.setColor(new Color(0x72, 0x8C, 0x6E)); // Muted Sage Green
                    g2.fill(new RoundRectangle2D.Float(0, 0, c.getWidth(), c.getHeight(), 10, 10));
                    btn.setForeground(Color.WHITE);
                } else {
                    btn.setForeground(new Color(0xF7, 0xF4, 0xEE)); // Warm Cream Beige
                }

                super.paint(g, c);
                g2.dispose();
            }
        });
        btn.repaint();
    }

    private void switchView(String viewName) {
        cardLayout.show(contentCardsPanel, viewName);
        styleNavButton(navDashboardBtn, "DASHBOARD".equals(viewName));
        styleNavButton(navAddFoodBtn, "ADD_FOOD".equals(viewName));
        styleNavButton(navMyDonationsBtn, "MY_DONATIONS".equals(viewName));
        styleNavButton(navReportsBtn, "REPORTS".equals(viewName));

        // Refresh views on switch
        if ("DASHBOARD".equals(viewName) || "MY_DONATIONS".equals(viewName)) {
            loadAllData();
        }
    }

    // =========================================================================
    // 1. DASHBOARD VIEW (KPI Cards + Recent Donations Table)
    // =========================================================================
    private JPanel createDashboardView() {
        JPanel view = new JPanel(new BorderLayout());
        view.setOpaque(false);
        view.setBorder(new EmptyBorder(28, 36, 28, 36));

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        // Header Greeting
        JLabel welcomeTitle = new JLabel("Welcome back, " + currentDonor.getName() + "!");
        welcomeTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        welcomeTitle.setForeground(UITheme.TEXT_DARK);

        JLabel welcomeSub = new JLabel("Real-time snapshot of your surplus commercial meals and community impact.");
        welcomeSub.setFont(UITheme.FONT_SUBTITLE);
        welcomeSub.setForeground(UITheme.TEXT_MUTED);

        topSection.add(welcomeTitle);
        topSection.add(Box.createVerticalStrut(4));
        topSection.add(welcomeSub);
        topSection.add(Box.createVerticalStrut(22));

        // 4 KPI Stat Cards in a 1x4 Grid
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        totalListedValue = new JLabel("0");
        totalClaimedValue = new JLabel("0");
        totalDeliveredValue = new JLabel("0");
        mealsDonatedValue = new JLabel("0");

        kpiGrid.add(createKpiCard("Total Listed", totalListedValue, "Lifetime listings created", new Color(0x6B, 0x1D, 0x2F)));
        kpiGrid.add(createKpiCard("Currently Claimed", totalClaimedValue, "Awaiting courier pickup", new Color(0x8C, 0x6A, 0x3E)));
        kpiGrid.add(createKpiCard("Delivered", totalDeliveredValue, "Reached shelters safely", new Color(0x72, 0x8C, 0x6E)));
        kpiGrid.add(createKpiCard("Meals Shared", mealsDonatedValue, "Total meal portions saved", new Color(0x5A, 0x47, 0x3E)));

        topSection.add(kpiGrid);
        topSection.add(Box.createVerticalStrut(28));

        view.add(topSection, BorderLayout.NORTH);

        // Recent Donations Table Panel
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);

        JPanel tableHeaderRow = new JPanel(new BorderLayout());
        tableHeaderRow.setOpaque(false);
        tableHeaderRow.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel tableTitle = new JLabel("Recent Food Donation Listings");
        tableTitle.setFont(UITheme.FONT_SECTION);
        tableTitle.setForeground(UITheme.TEXT_DARK);

        JButton postQuickBtn = new JButton("+ Post New Food");
        postQuickBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        postQuickBtn.setForeground(Color.WHITE);
        postQuickBtn.setBackground(UITheme.PRIMARY);
        postQuickBtn.setFocusPainted(false);
        postQuickBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        postQuickBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.PRIMARY_DARK, 1, true),
                new EmptyBorder(6, 14, 6, 14)
        ));
        postQuickBtn.addActionListener(e -> switchView("ADD_FOOD"));

        tableHeaderRow.add(tableTitle, BorderLayout.WEST);
        tableHeaderRow.add(postQuickBtn, BorderLayout.EAST);
        tableContainer.add(tableHeaderRow, BorderLayout.NORTH);

        // Recent JTable Setup
        String[] cols = {"ID", "Food Description", "Category", "Meals", "Prepared At", "Expires At", "Status"};
        recentTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        recentTable = new JTable(recentTableModel);
        styleTable(recentTable);
        recentTable.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());

        JScrollPane scrollPane = new JScrollPane(recentTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true));
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        view.add(tableContainer, BorderLayout.CENTER);
        return view;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, String subtitle, Color accentColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.SURFACE_WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(UITheme.BORDER_GRAY);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
                // Accent left stripe
                g2.setColor(accentColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, 5, getHeight(), 6, 6));
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 18, 14, 16));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLbl.setForeground(UITheme.TEXT_MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(UITheme.TEXT_DARK);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLbl.setForeground(UITheme.TEXT_MUTED);

        card.add(titleLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(3));
        card.add(subLbl);

        return card;
    }

    // =========================================================================
    // 2. ADD FOOD FORM VIEW
    // =========================================================================
    private JPanel createAddFoodView() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(28, 36, 28, 36));

        JPanel formCard = new JPanel();
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setOpaque(false);

        // Header Title
        JLabel title = new JLabel("Post Surplus Food Donation");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(UITheme.TEXT_DARK);

        JLabel sub = new JLabel("Fill in the details below. Verified NGOs and couriers will be instantly notified.");
        sub.setFont(UITheme.FONT_SUBTITLE);
        sub.setForeground(UITheme.TEXT_MUTED);

        formCard.add(title);
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(sub);
        formCard.add(Box.createVerticalStrut(14));

        // Quick Presets for Food Donors
        JPanel presetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        presetRow.setOpaque(false);
        presetRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel presetLbl = new JLabel("Quick Fill Presets:");
        presetLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        presetLbl.setForeground(UITheme.TEXT_MUTED);
        presetRow.add(presetLbl);

        JButton idliPresetBtn = new JButton("Idli & Sambar");
        idliPresetBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        idliPresetBtn.setFocusPainted(false);
        idliPresetBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        idliPresetBtn.addActionListener(e -> {
            tamilCategoryCombo.setSelectedItem("Idli & Sambar");
            foodNameField.setText("Steamed Soft Idlis with Fresh Chutney & Sambar");
            categoryCombo.setSelectedItem("Veg");
            occasionCombo.setSelectedIndex(0); // Regular
            quantitySpinner.setValue(50);
        });
        presetRow.add(idliPresetBtn);

        JButton pongalPresetBtn = new JButton("Pongal & Vada");
        pongalPresetBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pongalPresetBtn.setFocusPainted(false);
        pongalPresetBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        pongalPresetBtn.addActionListener(e -> {
            tamilCategoryCombo.setSelectedItem("Pongal & Vada");
            foodNameField.setText("Hot Ghee Ven Pongal, Medu Vada & Sambar");
            categoryCombo.setSelectedItem("Veg");
            occasionCombo.setSelectedIndex(1); // Pongal Festival
            quantitySpinner.setValue(40);
        });
        presetRow.add(pongalPresetBtn);

        JButton mealsPresetBtn = new JButton("Traditional Full Meals");
        mealsPresetBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        mealsPresetBtn.setFocusPainted(false);
        mealsPresetBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        mealsPresetBtn.addActionListener(e -> {
            tamilCategoryCombo.setSelectedItem("Traditional Full Meals");
            foodNameField.setText("Traditional Banana Leaf Full Meals (Rice, Sambar, Poriyal, Payasam)");
            categoryCombo.setSelectedItem("Veg");
            occasionCombo.setSelectedIndex(4); // Wedding Feast
            quantitySpinner.setValue(100);
        });
        presetRow.add(mealsPresetBtn);

        JButton puliPresetBtn = new JButton("Temple Puliyotharai");
        puliPresetBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        puliPresetBtn.setFocusPainted(false);
        puliPresetBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        puliPresetBtn.addActionListener(e -> {
            tamilCategoryCombo.setSelectedItem("Puliyotharai (Tamarind Rice)");
            foodNameField.setText("Temple Puliyotharai & Curd Rice Prasadam");
            categoryCombo.setSelectedItem("Veg");
            occasionCombo.setSelectedIndex(5); // Temple Festival
            quantitySpinner.setValue(75);
        });
        presetRow.add(puliPresetBtn);

        JButton biryaniPresetBtn = new JButton("Biryani");
        biryaniPresetBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        biryaniPresetBtn.setFocusPainted(false);
        biryaniPresetBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        biryaniPresetBtn.addActionListener(e -> {
            tamilCategoryCombo.setSelectedItem("Biryani");
            foodNameField.setText("Seeraga Samba Feast Biryani with Brinjal Gravy");
            categoryCombo.setSelectedItem("Non-Veg");
            occasionCombo.setSelectedIndex(2); // Deepavali
            quantitySpinner.setValue(60);
        });
        presetRow.add(biryaniPresetBtn);

        formCard.add(presetRow);
        formCard.add(Box.createVerticalStrut(12));

        // Form Alert Banner
        formAlertPanel = createFormAlertPanel();
        formAlertPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formAlertPanel.setVisible(false);
        formCard.add(formAlertPanel);
        formCard.add(Box.createVerticalStrut(12));

        // Field: Food Category Item
        JLabel tamilCatLbl = new JLabel("Food Category *");
        tamilCatLbl.setFont(UITheme.FONT_LABEL);
        tamilCategoryCombo = new JComboBox<>(FoodDonation.TAMIL_FOOD_ITEMS);
        tamilCategoryCombo.setFont(UITheme.FONT_INPUT);
        tamilCategoryCombo.setBackground(Color.WHITE);
        tamilCategoryCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        tamilCategoryCombo.addActionListener(e -> {
            String selectedItem = (String) tamilCategoryCombo.getSelectedItem();
            if (selectedItem != null) {
                foodNameField.setText(selectedItem);
            }
        });
        formCard.add(tamilCatLbl);
        formCard.add(Box.createVerticalStrut(5));
        formCard.add(tamilCategoryCombo);
        formCard.add(Box.createVerticalStrut(12));

        // Field: Food Name Description
        JLabel nameLbl = new JLabel("Food Name / Description *");
        nameLbl.setFont(UITheme.FONT_LABEL);
        foodNameField = new UITheme.RoundedTextField("e.g. Hot Ghee Ven Pongal with Medu Vada");
        foodNameField.setFont(UITheme.FONT_INPUT);
        foodNameField.setText(FoodDonation.TAMIL_FOOD_ITEMS[0]);
        foodNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        formCard.add(nameLbl);
        formCard.add(Box.createVerticalStrut(5));
        formCard.add(foodNameField);
        formCard.add(Box.createVerticalStrut(12));

        // Row Grid: Category, Quantity & Festival/Occasion
        JPanel catQtyRow = new JPanel(new GridLayout(1, 3, 12, 0));
        catQtyRow.setOpaque(false);
        catQtyRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JPanel catCol = new JPanel();
        catCol.setLayout(new BoxLayout(catCol, BoxLayout.Y_AXIS));
        catCol.setOpaque(false);
        JLabel catLbl = new JLabel("Diet Category *");
        catLbl.setFont(UITheme.FONT_LABEL);
        categoryCombo = new JComboBox<>(new String[]{"Veg", "Non-Veg"});
        categoryCombo.setFont(UITheme.FONT_INPUT);
        categoryCombo.setBackground(Color.WHITE);
        categoryCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        catCol.add(catLbl);
        catCol.add(Box.createVerticalStrut(5));
        catCol.add(categoryCombo);

        JPanel occasionCol = new JPanel();
        occasionCol.setLayout(new BoxLayout(occasionCol, BoxLayout.Y_AXIS));
        occasionCol.setOpaque(false);
        JLabel occLbl = new JLabel("Festival / Occasion");
        occLbl.setFont(UITheme.FONT_LABEL);
        occasionCombo = new JComboBox<>(new String[]{
                "Regular",
                "Pongal Festival",
                "Deepavali",
                "Karthigai Deepam",
                "Wedding Feast",
                "Temple Festival"
        });
        occasionCombo.setFont(UITheme.FONT_INPUT);
        occasionCombo.setBackground(Color.WHITE);
        occasionCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        occasionCol.add(occLbl);
        occasionCol.add(Box.createVerticalStrut(5));
        occasionCol.add(occasionCombo);

        JPanel qtyCol = new JPanel();
        qtyCol.setLayout(new BoxLayout(qtyCol, BoxLayout.Y_AXIS));
        qtyCol.setOpaque(false);
        JLabel qtyLbl = new JLabel("Quantity (Meals) *");
        qtyLbl.setFont(UITheme.FONT_LABEL);
        quantitySpinner = new JSpinner(new SpinnerNumberModel(25, 1, 10000, 5));
        quantitySpinner.setFont(UITheme.FONT_INPUT);
        quantitySpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        qtyCol.add(qtyLbl);
        qtyCol.add(Box.createVerticalStrut(5));
        qtyCol.add(quantitySpinner);

        catQtyRow.add(catCol);
        catQtyRow.add(occasionCol);
        catQtyRow.add(qtyCol);
        formCard.add(catQtyRow);
        formCard.add(Box.createVerticalStrut(12));

        // Row Grid: Preparation Time & Expiry Time (Chronological Spinners)
        JPanel timeRow = new JPanel(new GridLayout(1, 2, 14, 0));
        timeRow.setOpaque(false);
        timeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JPanel prepCol = new JPanel();
        prepCol.setLayout(new BoxLayout(prepCol, BoxLayout.Y_AXIS));
        prepCol.setOpaque(false);
        JLabel prepLbl = new JLabel("Preparation Time *");
        prepLbl.setFont(UITheme.FONT_LABEL);
        Date now = new Date();
        prepTimeSpinner = new JSpinner(new SpinnerDateModel(now, null, null, java.util.Calendar.MINUTE));
        prepTimeSpinner.setFont(UITheme.FONT_INPUT);
        prepTimeSpinner.setEditor(new JSpinner.DateEditor(prepTimeSpinner, "yyyy-MM-dd HH:mm"));
        prepTimeSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        prepCol.add(prepLbl);
        prepCol.add(Box.createVerticalStrut(5));
        prepCol.add(prepTimeSpinner);

        JPanel expCol = new JPanel();
        expCol.setLayout(new BoxLayout(expCol, BoxLayout.Y_AXIS));
        expCol.setOpaque(false);
        JLabel expLbl = new JLabel("Expiry Time * (Must be after Prep Time & in future)");
        expLbl.setFont(UITheme.FONT_LABEL);
        // Default to now + 6 hours
        Date defaultExpiry = new Date(System.currentTimeMillis() + (6 * 3600 * 1000));
        expiryTimeSpinner = new JSpinner(new SpinnerDateModel(defaultExpiry, null, null, java.util.Calendar.MINUTE));
        expiryTimeSpinner.setFont(UITheme.FONT_INPUT);
        expiryTimeSpinner.setEditor(new JSpinner.DateEditor(expiryTimeSpinner, "yyyy-MM-dd HH:mm"));
        expiryTimeSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        expCol.add(expLbl);
        expCol.add(Box.createVerticalStrut(5));
        expCol.add(expiryTimeSpinner);

        timeRow.add(prepCol);
        timeRow.add(expCol);
        formCard.add(timeRow);
        formCard.add(Box.createVerticalStrut(12));

        // Field: Pickup Address
        JLabel addrLbl = new JLabel("Pickup Dispatch Address *");
        addrLbl.setFont(UITheme.FONT_LABEL);
        String defaultAddr = (currentDonor.getAddress() != null) ? currentDonor.getAddress() : "124 Culinary Boulevard, Downtown";
        addressField = new UITheme.RoundedTextField(defaultAddr);
        addressField.setText(defaultAddr);
        addressField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        formCard.add(addrLbl);
        formCard.add(Box.createVerticalStrut(5));
        formCard.add(addressField);
        formCard.add(Box.createVerticalStrut(12));

        // Field: Notes
        JLabel notesLbl = new JLabel("Handling Notes / Packaging / Allergens (Optional)");
        notesLbl.setFont(UITheme.FONT_LABEL);
        notesArea = new JTextArea(3, 20);
        notesArea.setFont(UITheme.FONT_INPUT);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true));
        notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        formCard.add(notesLbl);
        formCard.add(Box.createVerticalStrut(5));
        formCard.add(notesScroll);
        formCard.add(Box.createVerticalStrut(18));

        // Submit Button
        submitFoodBtn = new UITheme.RoundedButton("Publish Food Donation Listing");
        submitFoodBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitFoodBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        submitFoodBtn.setPreferredSize(new Dimension(380, 44));
        submitFoodBtn.addActionListener(e -> executeSubmitFood());
        formCard.add(submitFoodBtn);

        // Put inside ScrollPane
        JScrollPane mainScroll = new JScrollPane(formCard);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(14);
        outer.add(mainScroll, BorderLayout.CENTER);

        return outer;
    }

    private JPanel createFormAlertPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.ERROR_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(UITheme.ERROR_BORDER);
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 12, 8, 12));
        panel.setPreferredSize(new Dimension(500, 40));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JComponent icon = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.ERROR_TEXT);
                g2.fillOval(1, 1, 14, 14);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString("!", 6, 12);
                g2.dispose();
            }
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(18, 18);
            }
        };

        formAlertLabel = new JLabel("");
        formAlertLabel.setFont(UITheme.FONT_SMALL);
        formAlertLabel.setForeground(UITheme.ERROR_TEXT);

        panel.add(icon, BorderLayout.WEST);
        panel.add(formAlertLabel, BorderLayout.CENTER);
        return panel;
    }

    private void executeSubmitFood() {
        hideFormAlert();

        String name = foodNameField.getText();
        String category = (String) categoryCombo.getSelectedItem();
        int quantity = (Integer) quantitySpinner.getValue();
        String address = addressField.getText();
        String notes = notesArea.getText();

        Date pDate = (Date) prepTimeSpinner.getValue();
        Date eDate = (Date) expiryTimeSpinner.getValue();

        LocalDateTime prepTime = pDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime expiryTime = eDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        String selectedOccasion = (occasionCombo != null) ? (String) occasionCombo.getSelectedItem() : "None";
        String occasionVal = "None";
        if (selectedOccasion != null) {
            if (selectedOccasion.toLowerCase().contains("pongal")) occasionVal = "Pongal";
            else if (selectedOccasion.toLowerCase().contains("deepavali")) occasionVal = "Deepavali";
            else if (selectedOccasion.toLowerCase().contains("karthigai")) occasionVal = "Karthigai";
            else if (selectedOccasion.toLowerCase().contains("wedding")) occasionVal = "Wedding";
            else if (selectedOccasion.toLowerCase().contains("temple")) occasionVal = "Temple Festival";
        }
        boolean isDemo = (currentDonor instanceof Donor) && ((Donor) currentDonor).isDemo();

        final String finalOccasion = occasionVal;
        final boolean finalIsDemo = isDemo;

        submitFoodBtn.setEnabled(false);
        submitFoodBtn.setText("Publishing Listing...");

        SwingUtilities.invokeLater(() -> {
            try {
                DonationService.DonationResult result = donationService.addDonation(
                        currentDonor.getId(), name, category, quantity, prepTime, expiryTime, address, notes, finalOccasion, finalIsDemo
                );

                if (result.isSuccess()) {
                    JOptionPane.showMessageDialog(this,
                            "<html><div style='font-family: Segoe UI; width: 300px;'>" +
                                    "<h3 style='color: #15803D; margin-bottom: 4px;'>Donation Published!</h3>" +
                                    "<p style='color: #334155;'><b>" + result.getDonation().getFoodName() + "</b> (" +
                                    result.getDonation().getQuantity() + " meals) is now live and available for NGOs.</p>" +
                                    "</div></html>",
                            "Listing Created",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Reset fields
                    foodNameField.setText("");
                    notesArea.setText("");
                    submitFoodBtn.setEnabled(true);
                    submitFoodBtn.setText("Publish Food Donation Listing");

                    // Refresh and jump to My Donations view
                    switchView("MY_DONATIONS");

                } else {
                    showFormAlert(result.getErrorMessage());
                    submitFoodBtn.setEnabled(true);
                    submitFoodBtn.setText("Publish Food Donation Listing");
                }
            } catch (Exception ex) {
                showFormAlert("Submission error: " + ex.getMessage());
                submitFoodBtn.setEnabled(true);
                submitFoodBtn.setText("Publish Food Donation Listing");
            }
        });
    }

    private void showFormAlert(String msg) {
        formAlertLabel.setText("<html>" + msg + "</html>");
        formAlertPanel.setVisible(true);
        formAlertPanel.revalidate();
        formAlertPanel.repaint();
    }

    private void hideFormAlert() {
        if (formAlertPanel.isVisible()) {
            formAlertPanel.setVisible(false);
            formAlertPanel.revalidate();
            formAlertPanel.repaint();
        }
    }

    // =========================================================================
    // 3. MY DONATIONS VIEW (JTable + Status Badges + Cancel Button)
    // =========================================================================
    private JPanel createMyDonationsView() {
        JPanel view = new JPanel(new BorderLayout());
        view.setOpaque(false);
        view.setBorder(new EmptyBorder(28, 36, 28, 36));

        // Top Section: Title & Controls Row
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(0, 0, 14, 0));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("My Food Donation Listings");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(UITheme.TEXT_DARK);

        JLabel sub = new JLabel("Live status tracking. Active items can be cancelled until claimed by an NGO courier.");
        sub.setFont(UITheme.FONT_SUBTITLE);
        sub.setForeground(UITheme.TEXT_MUTED);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(3));
        titlePanel.add(sub);

        // Right Actions: Refresh + Cancel Button
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsPanel.setOpaque(false);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setBackground(Color.WHITE);
        refreshBtn.setForeground(UITheme.TEXT_DARK);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true),
                new EmptyBorder(7, 14, 7, 14)
        ));
        refreshBtn.addActionListener(e -> loadAllData());

        cancelDonationBtn = new JButton("Cancel Selected Donation");
        cancelDonationBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cancelDonationBtn.setBackground(new Color(0xFE, 0xF2, 0xF2));
        cancelDonationBtn.setForeground(new Color(0xDC, 0x26, 0x26));
        cancelDonationBtn.setFocusPainted(false);
        cancelDonationBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelDonationBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xFE, 0xCA, 0xCA), 1, true),
                new EmptyBorder(7, 14, 7, 14)
        ));
        cancelDonationBtn.addActionListener(e -> executeCancelDonation());

        JButton trackDeliveryBtn = new JButton("Track Live Route");
        trackDeliveryBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        trackDeliveryBtn.setBackground(UITheme.PRIMARY_BG_TINT);
        trackDeliveryBtn.setForeground(UITheme.PRIMARY);
        trackDeliveryBtn.setFocusPainted(false);
        trackDeliveryBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        trackDeliveryBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xBB, 0xF7, 0xD0), 1, true),
                new EmptyBorder(7, 14, 7, 14)
        ));
        trackDeliveryBtn.setEnabled(false);
        trackDeliveryBtn.addActionListener(e -> {
            int selectedRow = myDonationsTable.getSelectedRow();
            if (selectedRow >= 0 && cachedDonations != null && selectedRow < cachedDonations.size()) {
                FoodDonation d = cachedDonations.get(selectedRow);
                DeliveryTrackingPanel.showLiveTimelineDialog(this, d.getId(), d.getFoodName(), new DeliveryService());
            }
        });

        actionsPanel.add(refreshBtn);
        actionsPanel.add(cancelDonationBtn);
        actionsPanel.add(trackDeliveryBtn);

        topBar.add(titlePanel, BorderLayout.WEST);
        topBar.add(actionsPanel, BorderLayout.EAST);
        view.add(topBar, BorderLayout.NORTH);

        // JTable for My Donations
        String[] cols = {"ID", "Food Description", "Category", "Meals", "Prepared At", "Expires At", "Status", "Notes"};
        myDonationsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        myDonationsTable = new JTable(myDonationsTableModel);
        styleTable(myDonationsTable);
        myDonationsTable.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());

        // Update Button enabled state based on selected row status
        myDonationsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateCancelButtonState();
                trackDeliveryBtn.setEnabled(myDonationsTable.getSelectedRow() >= 0);
            }
        });

        JScrollPane scrollPane = new JScrollPane(myDonationsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true));
        view.add(scrollPane, BorderLayout.CENTER);

        return view;
    }

    private void updateCancelButtonState() {
        int selectedRow = myDonationsTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < cachedDonations.size()) {
            FoodDonation donation = cachedDonations.get(selectedRow);
            boolean canCancel = donation.isCancelable();
            cancelDonationBtn.setEnabled(canCancel);
            if (!canCancel) {
                cancelDonationBtn.setToolTipText("Only 'Available' listings can be cancelled. Current status is " + donation.getStatus());
            } else {
                cancelDonationBtn.setToolTipText("Cancel this available food listing");
            }
        } else {
            cancelDonationBtn.setEnabled(false);
        }
    }

    private void executeCancelDonation() {
        int selectedRow = myDonationsTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= cachedDonations.size()) {
            JOptionPane.showMessageDialog(this, "Please select an available donation row from the table.", "No Row Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        FoodDonation donation = cachedDonations.get(selectedRow);
        if (!donation.isCancelable()) {
            JOptionPane.showMessageDialog(this,
                    "This donation cannot be cancelled because its current status is '" + donation.getStatus().getDisplayName() + "'.\n" +
                            "Only 'Available' items can be cancelled.",
                    "Cancellation Disallowed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel the food listing:\n\"" + donation.getFoodName() + "\" (" + donation.getQuantity() + " meals)?",
                "Confirm Listing Cancellation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = donationService.cancelDonation(donation.getId(), currentDonor.getId());
            if (success) {
                JOptionPane.showMessageDialog(this, "The donation listing has been marked as Cancelled.", "Listing Cancelled", JOptionPane.INFORMATION_MESSAGE);
                loadAllData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to cancel donation. It may have already been claimed.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Styles any JTable to match the clean green/white palette.
     */
    private void styleTable(JTable table) {
        table.setFont(UITheme.FONT_INPUT);
        table.setRowHeight(48);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(0xDC, 0xFC, 0xE7)); // Soft Emerald
        table.setSelectionForeground(UITheme.TEXT_DARK);
        table.setShowGrid(true);
        table.setGridColor(new Color(0xF1, 0xF5, 0xF9));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(0xF8, 0xFA, 0xFC));
        header.setForeground(UITheme.TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 36));

        // Column widths - generous width so full food names are clearly visible
        table.getColumnModel().getColumn(0).setPreferredWidth(45);  // ID
        table.getColumnModel().getColumn(1).setPreferredWidth(340); // Food Name (Full Name)
        table.getColumnModel().getColumn(2).setPreferredWidth(75);  // Category
        table.getColumnModel().getColumn(3).setPreferredWidth(65);  // Meals
        table.getColumnModel().getColumn(4).setPreferredWidth(125); // Prep
        table.getColumnModel().getColumn(5).setPreferredWidth(125); // Expiry
        table.getColumnModel().getColumn(6).setPreferredWidth(100); // Status Badge

        // Custom Cell Renderer for Food Name ensuring full visibility & hover tooltip
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                c.setBorder(new EmptyBorder(4, 8, 4, 8));
                if (value != null) {
                    String plainText = value.toString().replaceAll("<[^>]*>", "").trim();
                    c.setToolTipText(plainText);
                }
                return c;
            }
        });
    }

    /**
     * Custom TableCellRenderer rendering colored rounded badges for DonationStatus.
     */
    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String text = (value != null) ? value.toString() : "Available";
            DonationStatus status = DonationStatus.fromString(text);

            JPanel panel = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int w = getWidth();
                    int h = getHeight();
                    int badgeW = Math.min(w - 12, 92);
                    int badgeH = 22;
                    int bx = (w - badgeW) / 2;
                    int by = (h - badgeH) / 2;

                    g2.setColor(status.getBadgeBgColor());
                    g2.fill(new RoundRectangle2D.Float(bx, by, badgeW, badgeH, 12, 12));
                    g2.dispose();
                }
            };
            panel.setOpaque(true);
            panel.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);

            JLabel label = new JLabel(status.getDisplayName());
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));
            label.setForeground(status.getBadgeTextColor());
            panel.add(label);

            return panel;
        }
    }

    /**
     * Loads live data: auto-expires stale items, queries stats and tables.
     */
    public synchronized void loadAllData() {
        new Thread(() -> {
            // 1. Auto-expire old records
            donationService.expireOldDonations();

            // 2. Fetch KPI stats
            DonationDAO.DonorStats stats = donationService.getDonorStats(currentDonor.getId());

            // 3. Fetch all donations
            cachedDonations = donationService.getDonationsByDonor(currentDonor.getId());

            // 4. Fetch recent donations
            List<FoodDonation> recentList = donationService.getRecentDonations(currentDonor.getId(), 5);

            SwingUtilities.invokeLater(() -> {
                // Update KPI Labels
                totalListedValue.setText(String.valueOf(stats.getTotalListed()));
                totalClaimedValue.setText(String.valueOf(stats.getTotalClaimed()));
                totalDeliveredValue.setText(String.valueOf(stats.getTotalDelivered()));
                mealsDonatedValue.setText(String.format("%,d", stats.getMealsDonated()));

                // Populate Recent Table
                recentTableModel.setRowCount(0);
                for (FoodDonation d : recentList) {
                    recentTableModel.addRow(new Object[]{
                            d.getId(),
                            formatBilingualFoodHtml(d.getFoodName(), d.getFestivalBadge()),
                            d.getCategory(),
                            d.getQuantity(),
                            d.getFormattedPrepTime(),
                            d.getFormattedExpiryTime(),
                            d.getStatus().getDisplayName()
                    });
                }

                // Populate My Donations Table
                myDonationsTableModel.setRowCount(0);
                for (FoodDonation d : cachedDonations) {
                    myDonationsTableModel.addRow(new Object[]{
                            d.getId(),
                            formatBilingualFoodHtml(d.getFoodName(), d.getFestivalBadge()),
                            d.getCategory(),
                            d.getQuantity(),
                            d.getFormattedPrepTime(),
                            d.getFormattedExpiryTime(),
                            d.getStatus().getDisplayName(),
                            (d.getNotes() != null && !d.getNotes().isEmpty()) ? d.getNotes() : "—"
                    });
                }

                updateCancelButtonState();
            });
        }).start();
    }

    private static String formatBilingualFoodHtml(String foodName, String festivalBadge) {
        if (foodName == null || foodName.trim().isEmpty()) return "";
        String cleanName = foodName.trim();

        // If food name has Tamil prefix in parentheses like "பொங்கல் (Pongal)", use the clean English name
        if (cleanName.contains("(") && cleanName.contains(")")) {
            int openParen = cleanName.indexOf('(');
            int closeParen = cleanName.lastIndexOf(')');
            String prefix = cleanName.substring(0, openParen).trim();
            String inside = cleanName.substring(openParen + 1, closeParen).trim();
            if (prefix.matches(".*[\\u0B80-\\u0BFF].*") && !inside.isEmpty()) {
                cleanName = inside;
            }
        }

        String badgeHtml = (festivalBadge != null && !festivalBadge.isEmpty())
                ? " <span style='background:#FEF3C7; color:#92400E; font-size:10px; padding:2px 6px; border-radius:4px; font-weight:bold; white-space:nowrap;'>" + festivalBadge + "</span>"
                : "";

        return "<html><body style='margin:0; padding:2px;'><span style='font-family:Segoe UI, sans-serif; font-size:13px; font-weight:600; color:#1F2937;'>"
                + cleanName + "</span>" + badgeHtml + "</body></html>";
    }

    /**
     * Starts a background Swing timer running every 30 seconds
     * to automatically transition expired listings to 'Expired'.
     */
    private void startAutoExpiryTimer() {
        autoExpiryTimer = new Timer(30000, e -> {
            int expiredCount = donationService.expireOldDonations();
            if (expiredCount > 0) {
                loadAllData();
            }
        });
        autoExpiryTimer.start();
    }
}
