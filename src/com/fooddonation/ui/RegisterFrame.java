package com.fooddonation.ui;

import com.fooddonation.model.User;
import com.fooddonation.service.RegistrationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern Registration Frame for the Food Donation App.
 * Matches LoginFrame style (green/white palette, split layout, custom magic wand cursor, glitter trail).
 * Features:
 * - 3-Way Role Selector: Donor / NGO / Volunteer
 * - Common fields: Name, Email, Phone, Password, Confirm Password, City/Address
 * - Dynamic Role-Specific Fields (CardLayout):
 *     Donor: Type (Restaurant / Hotel / Individual)
 *     NGO: Registration Number, Service Area
 *     Volunteer: Vehicle Type (None / Bike / Car), Availability
 * - Real-Time Live Password Strength Bar
 * - Validation for empty fields, email format, 10-digit phone, password rules, and unique email
 * - Smooth fade and slide transition back to LoginFrame with prefilled email
 */
public class RegisterFrame extends JFrame {

    private final RegistrationService registrationService;
    private String selectedRole = "Donor"; // "Donor", "NGO", or "Volunteer"

    // Common Inputs
    private UITheme.RoundedTextField nameField;
    private UITheme.RoundedTextField emailField;
    private UITheme.RoundedTextField phoneField;
    private UITheme.RoundedTextField addressField;
    private UITheme.RoundedPasswordField passwordField;
    private UITheme.RoundedPasswordField confirmPasswordField;

    // Password Eye Toggles
    private boolean isPassVisible = false;
    private boolean isConfirmPassVisible = false;

    // Live Password Strength Bar
    private JPanel strengthMeterPanel;
    private JLabel strengthLabel;
    private int currentStrengthLevel = 0;
    private Color currentStrengthColor = new Color(0x94, 0xA3, 0xB8);

    // Role-Specific Dynamic Fields Container
    private JPanel dynamicFieldsContainer;
    private CardLayout dynamicCardLayout;

    // Donor Specific
    private JComboBox<String> donorTypeCombo;

    // NGO Specific
    private UITheme.RoundedTextField ngoRegNumberField;
    private UITheme.RoundedTextField ngoServiceAreaField;

    // Volunteer Specific
    private JComboBox<String> volunteerVehicleCombo;
    private JComboBox<String> volunteerAvailabilityCombo;

    // Toggles
    private JToggleButton donorToggle;
    private JToggleButton ngoToggle;
    private JToggleButton volunteerToggle;

    // Feedback
    private JPanel inlineErrorPanel;
    private JLabel inlineErrorLabel;
    private UITheme.RoundedButton submitButton;

    // Responsive Layout Panels
    private JPanel leftHeroPanel;
    private JPanel rightPanel;
    private JPanel formContainer;

    public RegisterFrame() {
        this(new RegistrationService());
    }

    public RegisterFrame(RegistrationService registrationService) {
        this.registrationService = registrationService;

        initWindow();
        initUI();
    }

    private void initWindow() {
        setTitle("ஈகை — Create Your Account");
        setSize(1040, 720);
        setMinimumSize(new Dimension(940, 640));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Attach Custom Magic Wand Cursor & Glitter Trail GlassPane
        GlitterTrailGlassPane glassPane = new GlitterTrailGlassPane(this);
        setGlassPane(glassPane);
        glassPane.setVisible(true);
    }

    private void initUI() {
        // Responsive 45% / 55% split that dynamically re-computes on window resize or maximize
        JPanel rootPanel = new JPanel() {
            @Override
            public void doLayout() {
                int w = getWidth();
                int h = getHeight();
                int leftW = (int) Math.round(w * 0.45);
                int rightW = w - leftW;
                if (leftHeroPanel != null) {
                    leftHeroPanel.setBounds(0, 0, leftW, h);
                }
                if (rightPanel != null) {
                    rightPanel.setBounds(leftW, 0, rightW, h);
                }
            }
        };
        rootPanel.setLayout(null);
        rootPanel.setBackground(UITheme.SURFACE_WHITE);

        // 1. Left Branding/Hero Panel (~45% width)
        leftHeroPanel = createLeftHeroPanel();

        // 2. Right Form Panel (~55% width)
        rightPanel = createRightScrollableForm();

        rootPanel.add(leftHeroPanel);
        rootPanel.add(rightPanel);

        setContentPane(rootPanel);
    }

    /**
     * Left Hero Panel matching the LoginFrame green aesthetic with community harvest artwork.
     */
    private JPanel createLeftHeroPanel() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                int w = getWidth();
                int h = getHeight();

