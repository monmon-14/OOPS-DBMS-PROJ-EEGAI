package com.fooddonation.ui;

import com.fooddonation.model.Claim;
import com.fooddonation.model.ClaimStatus;
import com.fooddonation.model.Delivery;
import com.fooddonation.model.FoodDonation;
import com.fooddonation.model.User;
import com.fooddonation.model.Volunteer;
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
 * Volunteer Courier & Food Rescue Dispatch Dashboard.
 * Matches DonorDashboard and NGODashboard style (forest green sidebar, stat cards,
 * wand cursor + glitter trail, soft transitions).
 *
 * Features:
 * - Real-time Volunteer KPI Stat Cards (Completed Deliveries, Active Deliveries, Available Rescue Tasks, Hours Contributed)
 * - "Available Food" JTable with live countdowns, category filter, and city search
 * - "Pickup Tasks" JTable showing open NGO claims awaiting courier assignment, with atomic "Accept Task" action
 * - "My Deliveries" JTable with interactive status stepper (Accepted -> Picked Up -> Delivered)
 * - Sequential single-step advancement, actual meals prompt, and live route pipeline dialog
 * - Background auto-expiry timer and concurrency protection against duplicate assignments
 */
public class VolunteerDashboard extends JFrame {

    private final DonationService donationService;
    private final DeliveryService deliveryService;
    private User currentVolunteer;

    // Navigation & Layout
    private JPanel contentCardsPanel;
    private CardLayout cardLayout;
    private JButton navDashboardBtn;
    private JButton navAvailableFoodBtn;
    private JButton navPickupTasksBtn;
    private JButton navMyDeliveriesBtn;
    private JButton navReportsBtn;

    // Dashboard View KPI Labels
    private JLabel completedDeliveriesValue;
    private JLabel activeDeliveriesValue;
    private JLabel availableTasksValue;
    private JLabel hoursContributedValue;

    // Available Food Table Components (Shared View)
    private DefaultTableModel availableTableModel;
    private JTable availableTable;
    private JComboBox<String> categoryFilterCombo;
    private UITheme.RoundedTextField citySearchField;
    private List<FoodDonation> cachedAvailableDonations;

    // Pickup Tasks Table Components
    private DefaultTableModel pickupTasksTableModel;
    private JTable pickupTasksTable;
    private JButton acceptTaskBtn;
    private List<Claim> cachedPickupTasks;

    // My Deliveries Table Components
    private DefaultTableModel myDeliveriesTableModel;
    private JTable myDeliveriesTable;
    private DeliveryTrackingPanel deliveryStepperPanel;
    private JButton trackTimelineBtn;
    private List<Claim> cachedDeliveries;

    // Background Timer
    private Timer autoExpiryTimer;

    public VolunteerDashboard() {
        this(new DonationService(), new DeliveryService());
    }

    public VolunteerDashboard(DonationService donationService) {
        this(donationService, new DeliveryService());
    }

    public VolunteerDashboard(DonationService donationService, DeliveryService deliveryService) {
        this.donationService = donationService;
        this.deliveryService = (deliveryService != null) ? deliveryService : new DeliveryService();
        this.currentVolunteer = UserSession.getInstance().getCurrentUser();

        // Fallback demo volunteer if accessed directly
        if (this.currentVolunteer == null) {
            this.currentVolunteer = new Volunteer(3, "Alex Green", "alex.green@volunteer.org",
                    null, null, "9876543212", "Community First Responders",
                    "789 Pine Street, Seattle, WA", "Car", "Flexible");
            UserSession.getInstance().setCurrentUser(this.currentVolunteer);
        }

        initWindow();
        initUI();
        loadAllData();
        startAutoExpiryTimer();
    }

    private void initWindow() {
        setTitle("ஈகை — Volunteer Courier Hub (" + currentVolunteer.getName() + ")");
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
        contentCardsPanel.add(createPickupTasksView(), "PICKUP_TASKS");
        contentCardsPanel.add(createMyDeliveriesView(), "MY_DELIVERIES");
        contentCardsPanel.add(new ReportsPanel(), "REPORTS");

        rootPanel.add(sidebarPanel, BorderLayout.WEST);
        rootPanel.add(contentCardsPanel, BorderLayout.CENTER);

        setContentPane(rootPanel);
    }

    // =========================================================================
    // SIDEBAR NAVIGATION
    // =========================================================================
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
        profileCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JLabel volNameLbl = new JLabel(currentVolunteer.getName());
        volNameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        volNameLbl.setForeground(Color.WHITE);

