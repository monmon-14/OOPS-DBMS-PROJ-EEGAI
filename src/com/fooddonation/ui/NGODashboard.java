package com.fooddonation.ui;

import com.fooddonation.model.Claim;
import com.fooddonation.model.ClaimStatus;
import com.fooddonation.model.FoodDonation;
import com.fooddonation.model.NGO;
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
import java.util.List;

/**
 * NGO Partner Dispatch & Rescue Dashboard.
 * Matches DonorDashboard style (forest green sidebar, stat cards, wand cursor + glitter trail).
 * Features:
 * - Real-time KPI Stat Cards (Active Claims, Meals Claimed, Delivered Meals, Available Listings)
 * - Available Food JTable with live time-left countdown, category filter, and city/address search
 * - Concurrency-safe "Claim Food" action preventing double-claims inside database transaction
 * - "My Claims" tracking table with colored status badges
 * - Auto-expiration sync and soft fade/slide transitions
 */
public class NGODashboard extends JFrame {

    private final DonationService donationService;
    private User currentNgo;

    // Navigation & Layout
    private JPanel contentCardsPanel;
    private CardLayout cardLayout;
    private JButton navDashboardBtn;
    private JButton navAvailableFoodBtn;
    private JButton navMyClaimsBtn;
    private JButton navReportsBtn;

    // Dashboard View KPI Labels
    private JLabel activeClaimsValue;
    private JLabel mealsClaimedValue;
    private JLabel deliveredMealsValue;
    private JLabel availableListingsValue;

    // Available Food Table Components
    private DefaultTableModel availableTableModel;
    private JTable availableTable;
    private JComboBox<String> categoryFilterCombo;
    private UITheme.RoundedTextField citySearchField;
    private JButton claimFoodBtn;
    private List<FoodDonation> cachedAvailableDonations;

    // My Claims Table Components
    private DefaultTableModel myClaimsTableModel;
    private JTable myClaimsTable;
    private List<Claim> cachedClaims;

    // Background Timer
    private Timer autoExpiryTimer;

    public NGODashboard() {
        this(new DonationService());
    }

    public NGODashboard(DonationService donationService) {
        this.donationService = donationService;
        this.currentNgo = UserSession.getInstance().getCurrentUser();

        // Fallback for standalone demo run if no session logged in
        if (this.currentNgo == null) {
            this.currentNgo = new NGO(2, "Hope Food Foundation", "hope.foundation@ngo.org",
                    null, null, "9876543211", "Hope Foundation International",
                    "450 Community Way, Suite 12, Seattle, WA", "NGO-US-2024-8849", "Greater Metro & Shelters");
            UserSession.getInstance().setCurrentUser(this.currentNgo);
        }

        initWindow();
        initUI();
        loadAllData();
        startAutoExpiryTimer();
    }

    private void initWindow() {
        setTitle("ஈகை — NGO & Shelter Hub (" + currentNgo.getName() + ")");
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

        // 1. Left Sidebar
        JPanel sidebarPanel = createSidebarPanel();

        // 2. Center Content Area (CardLayout)
        cardLayout = new CardLayout();
        contentCardsPanel = new JPanel(cardLayout);
        contentCardsPanel.setOpaque(false);

        contentCardsPanel.add(createDashboardView(), "DASHBOARD");
        contentCardsPanel.add(createAvailableFoodView(), "AVAILABLE_FOOD");
        contentCardsPanel.add(createMyClaimsView(), "MY_CLAIMS");
        contentCardsPanel.add(new ReportsPanel(), "REPORTS");

        rootPanel.add(sidebarPanel, BorderLayout.WEST);
        rootPanel.add(contentCardsPanel, BorderLayout.CENTER);

        setContentPane(rootPanel);
    }

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

        // Top Brand & Profile
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