                // Deep Burgundy Ambient Gradient
                GradientPaint bgGradient = new GradientPaint(
                        0, 0, new Color(0x4A, 0x15, 0x21), // Deep Burgundy
                        w, h, new Color(0x28, 0x0A, 0x11)  // Dark Wine Burgundy
                );
                g2.setPaint(bgGradient);
                g2.fillRect(0, 0, w, h);

                // Decorative Ambient Rings & Orbs
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
                g2.setColor(new Color(0x9B, 0xB3, 0x9A)); // Pastel Sage
                g2.fillOval(-80, -80, 360, 360);
                g2.fillOval(w - 220, h - 260, 320, 320);

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
                g2.setColor(Color.WHITE);
                g2.drawOval(-40, -40, 320, 320);
                g2.drawOval(w - 240, h - 280, 360, 360);

                g2.setComposite(AlphaComposite.SrcOver);

                // App Branding Header
                int brandY = 48;
                g2.setFont(UITheme.getTamilFont(Font.BOLD, 26));
                g2.setColor(Color.WHITE);
                g2.drawString("ஈகை", 48, brandY);

                g2.setFont(UITheme.getTamilFont(Font.BOLD, 13));
                g2.setColor(new Color(0x9B, 0xB3, 0x9A)); // Pastel Sage
                g2.drawString("பகுத்துண்டு பல்லுயிர் ஓம்புதல்", 50, brandY + 24);

                // Vector Artwork: Community Hands Cradling Harvest Bowl
                int cx = w / 2;
                int cy = h / 2 - 40;
                drawCommunityHarvestArt(g2, cx, cy);

                // Floating Glassmorphism Badges
                drawBadgePill(g2, 44, cy - 110, "3,500+ Community Partners");
                drawBadgePill(g2, w - 210, cy + 60, "100% Verified Impact");

                // Inspiring Mission Tagline at Bottom
                int textY = h - 140;
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                g2.setColor(Color.WHITE);
                g2.drawString("Turn Surplus Food", 48, textY);
                g2.drawString("into Community Smiles", 48, textY + 28);

                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                g2.setColor(new Color(0xF7, 0xF4, 0xEE)); // Warm Cream Beige
                g2.drawString("Join thousands of restaurants, verified NGOs, and volunteer couriers", 48, textY + 62);
                g2.drawString("making hunger history, one meal at a time.", 48, textY + 80);