        String vehicleInfo = (currentVolunteer instanceof Volunteer)
                ? ((Volunteer) currentVolunteer).getVehicleType()
                : "Vehicle Responder";
        JLabel vehicleLbl = new JLabel(vehicleInfo + " Courier");
        vehicleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        vehicleLbl.setForeground(new Color(0xC5, 0xD6, 0xC4)); // Light Pastel Sage

        profileCard.add(volNameLbl);
        profileCard.add(Box.createVerticalStrut(3));
        profileCard.add(vehicleLbl);
        topBrand.add(profileCard);
        topBrand.add(Box.createVerticalStrut(24));

        sidebar.add(topBrand, BorderLayout.NORTH);

        // Center Nav Tabs
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setOpaque(false);

        navDashboardBtn = createSidebarNavButton("Dashboard", true);
        navAvailableFoodBtn = createSidebarNavButton("Available Food", false);
        navPickupTasksBtn = createSidebarNavButton("Pickup Tasks", false);
        navMyDeliveriesBtn = createSidebarNavButton("My Deliveries", false);
        navReportsBtn = createSidebarNavButton("Reports & Analytics", false);

        navDashboardBtn.addActionListener(e -> switchView("DASHBOARD"));
        navAvailableFoodBtn.addActionListener(e -> switchView("AVAILABLE_FOOD"));
        navPickupTasksBtn.addActionListener(e -> switchView("PICKUP_TASKS"));
        navMyDeliveriesBtn.addActionListener(e -> switchView("MY_DELIVERIES"));
        navReportsBtn.addActionListener(e -> switchView("REPORTS"));

        navPanel.add(navDashboardBtn);
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(navAvailableFoodBtn);
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(navPickupTasksBtn);
        navPanel.add(Box.createVerticalStrut(8));
        navPanel.add(navMyDeliveriesBtn);
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
            UITheme.fadeTransition(VolunteerDashboard.this, () -> new RoleSelectionFrame().setVisible(true));
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
        styleNavButton(navPickupTasksBtn, "PICKUP_TASKS".equals(viewName));
        styleNavButton(navMyDeliveriesBtn, "MY_DELIVERIES".equals(viewName));
        styleNavButton(navReportsBtn, "REPORTS".equals(viewName));