        // Profile Card
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
        profileCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));

        JLabel ngoNameLbl = new JLabel(currentNgo.getName());
        ngoNameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        ngoNameLbl.setForeground(Color.WHITE);

        String regNum = (currentNgo instanceof NGO) ? ((NGO) currentNgo).getRegistrationNumber() : "Verified NGO";
        JLabel regLbl = new JLabel("Reg: " + regNum);
        regLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        regLbl.setForeground(new Color(0xC5, 0xD6, 0xC4)); // Light Pastel Sage

        // Partner status badge label
        JLabel partnerBadgeLbl = new JLabel();
        partnerBadgeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        // Admin Toggle Button
        JButton adminToggleBtn = new JButton();
        adminToggleBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        adminToggleBtn.setFocusPainted(false);
        adminToggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        adminToggleBtn.setMargin(new Insets(3, 8, 3, 8));

        Runnable updatePartnerUi = () -> {
            boolean partnered = (currentNgo instanceof NGO) && ((NGO) currentNgo).isPartnered();
            if (partnered) {
                partnerBadgeLbl.setText("<html><span style='background:#065F46; color:#A7F3D0; padding:2px 6px; border-radius:4px;'>Partnered NGO</span></html>");
                adminToggleBtn.setText("Admin: Set Listed");
                adminToggleBtn.setBackground(new Color(0x37, 0x41, 0x51));
                adminToggleBtn.setForeground(Color.WHITE);
            } else {
                partnerBadgeLbl.setText("<html><div style='background:#78350F; color:#FDE68A; padding:3px 6px; border-radius:4px; font-size:9px;'>Listed from public information,<br>not yet partnered</div></html>");
                adminToggleBtn.setText("Admin: Partner NGO");
                adminToggleBtn.setBackground(new Color(0xD9, 0x77, 0x06));
                adminToggleBtn.setForeground(Color.WHITE);
            }
        };
        updatePartnerUi.run();

        adminToggleBtn.addActionListener(e -> {
            if (currentNgo instanceof NGO) {
                NGO ngo = (NGO) currentNgo;
                String nextStatus = ngo.isPartnered() ? NGO.STATUS_LISTED : NGO.STATUS_PARTNERED;
                boolean updated = new com.fooddonation.dao.UserDAO().updatePartnerStatus(ngo.getId(), nextStatus);
                if (updated) {
                    ngo.setPartnerStatus(nextStatus);
                    updatePartnerUi.run();
                    profileCard.revalidate();
                    profileCard.repaint();
                    JOptionPane.showMessageDialog(NGODashboard.this,
                            "<html><div style='font-family: Segoe UI, Nirmala UI; width: 300px;'>" +
                            "<h3 style='color: " + (ngo.isPartnered() ? "#15803D" : "#B45309") + "; margin-bottom: 4px;'>Partner Status Updated</h3>" +
                            "<p><b>" + ngo.getName() + "</b> status changed to: <b>" + nextStatus + "</b></p>" +
                            "<p style='color: #64748B; font-size: 11px;'>" +
                            (ngo.isPartnered() ? "This NGO is now partnered and permitted to claim food listings." : "Food claims are now restricted until partnered.") +
                            "</p></div></html>",
                            "Admin Toggle",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        profileCard.add(ngoNameLbl);
        profileCard.add(Box.createVerticalStrut(3));
        profileCard.add(regLbl);
        profileCard.add(Box.createVerticalStrut(6));
        profileCard.add(partnerBadgeLbl);
        profileCard.add(Box.createVerticalStrut(6));
        profileCard.add(adminToggleBtn);
        topBrand.add(profileCard);
        topBrand.add(Box.createVerticalStrut(18));

        sidebar.add(topBrand, BorderLayout.NORTH);

        // Center Nav Tabs
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setOpaque(false);

        navDashboardBtn = createSidebarNavButton("Dashboard", true);
        navAvailableFoodBtn = createSidebarNavButton("Available Food", false);
        navMyClaimsBtn = createSidebarNavButton("My Claims", false);
        navReportsBtn = createSidebarNavButton("Reports & Analytics", false);

        navDashboardBtn.addActionListener(e -> switchView("DASHBOARD"));
        navAvailableFoodBtn.addActionListener(e -> switchView("AVAILABLE_FOOD"));
        navMyClaimsBtn.addActionListener(e -> switchView("MY_CLAIMS"));
        navReportsBtn.addActionListener(e -> switchView("REPORTS"));

        navPanel.add(navDashboardBtn);
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(navAvailableFoodBtn);
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(navMyClaimsBtn);
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
            UITheme.fadeTransition(NGODashboard.this, () -> new RoleSelectionFrame().setVisible(true));
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
        styleNavButton(navAvailableFoodBtn, "AVAILABLE_FOOD".equals(viewName));
        styleNavButton(navMyClaimsBtn, "MY_CLAIMS".equals(viewName));
        styleNavButton(navReportsBtn, "REPORTS".equals(viewName));

        loadAllData();
    }

    // =========================================================================
    // 1. DASHBOARD VIEW (KPI Cards + Quick Available Food Table)
    // =========================================================================
    private JPanel createDashboardView() {
        JPanel view = new JPanel(new BorderLayout());
        view.setOpaque(false);
        view.setBorder(new EmptyBorder(28, 36, 28, 36));

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        JLabel title = new JLabel("Welcome, " + currentNgo.getName() + "!");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(UITheme.TEXT_DARK);

        JLabel sub = new JLabel("Review incoming surplus food listings from local commercial kitchens and shelters.");
        sub.setFont(UITheme.FONT_SUBTITLE);
        sub.setForeground(UITheme.TEXT_MUTED);

        topSection.add(title);
        topSection.add(Box.createVerticalStrut(4));
        topSection.add(sub);
        topSection.add(Box.createVerticalStrut(22));

        // 4 KPI Cards
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        activeClaimsValue = new JLabel("0");
        mealsClaimedValue = new JLabel("0");
        deliveredMealsValue = new JLabel("0");
        availableListingsValue = new JLabel("0");

        kpiGrid.add(createKpiCard("Active Claims", activeClaimsValue, "Awaiting volunteer delivery", new Color(0x8C, 0x6A, 0x3E)));
        kpiGrid.add(createKpiCard("Meals Claimed", mealsClaimedValue, "Total meal portions secured", new Color(0x6B, 0x1D, 0x2F)));
        kpiGrid.add(createKpiCard("Meals Delivered", deliveredMealsValue, "Distributed to community", new Color(0x72, 0x8C, 0x6E)));
        kpiGrid.add(createKpiCard("Available Food", availableListingsValue, "Unclaimed surplus nearby", new Color(0x5A, 0x47, 0x3E)));

        topSection.add(kpiGrid);
        topSection.add(Box.createVerticalStrut(28));
        view.add(topSection, BorderLayout.NORTH);

        // Center Quick Action Table section
        JPanel centerSection = new JPanel(new BorderLayout());
        centerSection.setOpaque(false);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel quickTitle = new JLabel("Surplus Food Ready for Claiming");
        quickTitle.setFont(UITheme.FONT_SECTION);
        quickTitle.setForeground(UITheme.TEXT_DARK);

        JButton browseAllBtn = new JButton("Browse All & Filter ->");
        browseAllBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        browseAllBtn.setForeground(UITheme.PRIMARY);
        browseAllBtn.setContentAreaFilled(false);
        browseAllBtn.setFocusPainted(false);
        browseAllBtn.setBorderPainted(false);
        browseAllBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        browseAllBtn.addActionListener(e -> switchView("AVAILABLE_FOOD"));

        headerRow.add(quickTitle, BorderLayout.WEST);
        headerRow.add(browseAllBtn, BorderLayout.EAST);
        centerSection.add(headerRow, BorderLayout.NORTH);

        // Embed the shared available table panel
        centerSection.add(createSharedAvailableTablePanel(), BorderLayout.CENTER);

        view.add(centerSection, BorderLayout.CENTER);
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
    // 2. AVAILABLE FOOD VIEW (Filters + City Search + JTable + Claim Action)
    // =========================================================================
    private JPanel createAvailableFoodView() {
        JPanel view = new JPanel(new BorderLayout());
        view.setOpaque(false);
        view.setBorder(new EmptyBorder(28, 36, 28, 36));

        // Title Header
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(0, 0, 16, 0));

        JLabel title = new JLabel("Live Available Food Listings");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(UITheme.TEXT_DARK);

        JLabel sub = new JLabel("Select an available listing to claim it. Concurrency protection ensures items cannot be double-claimed.");
        sub.setFont(UITheme.FONT_SUBTITLE);
        sub.setForeground(UITheme.TEXT_MUTED);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(3));
        titlePanel.add(sub);

        view.add(titlePanel, BorderLayout.NORTH);
        view.add(createSharedAvailableTablePanel(), BorderLayout.CENTER);

        return view;
    }

    private JPanel createSharedAvailableTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        // Control Toolbar: Filter Category + Search City + Refresh + Claim Button
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        toolbar.setOpaque(false);

        // Category Filter
        JLabel catLbl = new JLabel("Category / Item:");
        catLbl.setFont(UITheme.FONT_LABEL);
        categoryFilterCombo = new JComboBox<>(new String[]{
                "All", "Veg", "Non-Veg",
                "பொங்கல் (Pongal)",
                "இட்லி (Idli)",
                "தோசை (Dosa)",
                "சாம்பார் சாதம் (Sambar Rice)",
                "தயிர் சாதம் (Curd Rice)",
                "எலுமிச்சை சாதம் (Lemon Rice)",
                "புளியோதரை (Puliogare)",
                "பிரியாணி (Biryani)",
                "சாப்பாடு (Meals)",
                "சப்பாத்தி (Chapati)"
        });
        categoryFilterCombo.setFont(UITheme.getTamilFont(Font.PLAIN, 12));
        categoryFilterCombo.setBackground(Color.WHITE);
        categoryFilterCombo.setPreferredSize(new Dimension(170, 34));
        categoryFilterCombo.addActionListener(e -> reloadAvailableTable());

        // City Search
        JLabel searchLbl = new JLabel("City / Address:");
        searchLbl.setFont(UITheme.FONT_LABEL);
        citySearchField = new UITheme.RoundedTextField("Type city to search...");
        citySearchField.setPreferredSize(new Dimension(180, 34));
        citySearchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) { reloadAvailableTable(); }
        });

        // Refresh Button
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setBackground(Color.WHITE);
        refreshBtn.setForeground(UITheme.TEXT_DARK);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));
        refreshBtn.addActionListener(e -> loadAllData());

        // Claim Button (Green action button)
        claimFoodBtn = new JButton("Claim Selected Food");
        claimFoodBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        claimFoodBtn.setBackground(UITheme.PRIMARY);
        claimFoodBtn.setForeground(Color.WHITE);
        claimFoodBtn.setFocusPainted(false);
        claimFoodBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        claimFoodBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.PRIMARY_DARK, 1, true),
                new EmptyBorder(7, 16, 7, 16)
        ));
        claimFoodBtn.setEnabled(false);
        claimFoodBtn.addActionListener(e -> executeClaimFood());

        toolbar.add(catLbl);
        toolbar.add(categoryFilterCombo);
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(searchLbl);
        toolbar.add(citySearchField);
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(refreshBtn);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(claimFoodBtn);

        panel.add(toolbar, BorderLayout.NORTH);

        // Available Food JTable
        String[] columns = {"ID", "Food Description", "Donor", "Category", "Quantity (Meals)", "Expires At", "Pickup Address", "Time Left"};
        availableTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        availableTable = new JTable(availableTableModel);
        styleTable(availableTable);
        availableTable.setRowHeight(38);
        availableTable.getColumnModel().getColumn(7).setCellRenderer(new CountdownCellRenderer());

        availableTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                claimFoodBtn.setEnabled(availableTable.getSelectedRow() >= 0);
            }
        });

        JScrollPane scrollPane = new JScrollPane(availableTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void executeClaimFood() {
        int selectedRow = availableTable.getSelectedRow();
        if (selectedRow < 0 || cachedAvailableDonations == null || selectedRow >= cachedAvailableDonations.size()) {
            JOptionPane.showMessageDialog(this, "Please select an available food item to claim.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Rule: Only PARTNERED NGOs can claim food listings!
        if (currentNgo instanceof NGO && !((NGO) currentNgo).isPartnered()) {
            JOptionPane.showMessageDialog(this,
                    "<html><div style='font-family: Segoe UI, Nirmala UI; width: 340px;'>" +
                            "<h3 style='color: #B45309; margin-bottom: 6px;'>Partner Verification Required</h3>" +
                            "<p style='color: #1F2937;'><b>" + currentNgo.getName() + "</b> is <i>Listed from public information, not yet partnered</i>.</p>" +
                            "<p style='color: #4B5563;'><b>Only PARTNERED NGOs can claim surplus food listings.</b></p>" +
                            "<p style='color: #6B7280; font-size: 11px; margin-top: 6px;'>Tip: Click the <b>Admin: Partner NGO</b> button in the sidebar profile card to activate partnership.</p>" +
                            "</div></html>",
                    "Claim Restricted",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        FoodDonation selectedDonation = cachedAvailableDonations.get(selectedRow);

        int confirm = JOptionPane.showConfirmDialog(this,
                "<html><div style='font-family: Segoe UI; width: 320px;'>" +
                        "<h3 style='color: #15803D; margin-bottom: 4px;'>Confirm Food Claim</h3>" +
                        "<p>Claim <b>" + selectedDonation.getFoodName() + "</b> (" + selectedDonation.getQuantity() + " meals)?</p>" +
                        "<p style='color: #64748B;'>Donor: " + selectedDonation.getDonorName() + "<br>Pickup: " + selectedDonation.getPickupAddress() + "</p>" +
                        "</div></html>",
                "Claim Food Listing",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            claimFoodBtn.setEnabled(false);
            claimFoodBtn.setText("Claiming...");

            SwingUtilities.invokeLater(() -> {
                boolean success = donationService.claimDonation(selectedDonation.getId(), currentNgo.getId());
                claimFoodBtn.setText("Claim Selected Food");

                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "<html><div style='font-family: Segoe UI; width: 300px;'>" +
                                    "<h3 style='color: #15803D; margin-bottom: 4px;'>Food Claimed Successfully!</h3>" +
                                    "<p style='color: #334155;'><b>" + selectedDonation.getFoodName() + "</b> is now reserved for " + currentNgo.getName() + ".</p>" +
                                    "<p style='color: #64748B;'>Our volunteer couriers have been notified for pickup dispatch.</p>" +
                                    "</div></html>",
                            "Claim Confirmed",
                            JOptionPane.INFORMATION_MESSAGE);

                    loadAllData();
                    switchView("MY_CLAIMS");
                } else {
                    JOptionPane.showMessageDialog(this,
                            "<html><div style='font-family: Segoe UI; width: 300px;'>" +
                                    "<h3 style='color: #DC2626; margin-bottom: 4px;'>Claim Unavailable</h3>" +
                                    "<p style='color: #334155;'>This listing has just been claimed by another organization or expired.</p>" +
                                    "</div></html>",
                            "Could Not Claim",
                            JOptionPane.WARNING_MESSAGE);
                    loadAllData();
                }
            });
        }
    }

    // =========================================================================
    // 3. MY CLAIMS VIEW
    // =========================================================================
    private JPanel createMyClaimsView() {
        JPanel view = new JPanel(new BorderLayout());
        view.setOpaque(false);
        view.setBorder(new EmptyBorder(28, 36, 28, 36));

        // Header Title
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(0, 0, 14, 0));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("My Organization's Claims");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(UITheme.TEXT_DARK);

        JLabel sub = new JLabel("Track volunteer courier assignments and delivery fulfillment in real-time.");
        sub.setFont(UITheme.FONT_SUBTITLE);
        sub.setForeground(UITheme.TEXT_MUTED);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(3));
        titlePanel.add(sub);

        JPanel actionToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionToolbar.setOpaque(false);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setBackground(Color.WHITE);
        refreshBtn.setForeground(UITheme.TEXT_DARK);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true),
                new EmptyBorder(6, 14, 6, 14)
        ));
        refreshBtn.addActionListener(e -> loadAllData());

        JButton trackRouteBtn = new JButton("Track Live Route");
        trackRouteBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        trackRouteBtn.setBackground(UITheme.PRIMARY_BG_TINT);
        trackRouteBtn.setForeground(UITheme.PRIMARY);
        trackRouteBtn.setFocusPainted(false);
        trackRouteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        trackRouteBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xBB, 0xF7, 0xD0), 1, true),
                new EmptyBorder(6, 14, 6, 14)
        ));
        trackRouteBtn.setEnabled(false);
        trackRouteBtn.addActionListener(e -> {
            int selectedRow = myClaimsTable.getSelectedRow();
            if (selectedRow >= 0 && cachedClaims != null && selectedRow < cachedClaims.size()) {
                Claim c = cachedClaims.get(selectedRow);
                DeliveryTrackingPanel.showLiveTimelineDialog(this, c.getDonationId(), c.getFoodName(), new DeliveryService());
            }
        });

        actionToolbar.add(refreshBtn);
        actionToolbar.add(trackRouteBtn);

        topBar.add(titlePanel, BorderLayout.WEST);
        topBar.add(actionToolbar, BorderLayout.EAST);
        view.add(topBar, BorderLayout.NORTH);

        // My Claims JTable
        String[] columns = {"Claim ID", "Food Description", "Category", "Quantity", "Donor", "Pickup Address", "Assigned Volunteer", "Status", "Claimed At"};
        myClaimsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        myClaimsTable = new JTable(myClaimsTableModel);
        styleTable(myClaimsTable);
        myClaimsTable.getColumnModel().getColumn(7).setCellRenderer(new ClaimStatusBadgeRenderer());

        myClaimsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                trackRouteBtn.setEnabled(myClaimsTable.getSelectedRow() >= 0);
            }
        });

        JScrollPane scrollPane = new JScrollPane(myClaimsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true));
        view.add(scrollPane, BorderLayout.CENTER);

        return view;
    }

    private void styleTable(JTable table) {
        table.setFont(UITheme.FONT_INPUT);
        table.setRowHeight(36);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(0xDC, 0xFC, 0xE7));
        table.setSelectionForeground(UITheme.TEXT_DARK);
        table.setShowGrid(true);
        table.setGridColor(new Color(0xF1, 0xF5, 0xF9));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(0xF8, 0xFA, 0xFC));
        header.setForeground(UITheme.TEXT_MUTED);
        header.setPreferredSize(new Dimension(0, 36));
    }

    private static class CountdownCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            String text = (value != null) ? value.toString() : "";
            if (text.contains("m left") && !text.contains("h ")) {
                lbl.setForeground(new Color(0xDC, 0x26, 0x26)); // Red urgency
            } else if (text.contains("Expired")) {
                lbl.setForeground(new Color(0x64, 0x74, 0x8B));
            } else {
                lbl.setForeground(new Color(0x15, 0x80, 0x3D)); // Fresh green
            }
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            return lbl;
        }
    }

    private static class ClaimStatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String text = (value != null) ? value.toString() : "Claimed";
            ClaimStatus status = ClaimStatus.fromString(text);

            JPanel panel = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int w = getWidth();
                    int h = getHeight();
                    int badgeW = Math.min(w - 10, 115);
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

    private void reloadAvailableTable() {
        String cat = (categoryFilterCombo != null) ? (String) categoryFilterCombo.getSelectedItem() : "All";
        String city = (citySearchField != null) ? citySearchField.getText() : "";

        new Thread(() -> {
            List<FoodDonation> list;
            if (cat.equalsIgnoreCase("All") || cat.equalsIgnoreCase("Veg") || cat.equalsIgnoreCase("Non-Veg")) {
                list = donationService.getAvailable(cat, city);
            } else {
                list = donationService.getAvailable("All", city);
                String cleanCat = cat.contains("(") ? cat.substring(0, cat.indexOf('(')).trim() : cat.trim();
                List<FoodDonation> filtered = new java.util.ArrayList<>();
                for (FoodDonation d : list) {
                    if (d.getFoodName().toLowerCase().contains(cleanCat.toLowerCase()) || d.getFoodName().toLowerCase().contains(cat.toLowerCase())) {
                        filtered.add(d);
                    }
                }
                list = filtered;
            }
            cachedAvailableDonations = list;

            SwingUtilities.invokeLater(() -> {
                availableTableModel.setRowCount(0);
                for (FoodDonation d : cachedAvailableDonations) {
                    availableTableModel.addRow(new Object[]{
                            d.getId(),
                            formatBilingualFoodHtml(d.getFoodName(), d.getFestivalBadge()),
                            formatDonorHtml(d.getDonorName(), d.isDemo()),
                            d.getCategory(),
                            d.getQuantity(),
                            d.getFormattedExpiryTime(),
                            d.getPickupAddress(),
                            d.getTimeLeftString()
                    });
                }
            });
        }).start();
    }

    public synchronized void loadAllData() {
        new Thread(() -> {
            donationService.expireOldDonations();

            DonationService.NgoStats stats = donationService.getNgoStats(currentNgo.getId());
            cachedClaims = donationService.getClaimsByNgo(currentNgo.getId());

            String cat = (categoryFilterCombo != null) ? (String) categoryFilterCombo.getSelectedItem() : "All";
            String city = (citySearchField != null) ? citySearchField.getText() : "";
            List<FoodDonation> list;
            if (cat.equalsIgnoreCase("All") || cat.equalsIgnoreCase("Veg") || cat.equalsIgnoreCase("Non-Veg")) {
                list = donationService.getAvailable(cat, city);
            } else {
                list = donationService.getAvailable("All", city);
                String cleanCat = cat.contains("(") ? cat.substring(0, cat.indexOf('(')).trim() : cat.trim();
                List<FoodDonation> filtered = new java.util.ArrayList<>();
                for (FoodDonation d : list) {
                    if (d.getFoodName().toLowerCase().contains(cleanCat.toLowerCase()) || d.getFoodName().toLowerCase().contains(cat.toLowerCase())) {
                        filtered.add(d);
                    }
                }
                list = filtered;
            }
            cachedAvailableDonations = list;

            SwingUtilities.invokeLater(() -> {
                activeClaimsValue.setText(String.valueOf(stats.getActiveClaims()));
                mealsClaimedValue.setText(String.format("%,d", stats.getTotalMealsClaimed()));
                deliveredMealsValue.setText(String.format("%,d", stats.getDeliveredMeals()));
                availableListingsValue.setText(String.valueOf(stats.getAvailableListings()));

                // Available Table
                availableTableModel.setRowCount(0);
                for (FoodDonation d : cachedAvailableDonations) {
                    availableTableModel.addRow(new Object[]{
                            d.getId(),
                            formatBilingualFoodHtml(d.getFoodName(), d.getFestivalBadge()),
                            formatDonorHtml(d.getDonorName(), d.isDemo()),
                            d.getCategory(),
                            d.getQuantity(),
                            d.getFormattedExpiryTime(),
                            d.getPickupAddress(),
                            d.getTimeLeftString()
                    });
                }

                // My Claims Table
                myClaimsTableModel.setRowCount(0);
                for (Claim c : cachedClaims) {
                    myClaimsTableModel.addRow(new Object[]{
                            c.getId(),
                            formatBilingualFoodHtml(c.getFoodName(), null),
                            c.getCategory(),
                            c.getQuantity(),
                            c.getDonorName(),
                            c.getPickupAddress(),
                            (c.getVolunteerName() != null) ? c.getVolunteerName() : "Awaiting Courier",
                            c.getStatus().getDisplayName(),
                            c.getFormattedClaimedAt()
                    });
                }
            });
        }).start();
    }

    private static String formatBilingualFoodHtml(String foodName, String festivalBadge) {
        if (foodName == null || foodName.trim().isEmpty()) return "";
        String cleanName = foodName.trim();

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

    private static String formatDonorHtml(String donorName, boolean isDemo) {
        if (donorName == null) return "";
        if (isDemo) {
            return "<html><b>" + donorName + "</b> <span style='background:#FEF3C7; color:#B45309; font-size:9px; padding:1px 4px; border-radius:3px;'>Demo</span></html>";
        }
        return donorName;
    }

    private void startAutoExpiryTimer() {
        autoExpiryTimer = new Timer(30000, e -> {
            int count = donationService.expireOldDonations();
            if (count > 0) {
                loadAllData();
            }
        });
        autoExpiryTimer.start();
    }
}