                g2.dispose();
            }

            private void drawCommunityHarvestArt(Graphics2D g2, int cx, int cy) {
                // Outer glowing halo
                g2.setColor(new Color(255, 255, 255, 28));
                g2.fillOval(cx - 100, cy - 90, 200, 200);
                g2.setColor(new Color(255, 255, 255, 45));
                g2.fillOval(cx - 80, cy - 70, 160, 160);

                // Ceramic Serving Bowl Base
                int bowlW = 150;
                int bowlH = 65;
                int bowlX = cx - (bowlW / 2);
                int bowlY = cy + 10;

                g2.setColor(new Color(0xFE, 0xF3, 0xC7)); // Warm Porcelain
                g2.fillArc(bowlX, bowlY - 30, bowlW, bowlH + 30, 180, 180);
                g2.setColor(new Color(0xF5, 0x9E, 0x0B)); // Amber Rim
                g2.setStroke(new BasicStroke(2.2f));
                g2.drawArc(bowlX, bowlY - 30, bowlW, bowlH + 30, 180, 180);
                g2.drawLine(bowlX, bowlY, bowlX + bowlW, bowlY);

                // Bountiful Fresh Fruits & Greens inside Bowl
                // 1. Red Apple
                g2.setColor(new Color(0xEF, 0x44, 0x44));
                g2.fillOval(cx - 36, cy - 25, 38, 38);
                // Apple Leaf
                g2.setColor(new Color(0x10, 0xB9, 0x81));
                g2.fillOval(cx - 24, cy - 35, 14, 8);

                // 2. Golden Pear
                g2.setColor(new Color(0xFA, 0xCC, 0x15));
                g2.fillOval(cx + 2, cy - 32, 34, 42);

                // 3. Crisp Green Grapes / Vegetables
                g2.setColor(new Color(0x34, 0xD3, 0x99));
                g2.fillOval(cx - 12, cy - 10, 20, 20);
                g2.fillOval(cx + 4, cy - 8, 20, 20);
                g2.fillOval(cx - 4, cy + 2, 18, 18);

                // 4. Central Radiating Heart of Giving
                Path2D.Float heart = new Path2D.Float();
                int hx = cx;
                int hy = bowlY + 24;
                int s = 14;
                heart.moveTo(hx, hy + s * 0.7f);
                heart.curveTo(hx - s * 1.1f, hy, hx - s * 0.9f, hy - s * 0.8f, hx, hy - s * 0.2f);
                heart.curveTo(hx + s * 0.9f, hy - s * 0.8f, hx + s * 1.1f, hy, hx, hy + s * 0.7f);
                heart.closePath();

                g2.setColor(new Color(255, 255, 255, 120));
                g2.fillOval(hx - s - 4, hy - s - 2, (s + 4) * 2, (s + 4) * 2);
                g2.setColor(new Color(0xDC, 0x26, 0x26));
                g2.fill(heart);
            }

            private void drawBadgePill(Graphics2D g2, int px, int py, String text) {
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int pillW = fm.stringWidth(text) + 26;
                int pillH = 30;

                g2.setColor(new Color(255, 255, 255, 38));
                g2.fillRoundRect(px, py, pillW, pillH, 15, 15);
                g2.setColor(new Color(255, 255, 255, 75));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(px, py, pillW, pillH, 15, 15);

                g2.setColor(Color.WHITE);
                g2.drawString(text, px + 13, py + 20);
            }
        };
    }

    /**
     * Scrollable panel that enforces viewport-width tracking, disabling horizontal scroll.
     */
    private static class ScrollableWidthPanel extends JPanel implements Scrollable {
        public ScrollableWidthPanel() {
            super();
            setBackground(UITheme.SURFACE_WHITE);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true; // Enforces fitting to the viewport width!
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * Right Form Panel housed inside a smooth, responsive JScrollPane with horizontal scroll disabled.
     */
    private JPanel createRightScrollableForm() {
        formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setOpaque(false);

        // Header Title
        // Back to Role Selection Hub link
        JLabel backLink = new JLabel("<html>&larr; <u>Back to Portal Selection</u></html>");
        backLink.setFont(UITheme.FONT_SMALL);
        backLink.setForeground(UITheme.TEXT_MUTED);
        backLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backLink.setAlignmentX(Component.LEFT_ALIGNMENT);
        backLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UITheme.fadeTransition(RegisterFrame.this, () -> new RoleSelectionFrame().setVisible(true));
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                backLink.setForeground(UITheme.PRIMARY);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                backLink.setForeground(UITheme.TEXT_MUTED);
            }
        });
        formContainer.add(backLink);
        formContainer.add(Box.createVerticalStrut(12));

        JLabel titleLabel = new JLabel("Create Your Account");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_DARK);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subTitleLabel = new JLabel("Join our verified food rescue ecosystem. It only takes a minute.");
        subTitleLabel.setFont(UITheme.FONT_SUBTITLE);
        subTitleLabel.setForeground(UITheme.TEXT_MUTED);
        subTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        formContainer.add(titleLabel);
        formContainer.add(Box.createVerticalStrut(4));
        formContainer.add(subTitleLabel);
        formContainer.add(Box.createVerticalStrut(18));

        // --- 3-Way Role Selector: Donor / NGO / Volunteer ---
        JLabel roleLabel = new JLabel("SELECT YOUR ACCOUNT ROLE");
        roleLabel.setFont(UITheme.FONT_LABEL);
        roleLabel.setForeground(UITheme.TEXT_MUTED);
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(roleLabel);
        formContainer.add(Box.createVerticalStrut(8));

        JPanel roleTogglePanel = create3WayRoleToggle();
        roleTogglePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(roleTogglePanel);
        formContainer.add(Box.createVerticalStrut(14));

        // --- Inline Error Alert Banner ---
        inlineErrorPanel = createInlineErrorPanel();
        inlineErrorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        inlineErrorPanel.setVisible(false);
        formContainer.add(inlineErrorPanel);
        formContainer.add(Box.createVerticalStrut(10));

        // --- Common Field: Full Name / Org Name ---
        JLabel nameLbl = new JLabel("Full Name / Organization Name *");
        nameLbl.setFont(UITheme.FONT_LABEL);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(nameLbl);
        formContainer.add(Box.createVerticalStrut(5));

        nameField = new UITheme.RoundedTextField("e.g. Green Garden Bistro or Jane Doe");
        styleInput(nameField);
        formContainer.add(nameField);
        formContainer.add(Box.createVerticalStrut(12));

        // --- Common Fields Grid: Email & Phone ---
        JPanel emailPhoneGrid = new JPanel(new GridLayout(1, 2, 12, 0));
        emailPhoneGrid.setOpaque(false);
        emailPhoneGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        emailPhoneGrid.setPreferredSize(new Dimension(0, 68));
        emailPhoneGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Email Column
        JPanel emailCol = new JPanel();
        emailCol.setLayout(new BoxLayout(emailCol, BoxLayout.Y_AXIS));
        emailCol.setOpaque(false);
        JLabel emailLbl = new JLabel("Email Address *");
        emailLbl.setFont(UITheme.FONT_LABEL);
        emailLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        emailField = new UITheme.RoundedTextField("e.g. contact@bistro.com");
        styleInput(emailField);
        emailCol.add(emailLbl);
        emailCol.add(Box.createVerticalStrut(5));
        emailCol.add(emailField);

        // Phone Column
        JPanel phoneCol = new JPanel();
        phoneCol.setLayout(new BoxLayout(phoneCol, BoxLayout.Y_AXIS));
        phoneCol.setOpaque(false);
        JLabel phoneLbl = new JLabel("Phone (10 Digits) *");
        phoneLbl.setFont(UITheme.FONT_LABEL);
        phoneLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        phoneField = new UITheme.RoundedTextField("e.g. 9876543210");
        styleInput(phoneField);
        phoneCol.add(phoneLbl);
        phoneCol.add(Box.createVerticalStrut(5));
        phoneCol.add(phoneField);

        emailPhoneGrid.add(emailCol);
        emailPhoneGrid.add(phoneCol);
        formContainer.add(emailPhoneGrid);
        formContainer.add(Box.createVerticalStrut(12));

        // --- Common Field: City / Address ---
        JLabel addrLbl = new JLabel("City / Operating Address *");
        addrLbl.setFont(UITheme.FONT_LABEL);
        addrLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(addrLbl);
        formContainer.add(Box.createVerticalStrut(5));

        addressField = new UITheme.RoundedTextField("e.g. 124 Culinary Blvd, Downtown District");
        styleInput(addressField);
        formContainer.add(addressField);
        formContainer.add(Box.createVerticalStrut(14));

        // --- Dynamic Role-Specific Fields Container ---
        dynamicCardLayout = new CardLayout();
        dynamicFieldsContainer = new JPanel(dynamicCardLayout);
        dynamicFieldsContainer.setOpaque(false);
        dynamicFieldsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        dynamicFieldsContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        dynamicFieldsContainer.setPreferredSize(new Dimension(0, 74));

        dynamicFieldsContainer.add(createDonorFieldsPanel(), "Donor");
        dynamicFieldsContainer.add(createNGOFieldsPanel(), "NGO");
        dynamicFieldsContainer.add(createVolunteerFieldsPanel(), "Volunteer");

        formContainer.add(dynamicFieldsContainer);
        formContainer.add(Box.createVerticalStrut(14));

        // --- Common Field: Password with Live Strength Bar ---
        JLabel passLbl = new JLabel("Password * (≥ 8 chars, letter + digit)");
        passLbl.setFont(UITheme.FONT_LABEL);
        passLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(passLbl);
        formContainer.add(Box.createVerticalStrut(5));

        JPanel passWrapper = createPasswordInputWrapper(false);
        passWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(passWrapper);
        formContainer.add(Box.createVerticalStrut(6));

        // Live Password Strength Bar Component
        JPanel strengthRow = createPasswordStrengthMeter();
        strengthRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(strengthRow);
        formContainer.add(Box.createVerticalStrut(12));

        // --- Common Field: Confirm Password ---
        JLabel confirmPassLbl = new JLabel("Confirm Password *");
        confirmPassLbl.setFont(UITheme.FONT_LABEL);
        confirmPassLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(confirmPassLbl);
        formContainer.add(Box.createVerticalStrut(5));

        JPanel confirmPassWrapper = createPasswordInputWrapper(true);
        confirmPassWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(confirmPassWrapper);
        formContainer.add(Box.createVerticalStrut(20));

        // --- Register Action Button ---
        submitButton = new UITheme.RoundedButton("Create " + selectedRole + " Account");
        submitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        submitButton.setPreferredSize(new Dimension(0, 46));
        submitButton.addActionListener(e -> executeRegistration());
        formContainer.add(submitButton);
        formContainer.add(Box.createVerticalStrut(14));

        // --- Footer Link: Already have an account? Sign In ---
        JPanel signInFooter = createSignInFooter();
        signInFooter.setAlignmentX(Component.CENTER_ALIGNMENT);
        formContainer.add(signInFooter);

        // Responsive scroll content: max width ~520px, centered, 40px padding on left and right
        ScrollableWidthPanel scrollContent = new ScrollableWidthPanel() {
            private static final int MAX_FORM_WIDTH = 520;
            private static final int HORIZONTAL_PADDING = 40;
            private static final int TOP_PADDING = 32;
            private static final int BOTTOM_PADDING = 40;

            @Override
            public Dimension getPreferredSize() {
                int formH = (formContainer != null) ? formContainer.getPreferredSize().height : 700;
                return new Dimension(MAX_FORM_WIDTH, formH + TOP_PADDING + BOTTOM_PADDING);
            }

            @Override
            public void doLayout() {
                if (formContainer == null) return;
                int availW = getWidth();
                int targetW = Math.min(MAX_FORM_WIDTH, Math.max(260, availW - (HORIZONTAL_PADDING * 2)));
                int formX = Math.max(0, (availW - targetW) / 2);
                int formY = TOP_PADDING;
                int formH = formContainer.getPreferredSize().height;
                formContainer.setBounds(formX, formY, targetW, formH);
            }
        };
        scrollContent.setLayout(null);
        scrollContent.add(formContainer);

        // Put in clean JScrollPane with horizontal scrolling disabled
        JScrollPane scrollPane = new JScrollPane(scrollContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(UITheme.SURFACE_WHITE);

        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(UITheme.SURFACE_WHITE);
        outer.add(scrollPane, BorderLayout.CENTER);
        return outer;
    }

    /**
     * 3-Way Role Selector: Donor / NGO / Volunteer.
     */
    private JPanel create3WayRoleToggle() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 6, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_OFFWHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(UITheme.BORDER_GRAY);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 44));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new EmptyBorder(3, 3, 3, 3));

        ButtonGroup group = new ButtonGroup();
        donorToggle = new JToggleButton("Donor", true);
        ngoToggle = new JToggleButton("NGO", false);
        volunteerToggle = new JToggleButton("Volunteer", false);

        styleRoleToggle(donorToggle, true);
        styleRoleToggle(ngoToggle, false);
        styleRoleToggle(volunteerToggle, false);

        group.add(donorToggle);
        group.add(ngoToggle);
        group.add(volunteerToggle);

        donorToggle.addActionListener(e -> selectRole("Donor"));
        ngoToggle.addActionListener(e -> selectRole("NGO"));
        volunteerToggle.addActionListener(e -> selectRole("Volunteer"));

        panel.add(donorToggle);
        panel.add(ngoToggle);
        panel.add(volunteerToggle);

        return panel;
    }

    private void selectRole(String role) {
        this.selectedRole = role;
        styleRoleToggle(donorToggle, "Donor".equalsIgnoreCase(role));
        styleRoleToggle(ngoToggle, "NGO".equalsIgnoreCase(role));
        styleRoleToggle(volunteerToggle, "Volunteer".equalsIgnoreCase(role));

        // Switch dynamic role fields view
        dynamicCardLayout.show(dynamicFieldsContainer, role);

        submitButton.setText("Create " + role + " Account");
        clearInlineError();
        if (formContainer != null) {
            formContainer.revalidate();
            formContainer.repaint();
            if (formContainer.getParent() != null) {
                formContainer.getParent().revalidate();
                formContainer.getParent().repaint();
            }
        }
        if (rightPanel != null) {
            rightPanel.revalidate();
            rightPanel.repaint();
        }
    }

    private void styleRoleToggle(JToggleButton btn, boolean active) {
        btn.setFont(UITheme.FONT_BUTTON);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                AbstractButton b = (AbstractButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (b.isSelected()) {
                    g2.setColor(UITheme.PRIMARY);
                    g2.fill(new RoundRectangle2D.Float(0, 0, b.getWidth(), b.getHeight(), 10, 10));
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(UITheme.BG_OFFWHITE);
                    g2.fill(new RoundRectangle2D.Float(0, 0, b.getWidth(), b.getHeight(), 10, 10));
                    g2.setColor(UITheme.TEXT_MUTED);
                }

                FontMetrics fm = g2.getFontMetrics();
                Rectangle bounds = fm.getStringBounds(b.getText(), g2).getBounds();
                int x = (b.getWidth() - bounds.width) / 2;
                int y = (b.getHeight() - bounds.height) / 2 + fm.getAscent();
                g2.drawString(b.getText(), x, y);
                g2.dispose();
            }
        });
        btn.repaint();
    }

    /**
     * Dynamic Fields for Donor: Type (Restaurant / Hotel / Individual).
     */
    private JPanel createDonorFieldsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel typeLbl = new JLabel("Donor Classification *");
        typeLbl.setFont(UITheme.FONT_LABEL);
        typeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        donorTypeCombo = new JComboBox<>(com.fooddonation.model.Donor.BILINGUAL_DONOR_TYPES);
        donorTypeCombo.setFont(UITheme.FONT_INPUT);
        donorTypeCombo.setBackground(UITheme.SURFACE_WHITE);
        donorTypeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        donorTypeCombo.setPreferredSize(new Dimension(0, 40));
        donorTypeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(typeLbl);
        p.add(Box.createVerticalStrut(5));
        p.add(donorTypeCombo);
        return p;
    }

    /**
     * Dynamic Fields for NGO: Registration Number, Service Area.
     */
    private JPanel createNGOFieldsPanel() {
        JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(0, 68));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Reg Number
        JPanel col1 = new JPanel();
        col1.setLayout(new BoxLayout(col1, BoxLayout.Y_AXIS));
        col1.setOpaque(false);
        JLabel regLbl = new JLabel("Registration / Tax ID *");
        regLbl.setFont(UITheme.FONT_LABEL);
        regLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        ngoRegNumberField = new UITheme.RoundedTextField("e.g. NGO-501C-9842");
        styleInput(ngoRegNumberField);
        col1.add(regLbl);
        col1.add(Box.createVerticalStrut(5));
        col1.add(ngoRegNumberField);

        // Service Area
        JPanel col2 = new JPanel();
        col2.setLayout(new BoxLayout(col2, BoxLayout.Y_AXIS));
        col2.setOpaque(false);
        JLabel areaLbl = new JLabel("Service Area / District *");
        areaLbl.setFont(UITheme.FONT_LABEL);
        areaLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        ngoServiceAreaField = new UITheme.RoundedTextField("e.g. Greater Metro & Shelters");
        styleInput(ngoServiceAreaField);
        col2.add(areaLbl);
        col2.add(Box.createVerticalStrut(5));
        col2.add(ngoServiceAreaField);

        p.add(col1);
        p.add(col2);
        return p;
    }

    /**
     * Dynamic Fields for Volunteer: Vehicle Type, Availability.
     */
    private JPanel createVolunteerFieldsPanel() {
        JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(0, 68));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Vehicle Type
        JPanel col1 = new JPanel();
        col1.setLayout(new BoxLayout(col1, BoxLayout.Y_AXIS));
        col1.setOpaque(false);
        JLabel vLbl = new JLabel("Vehicle Type *");
        vLbl.setFont(UITheme.FONT_LABEL);
        vLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        volunteerVehicleCombo = new JComboBox<>(new String[]{"Car", "Bike", "None"});
        volunteerVehicleCombo.setFont(UITheme.FONT_INPUT);
        volunteerVehicleCombo.setBackground(UITheme.SURFACE_WHITE);
        volunteerVehicleCombo.setPreferredSize(new Dimension(0, 40));
        volunteerVehicleCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        volunteerVehicleCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        col1.add(vLbl);
        col1.add(Box.createVerticalStrut(5));
        col1.add(volunteerVehicleCombo);

        // Availability
        JPanel col2 = new JPanel();
        col2.setLayout(new BoxLayout(col2, BoxLayout.Y_AXIS));
        col2.setOpaque(false);
        JLabel aLbl = new JLabel("Availability Schedule *");
        aLbl.setFont(UITheme.FONT_LABEL);
        aLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        volunteerAvailabilityCombo = new JComboBox<>(new String[]{
                "Flexible (Anytime)", "Weekdays Only", "Weekends Only", "Evenings (After 5 PM)"
        });
        volunteerAvailabilityCombo.setFont(UITheme.FONT_INPUT);
        volunteerAvailabilityCombo.setBackground(UITheme.SURFACE_WHITE);
        volunteerAvailabilityCombo.setPreferredSize(new Dimension(0, 40));
        volunteerAvailabilityCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        volunteerAvailabilityCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        col2.add(aLbl);
        col2.add(Box.createVerticalStrut(5));
        col2.add(volunteerAvailabilityCombo);

        p.add(col1);
        p.add(col2);
        return p;
    }

    /**
     * Builds Password input wrapper with integrated Eye Show/Hide button.
     */
    private JPanel createPasswordInputWrapper(boolean isConfirm) {
        UITheme.RoundedPasswordField pField = new UITheme.RoundedPasswordField(
                isConfirm ? "Re-enter your password" : "At least 8 chars, letter + digit"
        );

        if (isConfirm) {
            confirmPasswordField = pField;
        } else {
            passwordField = pField;
            // Listen for keystrokes to update live password strength bar
            passwordField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) { updateStrength(); }
                @Override
                public void removeUpdate(DocumentEvent e) { updateStrength(); }
                @Override
                public void changedUpdate(DocumentEvent e) { updateStrength(); }
            });
        }

        pField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) { clearInlineError(); }
        });

        // Eye Button
        JButton eyeBtn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                boolean visible = isConfirm ? isConfirmPassVisible : isPassVisible;
                g2.setColor(visible ? UITheme.PRIMARY : UITheme.TEXT_LIGHT);
                g2.setStroke(new BasicStroke(1.6f));

                Path2D.Float eye = new Path2D.Float();
                eye.moveTo(6, h / 2f);
                eye.quadTo(w / 2f, 7, w - 6, h / 2f);
                eye.quadTo(w / 2f, h - 7, 6, h / 2f);
                eye.closePath();
                g2.draw(eye);

                g2.fillOval((w / 2) - 3, (h / 2) - 3, 6, 6);

                if (!visible) {
                    g2.setColor(UITheme.TEXT_MUTED);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawLine(8, h - 8, w - 8, 8);
                }
                g2.dispose();
            }
        };
        eyeBtn.setOpaque(false);
        eyeBtn.setContentAreaFilled(false);
        eyeBtn.setBorderPainted(false);
        eyeBtn.setFocusPainted(false);
        eyeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        eyeBtn.addActionListener(e -> {
            if (isConfirm) {
                isConfirmPassVisible = !isConfirmPassVisible;
                confirmPasswordField.setEchoChar(isConfirmPassVisible ? (char) 0 : '•');
            } else {
                isPassVisible = !isPassVisible;
                passwordField.setEchoChar(isPassVisible ? (char) 0 : '•');
            }
            eyeBtn.repaint();
            pField.requestFocusInWindow();
        });

        JPanel wrapper = new JPanel(null) {
            @Override
            public void doLayout() {
                int w = getWidth();
                int h = getHeight();
                pField.setBounds(0, 0, w, h);
                eyeBtn.setBounds(Math.max(0, w - 38), (h - 30) / 2, 30, 30);
            }
        };
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(0, 42));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        wrapper.add(eyeBtn);
        wrapper.add(pField);

        return wrapper;
    }

    /**
     * Builds the Live Password Strength Bar component.
     */
    private JPanel createPasswordStrengthMeter() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 18));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        strengthMeterPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int gap = 4;
                int totalSegments = 4;
                int segWidth = (w - (gap * (totalSegments - 1))) / totalSegments;

                for (int i = 0; i < totalSegments; i++) {
                    int x = i * (segWidth + gap);
                    if (i < currentStrengthLevel) {
                        g2.setColor(currentStrengthColor);
                    } else {
                        g2.setColor(new Color(0xE2, 0xE8, 0xF0)); // Slate 200
                    }
                    g2.fill(new RoundRectangle2D.Float(x, 2, segWidth, h - 4, 4, 4));
                }

                g2.dispose();
            }
        };
        strengthMeterPanel.setOpaque(false);
        strengthMeterPanel.setPreferredSize(new Dimension(220, 16));

        strengthLabel = new JLabel("Enter password");
        strengthLabel.setFont(UITheme.FONT_SMALL);
        strengthLabel.setForeground(UITheme.TEXT_MUTED);

        panel.add(strengthMeterPanel, BorderLayout.CENTER);
        panel.add(strengthLabel, BorderLayout.EAST);

        return panel;
    }

    private void updateStrength() {
        String pass = new String(passwordField.getPassword());
        RegistrationService.PasswordStrength str = RegistrationService.assessPasswordStrength(pass);
        this.currentStrengthLevel = str.getLevel();
        this.currentStrengthColor = str.getColor();
        strengthLabel.setText(str.getLabel());
        strengthLabel.setForeground(str.getColor());
        strengthMeterPanel.repaint();
    }

    /**
     * Builds Inline Error Alert Banner.
     */
    private JPanel createInlineErrorPanel() {
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
        panel.setPreferredSize(new Dimension(0, 42));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent iconLabel = new JComponent() {
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

        inlineErrorLabel = new JLabel("");
        inlineErrorLabel.setFont(UITheme.FONT_SMALL);
        inlineErrorLabel.setForeground(UITheme.ERROR_TEXT);

        panel.add(iconLabel, BorderLayout.WEST);
        panel.add(inlineErrorLabel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSignInFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        footer.setOpaque(false);

        JLabel prompt = new JLabel("Already have an account?");
        prompt.setFont(UITheme.FONT_SUBTITLE);
        prompt.setForeground(UITheme.TEXT_MUTED);

        JLabel signInLink = new JLabel("<html><u><b>Sign In here</b></u></html>");
        signInLink.setFont(UITheme.FONT_SUBTITLE);
        signInLink.setForeground(UITheme.PRIMARY);
        signInLink.setCursor(new Cursor(Cursor.HAND_CURSOR));

        signInLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                transitionToLogin(null, selectedRole);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                signInLink.setForeground(UITheme.PRIMARY_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                signInLink.setForeground(UITheme.PRIMARY);
            }
        });

        footer.add(prompt);
        footer.add(signInLink);
        return footer;
    }

    private void styleInput(JTextField tf) {
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        tf.setPreferredSize(new Dimension(0, 42));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        tf.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) { clearInlineError(); }
        });
    }

    /**
     * Executes Registration using RegistrationService.
     */
    private void executeRegistration() {
        clearInlineError();

        String name = nameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String address = addressField.getText();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        String donorType = (donorTypeCombo != null) ? (String) donorTypeCombo.getSelectedItem() : "Restaurant";
        String ngoReg = (ngoRegNumberField != null) ? ngoRegNumberField.getText() : "";
        String ngoArea = (ngoServiceAreaField != null) ? ngoServiceAreaField.getText() : "";
        String volVehicle = (volunteerVehicleCombo != null) ? (String) volunteerVehicleCombo.getSelectedItem() : "Car";
        String volAvail = (volunteerAvailabilityCombo != null) ? (String) volunteerAvailabilityCombo.getSelectedItem() : "Flexible";

        submitButton.setEnabled(false);
        submitButton.setText("Creating Account...");

        SwingUtilities.invokeLater(() -> {
            try {
                RegistrationService.RegistrationResult result = registrationService.register(
                        selectedRole, name, email, phone, address,
                        password, confirmPassword,
                        donorType, ngoReg, ngoArea,
                        volVehicle, volAvail
                );

                if (result.isSuccess()) {
                    User newUser = result.getUser();

                    // Show success confirmation popup
                    JOptionPane.showMessageDialog(this,
                            "<html><div style='width: 320px; font-family: Segoe UI;'>" +
                                    "<h2 style='color: #15803D; margin-bottom: 4px;'>Registration Successful!</h2>" +
                                    "<p style='color: #334155;'>Welcome to Eegai (ஈகை), <b>" + newUser.getName() + "</b>!</p>" +
                                    "<p style='color: #64748B;'>Your " + newUser.getRole() + " account is verified. You will now be redirected to the sign-in screen.</p>" +
                                    "</div></html>",
                            "Account Created",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Soft fade and slide transition back to LoginFrame with prefilled email
                    transitionToLogin(newUser.getEmail(), newUser.getRole());

                } else {
                    showInlineError(result.getErrorMessage());
                    submitButton.setEnabled(true);
                    submitButton.setText("Create " + selectedRole + " Account");
                }
            } catch (Exception ex) {
                showInlineError("Registration error: " + ex.getMessage());
                submitButton.setEnabled(true);
                submitButton.setText("Create " + selectedRole + " Account");
            }
        });
    }

    private void showInlineError(String msg) {
        inlineErrorLabel.setText("<html>" + msg + "</html>");
        inlineErrorPanel.setVisible(true);
        inlineErrorPanel.revalidate();
        inlineErrorPanel.repaint();
        if (formContainer != null) {
            formContainer.revalidate();
            if (formContainer.getParent() != null) {
                formContainer.getParent().revalidate();
            }
        }
    }

    private void clearInlineError() {
        if (inlineErrorPanel.isVisible()) {
            inlineErrorPanel.setVisible(false);
            inlineErrorPanel.revalidate();
            inlineErrorPanel.repaint();
            if (formContainer != null) {
                formContainer.revalidate();
                if (formContainer.getParent() != null) {
                    formContainer.getParent().revalidate();
                }
            }
        }
    }

    /**
     * Performs a smooth soft fade and slide transition back to LoginFrame
     * prefilling the registered email and role.
     */
    private void transitionToLogin(String prefillEmail, String prefillRole) {
        Timer fadeTimer = new Timer(15, null);
        final float[] opacity = {1.0f};

        fadeTimer.addActionListener(e -> {
            opacity[0] -= 0.08f;
            if (opacity[0] <= 0.05f) {
                fadeTimer.stop();
                dispose();
                SwingUtilities.invokeLater(() -> {
                    if ("Donor".equalsIgnoreCase(prefillRole)) {
                        DonorLoginFrame donorFrame = new DonorLoginFrame(prefillEmail);
                        donorFrame.setVisible(true);
                    } else {
                        LoginFrame loginFrame = new LoginFrame(prefillEmail, prefillRole);
                        loginFrame.setVisible(true);
                    }
                });
            } else {
                try {
                    setOpacity(Math.max(0.0f, opacity[0]));
                } catch (Exception ex) {
                    fadeTimer.stop();
                    dispose();
                    if ("Donor".equalsIgnoreCase(prefillRole)) {
                        DonorLoginFrame donorFrame = new DonorLoginFrame(prefillEmail);
                        donorFrame.setVisible(true);
                    } else {
                        LoginFrame loginFrame = new LoginFrame(prefillEmail, prefillRole);
                        loginFrame.setVisible(true);
                    }
                }
            }
        });
        fadeTimer.start();
    }
}