        loadAllData();
    }

    // =========================================================================
    // 1. DASHBOARD VIEW (KPI Cards + Quick Available Pickup Tasks)
    // =========================================================================
    private JPanel createDashboardView() {
        JPanel view = new JPanel(new BorderLayout());
        view.setOpaque(false);
        view.setBorder(new EmptyBorder(28, 36, 28, 36));

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        JLabel title = new JLabel("Welcome back, " + currentVolunteer.getName() + "!");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(UITheme.TEXT_DARK);

        JLabel sub = new JLabel("Active courier dispatch center. Find rescue tasks and deliver surplus meals safely.");
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

        completedDeliveriesValue = new JLabel("0");
        activeDeliveriesValue = new JLabel("0");
        availableTasksValue = new JLabel("0");
        hoursContributedValue = new JLabel("0.0 hrs");

        kpiGrid.add(createKpiCard("Deliveries Done", completedDeliveriesValue, "Safely reached community", new Color(0x72, 0x8C, 0x6E)));
        kpiGrid.add(createKpiCard("Active Pickups", activeDeliveriesValue, "In transit or assigned", new Color(0x6B, 0x1D, 0x2F)));
        kpiGrid.add(createKpiCard("Available Tasks", availableTasksValue, "Awaiting volunteer courier", new Color(0x8C, 0x6A, 0x3E)));
        kpiGrid.add(createKpiCard("Hours Contributed", hoursContributedValue, "Community service logged", new Color(0x5A, 0x47, 0x3E)));

        topSection.add(kpiGrid);
        topSection.add(Box.createVerticalStrut(28));
        view.add(topSection, BorderLayout.NORTH);

        // Center Quick Action Table Section (Open Pickup Tasks)
        JPanel centerSection = new JPanel(new BorderLayout());
        centerSection.setOpaque(false);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel quickTitle = new JLabel("Open Food Rescue Tasks Ready for Pickup");
        quickTitle.setFont(UITheme.FONT_SECTION);
        quickTitle.setForeground(UITheme.TEXT_DARK);

        JButton viewAllBtn = new JButton("View All Tasks & Accept ->");
        viewAllBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        viewAllBtn.setForeground(UITheme.PRIMARY);
        viewAllBtn.setContentAreaFilled(false);
        viewAllBtn.setFocusPainted(false);
        viewAllBtn.setBorderPainted(false);
        viewAllBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewAllBtn.addActionListener(e -> switchView("PICKUP_TASKS"));

        headerRow.add(quickTitle, BorderLayout.WEST);
        headerRow.add(viewAllBtn, BorderLayout.EAST);
        centerSection.add(headerRow, BorderLayout.NORTH);

        // Embed the shared pickup tasks table panel
        centerSection.add(createPickupTasksTablePanel(), BorderLayout.CENTER);

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
    // 2. AVAILABLE FOOD VIEW (Shared JTable + Filter + Search)
    // =========================================================================
    private JPanel createAvailableFoodView() {
        JPanel view = new JPanel(new BorderLayout());
        view.setOpaque(false);
        view.setBorder(new EmptyBorder(28, 36, 28, 36));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(0, 0, 16, 0));

        JLabel title = new JLabel("Live Available Food Listings");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(UITheme.TEXT_DARK);

        JLabel sub = new JLabel("Browse surplus food listings across the community. When claimed by NGOs, they appear in your Pickup Tasks.");
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

        // Control Toolbar: Filter Category + Search City + Refresh
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

        // Info Badge
        JLabel infoBadge = new JLabel("NGO partners claim listings, generating delivery tasks.");
        infoBadge.setFont(UITheme.FONT_SMALL);
        infoBadge.setForeground(UITheme.TEXT_MUTED);

        toolbar.add(catLbl);
        toolbar.add(categoryFilterCombo);
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(searchLbl);
        toolbar.add(citySearchField);
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(refreshBtn);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(infoBadge);

        panel.add(toolbar, BorderLayout.NORTH);

        // Available Food JTable
        String[] columns = {"ID", "Food Description", "Donor", "Category", "Quantity (Meals)", "Expires At", "Pickup Address", "Time Left"};
        availableTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        availableTable = new JTable(availableTableModel);
        styleTable(availableTable);
        availableTable.getColumnModel().getColumn(7).setCellRenderer(new CountdownCellRenderer());

        JScrollPane scrollPane = new JScrollPane(availableTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // =========================================================================
    // 3. PICKUP TASKS VIEW (Open Claims + "Accept Task" Action)
    // =========================================================================
    private JPanel createPickupTasksView() {
        JPanel view = new JPanel(new BorderLayout());
        view.setOpaque(false);
        view.setBorder(new EmptyBorder(28, 36, 28, 36));

        // Header Title
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(0, 0, 16, 0));

        JLabel title = new JLabel("Open Pickup & Delivery Tasks");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(UITheme.TEXT_DARK);

        JLabel sub = new JLabel("These donations have been claimed by partner NGOs. Select a task and accept it to take charge of the delivery.");
        sub.setFont(UITheme.FONT_SUBTITLE);
        sub.setForeground(UITheme.TEXT_MUTED);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(3));
        titlePanel.add(sub);

        view.add(titlePanel, BorderLayout.NORTH);
        view.add(createPickupTasksTablePanel(), BorderLayout.CENTER);

        return view;
    }

    private JPanel createPickupTasksTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        // Toolbar: Refresh + Accept Task Button
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        toolbar.setOpaque(false);

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

        acceptTaskBtn = new JButton("Accept Delivery Task");
        acceptTaskBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        acceptTaskBtn.setBackground(UITheme.PRIMARY);
        acceptTaskBtn.setForeground(Color.WHITE);
        acceptTaskBtn.setFocusPainted(false);
        acceptTaskBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        acceptTaskBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.PRIMARY_DARK, 1, true),
                new EmptyBorder(7, 16, 7, 16)
        ));
        acceptTaskBtn.setEnabled(false);
        acceptTaskBtn.addActionListener(e -> executeAcceptTask());

        toolbar.add(refreshBtn);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(acceptTaskBtn);

        panel.add(toolbar, BorderLayout.NORTH);

        // Pickup Tasks JTable
        String[] columns = {"Task ID", "Food Description", "Category", "Quantity", "Pickup Location (Donor)", "Dropoff Destination (NGO)", "Claimed Time", "Status"};
        pickupTasksTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        pickupTasksTable = new JTable(pickupTasksTableModel);
        styleTable(pickupTasksTable);
        pickupTasksTable.getColumnModel().getColumn(7).setCellRenderer(new ClaimStatusBadgeRenderer());

        pickupTasksTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                acceptTaskBtn.setEnabled(pickupTasksTable.getSelectedRow() >= 0);
            }
        });

        JScrollPane scrollPane = new JScrollPane(pickupTasksTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void executeAcceptTask() {
        int selectedRow = pickupTasksTable.getSelectedRow();
        if (selectedRow < 0 || cachedPickupTasks == null || selectedRow >= cachedPickupTasks.size()) {
            JOptionPane.showMessageDialog(this, "Please select an open pickup task to accept.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Claim selectedClaim = cachedPickupTasks.get(selectedRow);

        int confirm = JOptionPane.showConfirmDialog(this,
                "<html><div style='font-family: Segoe UI; width: 340px;'>" +
                        "<h3 style='color: #15803D; margin-bottom: 6px;'>Accept Rescue Delivery Task</h3>" +
                        "<p>Are you ready to deliver <b>" + selectedClaim.getFoodName() + "</b> (" + selectedClaim.getQuantity() + " meals)?</p>" +
                        "<p style='color: #475569;'><b>Pickup From:</b> " + selectedClaim.getDonorName() + "<br>" +
                        "<b>Address:</b> " + selectedClaim.getPickupAddress() + "</p>" +
                        "<p style='color: #475569;'><b>Deliver To:</b> " + selectedClaim.getNgoName() + "<br>" +
                        "<b>Address:</b> " + selectedClaim.getDropoffAddress() + "</p>" +
                        "</div></html>",
                "Confirm Task Assignment",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            acceptTaskBtn.setEnabled(false);
            acceptTaskBtn.setText("Assigning...");

            SwingUtilities.invokeLater(() -> {
                boolean success = donationService.acceptTask(selectedClaim.getId(), currentVolunteer.getId());
                acceptTaskBtn.setText("Accept Delivery Task");

                if (success) {
                    // Initialize Delivery mission record
                    deliveryService.getOrCreateDeliveryForClaim(selectedClaim.getId(), currentVolunteer.getId());

                    JOptionPane.showMessageDialog(this,
                            "<html><div style='font-family: Segoe UI; width: 320px;'>" +
                                    "<h3 style='color: #15803D; margin-bottom: 4px;'>Task Assigned Successfully!</h3>" +
                                    "<p style='color: #334155;'>You have taken responsibility for <b>" + selectedClaim.getFoodName() + "</b>.</p>" +
                                    "<p style='color: #64748B;'>Advance milestones one step at a time under <b>My Deliveries</b>.</p>" +
                                    "</div></html>",
                            "Assignment Confirmed",
                            JOptionPane.INFORMATION_MESSAGE);

                    loadAllData();
                    switchView("MY_DELIVERIES");
                } else {
                    JOptionPane.showMessageDialog(this,
                            "<html><div style='font-family: Segoe UI; width: 300px;'>" +
                                    "<h3 style='color: #DC2626; margin-bottom: 4px;'>Task Unavailable</h3>" +
                                    "<p style='color: #334155;'>This task has already been accepted by another volunteer courier or cancelled.</p>" +
                                    "</div></html>",
                            "Could Not Assign Task",
                            JOptionPane.WARNING_MESSAGE);
                    loadAllData();
                }
            });
        }
    }

    // =========================================================================
    // 4. MY DELIVERIES VIEW (Tracking + Status Stepper + Route Progression)
    // =========================================================================
    private JPanel createMyDeliveriesView() {
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

        JLabel title = new JLabel("My Deliveries & Route Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(UITheme.TEXT_DARK);

        JLabel sub = new JLabel("Select an active delivery below to view the 3-stage status stepper and advance route milestones.");
        sub.setFont(UITheme.FONT_SUBTITLE);
        sub.setForeground(UITheme.TEXT_MUTED);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(3));
        titlePanel.add(sub);

        topBar.add(titlePanel, BorderLayout.WEST);

        // Action Toolbar
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
                new EmptyBorder(6, 12, 6, 12)
        ));
        refreshBtn.addActionListener(e -> loadAllData());

        trackTimelineBtn = new JButton("View Live Timeline");
        trackTimelineBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        trackTimelineBtn.setBackground(UITheme.PRIMARY_BG_TINT);
        trackTimelineBtn.setForeground(UITheme.PRIMARY);
        trackTimelineBtn.setFocusPainted(false);
        trackTimelineBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        trackTimelineBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xBB, 0xF7, 0xD0), 1, true),
                new EmptyBorder(6, 14, 6, 14)
        ));
        trackTimelineBtn.setEnabled(false);
        trackTimelineBtn.addActionListener(e -> {
            int selectedRow = myDeliveriesTable.getSelectedRow();
            if (selectedRow >= 0 && cachedDeliveries != null && selectedRow < cachedDeliveries.size()) {
                Claim c = cachedDeliveries.get(selectedRow);
                DeliveryTrackingPanel.showLiveTimelineDialog(this, c.getDonationId(), c.getFoodName(), deliveryService);
            }
        });

        actionToolbar.add(refreshBtn);
        actionToolbar.add(trackTimelineBtn);

        topBar.add(actionToolbar, BorderLayout.EAST);
        view.add(topBar, BorderLayout.NORTH);

        // Center Content Box (Table on top, Stepper Card on bottom)
        JPanel centerBox = new JPanel(new BorderLayout(0, 16));
        centerBox.setOpaque(false);

        // My Deliveries JTable
        String[] columns = {"Claim ID", "Food Item", "Category", "Quantity", "Pickup From (Donor)", "Deliver To (NGO)", "Status", "Claimed At"};
        myDeliveriesTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        myDeliveriesTable = new JTable(myDeliveriesTableModel);
        styleTable(myDeliveriesTable);
        myDeliveriesTable.getColumnModel().getColumn(6).setCellRenderer(new ClaimStatusBadgeRenderer());

        JScrollPane scrollPane = new JScrollPane(myDeliveriesTable);
        scrollPane.setPreferredSize(new Dimension(0, 220));
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true));
        centerBox.add(scrollPane, BorderLayout.CENTER);

        // Stepper Panel on bottom
        deliveryStepperPanel = DeliveryTrackingPanel.createVolunteerStepper(null, deliveryService, this::loadAllData);
        centerBox.add(deliveryStepperPanel, BorderLayout.SOUTH);

        myDeliveriesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectedDeliveryInStepper();
            }
        });

        view.add(centerBox, BorderLayout.CENTER);
        return view;
    }

    private void updateSelectedDeliveryInStepper() {
        int selectedRow = myDeliveriesTable.getSelectedRow();
        if (selectedRow < 0 || cachedDeliveries == null || selectedRow >= cachedDeliveries.size()) {
            deliveryStepperPanel.setDelivery(null);
            trackTimelineBtn.setEnabled(false);
            return;
        }

        Claim claim = cachedDeliveries.get(selectedRow);
        trackTimelineBtn.setEnabled(true);

        new Thread(() -> {
            Delivery del = deliveryService.getOrCreateDeliveryForClaim(claim.getId(), currentVolunteer.getId());
            SwingUtilities.invokeLater(() -> {
                deliveryStepperPanel.setDelivery(del);
            });
        }).start();
    }

    // =========================================================================
    // TABLE STYLING & CUSTOM RENDERERS
    // =========================================================================
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
                    int badgeW = Math.min(w - 10, 125);
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

    // =========================================================================
    // DATA LOADING & REFRESH LOGIC
    // =========================================================================
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

            DonationService.VolunteerStats stats = donationService.getVolunteerStats(currentVolunteer.getId());
            cachedPickupTasks = donationService.getPickupTasks();
            cachedDeliveries = donationService.getDeliveriesByVolunteer(currentVolunteer.getId());

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
                completedDeliveriesValue.setText(String.valueOf(stats.getCompletedDeliveries()));
                activeDeliveriesValue.setText(String.valueOf(stats.getActivePickups()));
                availableTasksValue.setText(String.valueOf(stats.getAvailableTasks()));
                hoursContributedValue.setText(String.format("%.1f hrs", stats.getHoursContributed()));

                // 1. Available Table
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

                // 2. Pickup Tasks Table
                pickupTasksTableModel.setRowCount(0);
                for (Claim c : cachedPickupTasks) {
                    pickupTasksTableModel.addRow(new Object[]{
                            c.getId(),
                            formatBilingualFoodHtml(c.getFoodName(), null),
                            c.getCategory(),
                            c.getQuantity(),
                            c.getDonorName() + " (" + c.getPickupAddress() + ")",
                            c.getNgoName() + " (" + c.getDropoffAddress() + ")",
                            c.getFormattedClaimedAt(),
                            c.getStatus().getDisplayName()
                    });
                }

                // 3. My Deliveries Table
                myDeliveriesTableModel.setRowCount(0);
                for (Claim c : cachedDeliveries) {
                    myDeliveriesTableModel.addRow(new Object[]{
                            c.getId(),
                            formatBilingualFoodHtml(c.getFoodName(), null),
                            c.getCategory(),
                            c.getQuantity(),
                            c.getDonorName() + " (" + c.getPickupAddress() + ")",
                            c.getNgoName() + " (" + c.getDropoffAddress() + ")",
                            c.getStatus().getDisplayName(),
                            c.getFormattedClaimedAt()
                    });
                }

                if (myDeliveriesTableModel.getRowCount() > 0) {
                    if (myDeliveriesTable.getSelectedRow() < 0) {
                        myDeliveriesTable.setRowSelectionInterval(0, 0);
                    } else {
                        updateSelectedDeliveryInStepper();
                    }
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
