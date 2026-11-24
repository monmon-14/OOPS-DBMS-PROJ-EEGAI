package com.fooddonation.ui;

import com.fooddonation.model.User;
import com.fooddonation.service.AuthService;
import com.fooddonation.service.UserSession;
import com.fooddonation.util.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Dedicated Login Screen for NGO and Volunteer Courier portals.
 * Features:
 * - Direct, role-specific layout for NGO and Volunteer (no redundant 3-way toggle bar)
 * - Dynamic role theming: headers, badges, placeholders, and demo pills update based on selected role
 * - Seamless switch link between NGO and Volunteer Courier modes
 * - Back to Portal Selection link
 * - Clean, elegant green/white palette with rounded inputs and soft shadows
 * - Split layout with custom-painted food-donation illustration and tagline
 * - Email and password fields with inline show/hide password toggle
 * - Instant inline error feedback for missing fields, invalid credentials, and role mismatch
 * - Soft fade and slide transition animation upon successful authentication or navigating to RegisterFrame
 * - Custom magic wand cursor with interactive glitter/sparkle particle trail
 */
public class LoginFrame extends JFrame {

    private final AuthService authService;
    private String selectedRole = "NGO"; // "NGO" or "Volunteer" (or "Donor")

    // Dynamic Role Labels and UI Components
    private JLabel badgeLabel;
    private JLabel headerLabel;
    private JLabel subHeaderLabel;
    private JLabel emailLabel;
    private UITheme.RoundedTextField emailField;
    private UITheme.RoundedPasswordField passwordField;
    private JButton eyeButton;
    private boolean isPasswordVisible = false;
    private JPanel inlineErrorPanel;
    private JLabel inlineErrorLabel;
    private JPanel quickFillPanel;
    private UITheme.RoundedButton loginButton;
    private JLabel switchPromptLabel;
    private JLabel switchActionLink;
    private JPanel rightCardPanel;

    private String initialEmail = null;
    private String initialRole = "NGO";

    public LoginFrame() {
        this(new AuthService(), null, "NGO");
    }

    public LoginFrame(String prefilledEmail, String prefilledRole) {
        this(new AuthService(), prefilledEmail, prefilledRole);
    }

    public LoginFrame(AuthService authService) {
        this(authService, null, "NGO");
    }

    public LoginFrame(AuthService authService, String prefilledEmail, String prefilledRole) {
        this.authService = authService;
        this.initialEmail = prefilledEmail;
        this.initialRole = (prefilledRole != null && !prefilledRole.trim().isEmpty()) ? prefilledRole.trim() : "NGO";
        this.selectedRole = this.initialRole;

        // Initialize Database schema and seed data asynchronously
        new Thread(DBConnection::initializeDatabase).start();

        initWindow();
        initUI();

        // Apply prefill if provided
        if (initialEmail != null && !initialEmail.isEmpty()) {
            emailField.setText(initialEmail);
            passwordField.requestFocusInWindow();
        }
    }

    private void initWindow() {
        updateWindowTitle();
        setSize(980, 650);
        setMinimumSize(new Dimension(880, 580));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Apply Custom Magic Wand Cursor & Glitter Trail Glass Pane
        GlitterTrailGlassPane glassPane = new GlitterTrailGlassPane(this);
        setGlassPane(glassPane);
        glassPane.setVisible(true);
    }

    private void updateWindowTitle() {
        if ("Volunteer".equalsIgnoreCase(selectedRole)) {
            setTitle("ஈகை — Volunteer Courier Login");
        } else if ("Donor".equalsIgnoreCase(selectedRole)) {
            setTitle("ஈகை — Food Donor Portal Login");
        } else {
            setTitle("ஈகை — NGO & Shelter Portal Login");
        }
    }

    private void initUI() {
        JPanel rootPanel = new JPanel(new GridLayout(1, 2, 0, 0));
        rootPanel.setBackground(UITheme.SURFACE_WHITE);

        // 1. Left Panel: Vector Illustration & Tagline
        JPanel leftHeroPanel = createLeftHeroPanel();

        // 2. Right Panel: Role-Specific Login Form Card
        rightCardPanel = createRightFormPanel();

        rootPanel.add(leftHeroPanel);
        rootPanel.add(rightCardPanel);

        setContentPane(rootPanel);
    }

    /**
     * Creates the Left Hero Panel with vector food-donation illustration, badges, and tagline.
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

                // Rich Deep Burgundy Gradient
                GradientPaint bgGradient = new GradientPaint(
                        0, 0, new Color(0x4A, 0x15, 0x21), // Deep Burgundy
                        w, h, new Color(0x28, 0x0A, 0x11)  // Dark Wine Burgundy
                );
                g2.setPaint(bgGradient);
                g2.fillRect(0, 0, w, h);

                // Decorative Ambient Glow Orbs
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
                g2.setColor(new Color(0x9B, 0xB3, 0x9A)); // Pastel Sage
                g2.fillOval(-60, -60, 320, 320);
                g2.fillOval(w - 200, h - 220, 280, 280);

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
                g2.setColor(Color.WHITE);
                g2.drawOval(-40, -40, 280, 280);
                g2.drawOval(w - 220, h - 240, 320, 320);

                // Reset Composite
                g2.setComposite(AlphaComposite.SrcOver);

                // Draw App Branding
                int brandY = 48;
                g2.setFont(UITheme.getTamilFont(Font.BOLD, 26));
                g2.setColor(Color.WHITE);
                g2.drawString("ஈகை", 48, brandY);

                g2.setFont(UITheme.getTamilFont(Font.BOLD, 13));
                g2.setColor(new Color(0x9B, 0xB3, 0x9A)); // Pastel Sage
                g2.drawString("பகுத்துண்டு பல்லுயிர் ஓம்புதல்", 50, brandY + 24);

                // Draw Vector Food Donation Illustration
                int illusCenterX = w / 2;
                int illusCenterY = h / 2 - 30;
                drawFoodDonationIllustration(g2, illusCenterX, illusCenterY);

                // Draw Floating Glassmorphism Badges
                drawBadgePill(g2, 44, illusCenterY - 100, "45,000+ Meals Shared");
                drawBadgePill(g2, w - 190, illusCenterY + 50, "Zero Food Waste");

                // Tagline and Subtitle at Bottom
                int textY = h - 130;
                g2.setFont(new Font("Segoe UI", Font.BOLD, 21));
                g2.setColor(Color.WHITE);
                g2.drawString("Bridging Surplus Food", 48, textY);
                g2.drawString("to Those in Need", 48, textY + 28);

                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                g2.setColor(new Color(0xF7, 0xF4, 0xEE)); // Warm Cream Beige
                g2.drawString("Connecting local donors with verified NGOs and volunteer couriers.", 48, textY + 62);

                g2.dispose();
            }

            private void drawFoodDonationIllustration(Graphics2D g2, int cx, int cy) {
                // Outer radiating halo
                g2.setColor(new Color(255, 255, 255, 25));
                g2.fillOval(cx - 105, cy - 85, 210, 210);

                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillOval(cx - 85, cy - 65, 170, 170);

                // Wooden Food Crate Base
                int boxW = 140;
                int boxH = 65;
                int boxX = cx - (boxW / 2);
                int boxY = cy + 5;

                // Box Body
                g2.setColor(new Color(0xD9, 0x77, 0x06));
                g2.fillRoundRect(boxX, boxY, boxW, boxH, 10, 10);
                g2.setColor(new Color(0xB4, 0x53, 0x09));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(boxX, boxY, boxW, boxH, 10, 10);

                g2.drawLine(boxX + 6, boxY + 22, boxX + boxW - 6, boxY + 22);
                g2.drawLine(boxX + 6, boxY + 44, boxX + boxW - 6, boxY + 44);

                // Handle slot
                g2.setColor(new Color(0x78, 0x35, 0x0F));
                g2.fillRoundRect(cx - 18, boxY + 10, 36, 12, 6, 6);

                // Fresh Bread
                AffineTransform old = g2.getTransform();
                g2.translate(cx - 35, cy - 35);
                g2.rotate(Math.toRadians(-22));
                g2.setColor(new Color(0xF5, 0x9E, 0x0B));
                g2.fillRoundRect(0, 0, 32, 70, 14, 14);
                g2.setColor(new Color(0xD9, 0x77, 0x06));
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect(0, 0, 32, 70, 14, 14);
                g2.setColor(new Color(0xFE, 0xF3, 0xC7));
                g2.drawLine(6, 16, 26, 20);
                g2.drawLine(6, 32, 26, 36);
                g2.drawLine(6, 48, 26, 52);
                g2.setTransform(old);

                // Milk / Oil Bottle
                g2.setColor(new Color(0xE2, 0xE8, 0xF0));
                g2.fillRoundRect(cx + 15, cy - 38, 26, 54, 8, 8);
                g2.fillRoundRect(cx + 22, cy - 48, 12, 14, 4, 4);
                g2.setColor(new Color(0x10, 0xB9, 0x81));
                g2.fillRect(cx + 21, cy - 52, 14, 6);

                // Apple
                g2.setColor(new Color(0xEF, 0x44, 0x44));
                g2.fillOval(cx - 15, cy - 14, 38, 36);
                g2.setColor(new Color(0x22, 0xC5, 0x5E));
                g2.fillOval(cx, cy - 22, 12, 8);
                g2.setColor(new Color(0x78, 0x35, 0x0F));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawLine(cx + 4, cy - 14, cx + 5, cy - 20);

                // Heart Emblem
                drawGlowingHeart(g2, cx, boxY + 34, 14, new Color(0xDC, 0x26, 0x26));
            }

            private void drawGlowingHeart(Graphics2D g2, int hx, int hy, int size, Color color) {
                Path2D.Float heart = new Path2D.Float();
                heart.moveTo(hx, hy + size * 0.7f);
                heart.curveTo(hx - size * 1.1f, hy, hx - size * 0.9f, hy - size * 0.8f, hx, hy - size * 0.2f);
                heart.curveTo(hx + size * 0.9f, hy - size * 0.8f, hx + size * 1.1f, hy, hx, hy + size * 0.7f);
                heart.closePath();

                g2.setColor(new Color(255, 255, 255, 90));
                g2.fillOval(hx - size - 4, hy - size - 2, (size + 4) * 2, (size + 4) * 2);
                g2.setColor(color);
                g2.fill(heart);
            }

            private void drawBadgePill(Graphics2D g2, int px, int py, String text) {
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(text);
                int pillW = textW + 28;
                int pillH = 30;

                g2.setColor(new Color(255, 255, 255, 38));
                g2.fillRoundRect(px, py, pillW, pillH, 15, 15);
                g2.setColor(new Color(255, 255, 255, 80));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(px, py, pillW, pillH, 15, 15);

                g2.setColor(Color.WHITE);
                g2.drawString(text, px + 14, py + 20);
            }
        };
    }

    /**
     * Creates the Right Form Panel tailored directly to the selected role.
     * Note: No 3-way toggle button bar; role is clearly identified with badge and title.
     */
    private JPanel createRightFormPanel() {
        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(UITheme.SURFACE_WHITE);

        JPanel formCard = new JPanel();
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setOpaque(false);
        formCard.setPreferredSize(new Dimension(400, 560));
        formCard.setMaximumSize(new Dimension(400, 560));

        // Back to Role Selection Hub link
        JLabel backLink = new JLabel("<html>← <u>Back to Portal Selection</u></html>");
        backLink.setFont(UITheme.FONT_SMALL);
        backLink.setForeground(UITheme.TEXT_MUTED);
        backLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backLink.setAlignmentX(Component.LEFT_ALIGNMENT);
        backLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UITheme.fadeTransition(LoginFrame.this, () -> new RoleSelectionFrame().setVisible(true));
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
        formCard.add(backLink);
        formCard.add(Box.createVerticalStrut(10));

        // Role Badge Pill
        badgeLabel = new JLabel("NGO & COMMUNITY NETWORK");
        badgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badgeLabel.setForeground(new Color(0x02, 0x84, 0xC7));
        badgeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(badgeLabel);
        formCard.add(Box.createVerticalStrut(4));

        // Header Section
        headerLabel = new JLabel("NGO Portal Login");
        headerLabel.setFont(UITheme.FONT_TITLE);
        headerLabel.setForeground(UITheme.TEXT_DARK);
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        subHeaderLabel = new JLabel("Sign in to discover, claim, and distribute surplus food donations");
        subHeaderLabel.setFont(UITheme.FONT_SUBTITLE);
        subHeaderLabel.setForeground(UITheme.TEXT_MUTED);
        subHeaderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        formCard.add(headerLabel);
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(subHeaderLabel);
        formCard.add(Box.createVerticalStrut(16));

        // Inline Error Alert Banner
        inlineErrorPanel = createInlineErrorPanel();
        inlineErrorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        inlineErrorPanel.setVisible(false);
        formCard.add(inlineErrorPanel);
        formCard.add(Box.createVerticalStrut(10));

        // Email Field
        emailLabel = new JLabel("NGO Account Email");
        emailLabel.setFont(UITheme.FONT_LABEL);
        emailLabel.setForeground(UITheme.TEXT_DARK);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(emailLabel);
        formCard.add(Box.createVerticalStrut(6));

        emailField = new UITheme.RoundedTextField("e.g. hope.foundation@ngo.org");
        emailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        emailField.setPreferredSize(new Dimension(400, 42));
        emailField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) { clearInlineError(); }
        });
        formCard.add(emailField);
        formCard.add(Box.createVerticalStrut(12));

        // Password Field
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(UITheme.FONT_LABEL);
        passLabel.setForeground(UITheme.TEXT_DARK);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(passLabel);
        formCard.add(Box.createVerticalStrut(6));

        JPanel passwordContainer = createPasswordFieldWithEye();
        passwordContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(passwordContainer);
        formCard.add(Box.createVerticalStrut(12));

        // Helper Demo Quick-Fill Pills (Role-Specific)
        quickFillPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        quickFillPanel.setOpaque(false);
        quickFillPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(quickFillPanel);
        formCard.add(Box.createVerticalStrut(16));

        // Login Action Button
        loginButton = new UITheme.RoundedButton("Sign In to NGO Dispatch Center");
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginButton.setPreferredSize(new Dimension(400, 44));
        loginButton.addActionListener(e -> executeLogin());
        formCard.add(loginButton);
        formCard.add(Box.createVerticalStrut(14));

        ActionListener enterSubmit = e -> executeLogin();
        emailField.addActionListener(enterSubmit);
        passwordField.addActionListener(enterSubmit);

        // Role Switch Link (e.g. NGO <-> Volunteer without returning to menu)
        JPanel roleSwitchPanel = createRoleSwitchPanel();
        roleSwitchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(roleSwitchPanel);
        formCard.add(Box.createVerticalStrut(8));

        // Register Link Footer
        JPanel footerPanel = createRegisterFooterPanel();
        footerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(footerPanel);

        // Apply theme and role text dynamically
        applyRoleTheme(selectedRole);

        container.add(formCard);
        return container;
    }

    /**
     * Dynamically updates the Login page to match the requested role (NGO or Volunteer).
     * Eliminates redundant selection toggles while allowing seamless switching.
     */
    public void applyRoleTheme(String role) {
        this.selectedRole = (role != null && !role.trim().isEmpty()) ? role.trim() : "NGO";
        updateWindowTitle();

        if ("Volunteer".equalsIgnoreCase(selectedRole)) {
            badgeLabel.setText("VOLUNTEER COURIER NETWORK");
            badgeLabel.setForeground(new Color(0x8C, 0x6A, 0x3E)); // Warm Amber
            headerLabel.setText("Volunteer Courier Login");
            subHeaderLabel.setText("Sign in to accept food pickup tasks and log delivery missions");
            emailLabel.setText("Volunteer Courier Email");
            emailField.setPlaceholder("e.g. alex.green@volunteer.org");
            loginButton.setText("Sign In to Courier Hub");

            updateQuickFillPills("Volunteer");

            switchPromptLabel.setText("Representing an NGO or Shelter?");
            switchActionLink.setText("<html><u><b>Switch to NGO Login -&gt;</b></u></html>");
            switchActionLink.setForeground(new Color(0x72, 0x8C, 0x6E));
        } else if ("Donor".equalsIgnoreCase(selectedRole)) {
            badgeLabel.setText("FOOD DONOR NETWORK");
            badgeLabel.setForeground(UITheme.PRIMARY);
            headerLabel.setText("Donor Login");
            subHeaderLabel.setText("Sign in to manage and track your commercial food donations");
            emailLabel.setText("Donor Email Address");
            emailField.setPlaceholder("e.g. bistro@greengarden.com");
            loginButton.setText("Sign In to Donor Dashboard");

            updateQuickFillPills("Donor");

            switchPromptLabel.setText("Looking for NGO Dispatch?");
            switchActionLink.setText("<html><u><b>Switch to NGO Login -&gt;</b></u></html>");
            switchActionLink.setForeground(new Color(0x72, 0x8C, 0x6E));
        } else { // NGO
            badgeLabel.setText("NGO & COMMUNITY NETWORK");
            badgeLabel.setForeground(new Color(0x72, 0x8C, 0x6E)); // Muted Sage Green
            headerLabel.setText("NGO Portal Login");
            subHeaderLabel.setText("Sign in to discover, claim, and distribute surplus food donations");
            emailLabel.setText("NGO Account Email");
            emailField.setPlaceholder("e.g. hope.foundation@ngo.org");
            loginButton.setText("Sign In to NGO Dispatch Center");

            updateQuickFillPills("NGO");

            switchPromptLabel.setText("Looking for Volunteer Courier login?");
            switchActionLink.setText("<html><u><b>Switch to Courier Login -&gt;</b></u></html>");
            switchActionLink.setForeground(new Color(0x8C, 0x6A, 0x3E));
        }

        clearInlineError();
        revalidate();
        repaint();
    }

    /**
     * Updates quick-fill demo pills matching exclusively the active role.
     */
    private void updateQuickFillPills(String role) {
        quickFillPanel.removeAll();

        JLabel hint = new JLabel("Demo fill:");
        hint.setFont(UITheme.FONT_SMALL);
        hint.setForeground(UITheme.TEXT_MUTED);
        quickFillPanel.add(hint);

        if ("Volunteer".equalsIgnoreCase(role)) {
            JButton pill1 = createMiniPillButton("Alex Green");
            pill1.addActionListener(e -> {
                emailField.setText("alex.green@volunteer.org");
                passwordField.setText("Password@123");
                clearInlineError();
            });
            JButton pill2 = createMiniPillButton("Maya Lin");
            pill2.addActionListener(e -> {
                emailField.setText("maya.lin@volunteer.org");
                passwordField.setText("Password@123");
                clearInlineError();
            });
            JButton pill3 = createMiniPillButton("David Chen");
            pill3.addActionListener(e -> {
                emailField.setText("david.chen@volunteer.org");
                passwordField.setText("Password@123");
                clearInlineError();
            });
            quickFillPanel.add(pill1);
            quickFillPanel.add(pill2);
            quickFillPanel.add(pill3);
        } else if ("Donor".equalsIgnoreCase(role)) {
            JButton pill1 = createMiniPillButton("Bistro Donor");
            pill1.addActionListener(e -> {
                emailField.setText("bistro@greengarden.com");
                passwordField.setText("Password@123");
                clearInlineError();
            });
            JButton pill2 = createMiniPillButton("Grand Hotel");
            pill2.addActionListener(e -> {
                emailField.setText("grandhotel@luxury.com");
                passwordField.setText("Password@123");
                clearInlineError();
            });
            quickFillPanel.add(pill1);
            quickFillPanel.add(pill2);
        } else { // NGO
            JButton pill1 = createMiniPillButton("Hope NGO");
            pill1.addActionListener(e -> {
                emailField.setText("hope.foundation@ngo.org");
                passwordField.setText("Password@123");
                clearInlineError();
            });
            JButton pill2 = createMiniPillButton("City Harvest");
            pill2.addActionListener(e -> {
                emailField.setText("cityharvest@charity.org");
                passwordField.setText("Password@123");
                clearInlineError();
            });
            JButton pill3 = createMiniPillButton("Compassion");
            pill3.addActionListener(e -> {
                emailField.setText("compassion.meals@shelter.org");
                passwordField.setText("Password@123");
                clearInlineError();
            });
            quickFillPanel.add(pill1);
            quickFillPanel.add(pill2);
            quickFillPanel.add(pill3);
        }

        quickFillPanel.revalidate();
        quickFillPanel.repaint();
    }

    private JPanel createPasswordFieldWithEye() {
        JPanel wrapper = new JPanel(null);
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(400, 42));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        passwordField = new UITheme.RoundedPasswordField("Enter your password");
        passwordField.setBounds(0, 0, 400, 42);
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) { clearInlineError(); }
        });

        eyeButton = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(isPasswordVisible ? UITheme.PRIMARY : UITheme.TEXT_LIGHT);
                g2.setStroke(new BasicStroke(1.6f));

                Path2D.Float eye = new Path2D.Float();
                eye.moveTo(6, h / 2f);
                eye.quadTo(w / 2f, 7, w - 6, h / 2f);
                eye.quadTo(w / 2f, h - 7, 6, h / 2f);
                eye.closePath();
                g2.draw(eye);

                g2.fillOval((w / 2) - 3, (h / 2) - 3, 6, 6);

                if (!isPasswordVisible) {
                    g2.setColor(UITheme.TEXT_MUTED);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawLine(8, h - 8, w - 8, 8);
                }
                g2.dispose();
            }
        };
        eyeButton.setBounds(362, 6, 30, 30);
        eyeButton.setOpaque(false);
        eyeButton.setContentAreaFilled(false);
        eyeButton.setBorderPainted(false);
        eyeButton.setFocusPainted(false);
        eyeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eyeButton.setToolTipText("Show/Hide password");

        eyeButton.addActionListener(e -> {
            isPasswordVisible = !isPasswordVisible;
            passwordField.setEchoChar(isPasswordVisible ? (char) 0 : '•');
            eyeButton.repaint();
            passwordField.requestFocusInWindow();
        });

        wrapper.add(eyeButton);
        wrapper.add(passwordField);

        wrapper.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = wrapper.getWidth();
                passwordField.setBounds(0, 0, w, 42);
                eyeButton.setBounds(w - 38, 6, 30, 30);
            }
        });

        return wrapper;
    }

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
        panel.setPreferredSize(new Dimension(400, 40));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

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

    private JButton createMiniPillButton(String text) {
        JButton pill = new JButton(text);
        pill.setFont(UITheme.FONT_SMALL);
        pill.setForeground(UITheme.PRIMARY);
        pill.setBackground(UITheme.PRIMARY_BG_TINT);
        pill.setFocusPainted(false);
        pill.setCursor(new Cursor(Cursor.HAND_CURSOR));
        pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xA9, 0xBF, 0xA8), 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));
        return pill;
    }

    private JPanel createRoleSwitchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        panel.setOpaque(false);

        switchPromptLabel = new JLabel("Looking for Volunteer Courier login?");
        switchPromptLabel.setFont(UITheme.FONT_SMALL);
        switchPromptLabel.setForeground(UITheme.TEXT_MUTED);

        switchActionLink = new JLabel("<html><u><b>Switch to Courier Login →</b></u></html>");
        switchActionLink.setFont(UITheme.FONT_SMALL);
        switchActionLink.setForeground(new Color(0x8C, 0x6A, 0x3E));
        switchActionLink.setCursor(new Cursor(Cursor.HAND_CURSOR));

        switchActionLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if ("NGO".equalsIgnoreCase(selectedRole)) {
                    applyRoleTheme("Volunteer");
                } else if ("Volunteer".equalsIgnoreCase(selectedRole)) {
                    applyRoleTheme("NGO");
                } else {
                    applyRoleTheme("NGO");
                }
                emailField.setText("");
                passwordField.setText("");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if ("Volunteer".equalsIgnoreCase(selectedRole)) {
                    switchActionLink.setForeground(new Color(0x4E, 0x65, 0x4B));
                } else {
                    switchActionLink.setForeground(new Color(0x6B, 0x1D, 0x2F));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if ("Volunteer".equalsIgnoreCase(selectedRole)) {
                    switchActionLink.setForeground(new Color(0x72, 0x8C, 0x6E));
                } else {
                    switchActionLink.setForeground(new Color(0x8C, 0x6A, 0x3E));
                }
            }
        });

        panel.add(switchPromptLabel);
        panel.add(switchActionLink);
        return panel;
    }

    private JPanel createRegisterFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        footer.setOpaque(false);

        JLabel prompt = new JLabel("Don't have an account?");
        prompt.setFont(UITheme.FONT_SUBTITLE);
        prompt.setForeground(UITheme.TEXT_MUTED);

        JLabel registerLink = new JLabel("<html><u><b>Register here</b></u></html>");
        registerLink.setFont(UITheme.FONT_SUBTITLE);
        registerLink.setForeground(UITheme.PRIMARY);
        registerLink.setCursor(new Cursor(Cursor.HAND_CURSOR));

        registerLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                transitionToRegister();
            }

            @Override
            public void mouseEntered(MouseEvent e) { registerLink.setForeground(UITheme.PRIMARY_HOVER); }
            @Override
            public void mouseExited(MouseEvent e) { registerLink.setForeground(UITheme.PRIMARY); }
        });

        footer.add(prompt);
        footer.add(registerLink);
        return footer;
    }

    private void executeLogin() {
        clearInlineError();

        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        loginButton.setEnabled(false);
        loginButton.setText("Authenticating...");

        SwingUtilities.invokeLater(() -> {
            try {
                AuthService.AuthResult result = authService.login(email, password, selectedRole);

                if (result.isSuccess()) {
                    User user = result.getUser();
                    performTransitionToDashboard(user);
                } else {
                    showInlineError(result.getErrorMessage());
                    loginButton.setEnabled(true);
                    loginButton.setText(getLoginButtonTextForRole(selectedRole));
                }
            } catch (Exception ex) {
                showInlineError("Authentication error: " + ex.getMessage());
                loginButton.setEnabled(true);
                loginButton.setText(getLoginButtonTextForRole(selectedRole));
            }
        });
    }

    private String getLoginButtonTextForRole(String role) {
        if ("Volunteer".equalsIgnoreCase(role)) {
            return "Sign In to Courier Hub";
        } else if ("Donor".equalsIgnoreCase(role)) {
            return "Sign In to Donor Dashboard";
        } else {
            return "Sign In to NGO Dispatch Center";
        }
    }

    private void showInlineError(String message) {
        inlineErrorLabel.setText("<html>" + message + "</html>");
        inlineErrorPanel.setVisible(true);
        inlineErrorPanel.revalidate();
        inlineErrorPanel.repaint();
    }

    private void clearInlineError() {
        if (inlineErrorPanel.isVisible()) {
            inlineErrorPanel.setVisible(false);
            inlineErrorPanel.revalidate();
            inlineErrorPanel.repaint();
        }
    }

    private void performTransitionToDashboard(User user) {
        Timer fadeTimer = new Timer(15, null);
        final float[] opacity = {1.0f};

        fadeTimer.addActionListener(e -> {
            opacity[0] -= 0.08f;
            if (opacity[0] <= 0.05f) {
                fadeTimer.stop();
                dispose();
                SwingUtilities.invokeLater(() -> {
                    JFrame dashboard;
                    if ("Donor".equalsIgnoreCase(user.getRole())) {
                        dashboard = new DonorDashboard();
                    } else if ("NGO".equalsIgnoreCase(user.getRole())) {
                        dashboard = new NGODashboard();
                    } else {
                        dashboard = new VolunteerDashboard();
                    }
                    dashboard.setVisible(true);
                });
            } else {
                try {
                    setOpacity(Math.max(0.0f, opacity[0]));
                } catch (Exception ex) {
                    fadeTimer.stop();
                    dispose();
                    JFrame dashboard = "Donor".equalsIgnoreCase(user.getRole())
                            ? new DonorDashboard()
                            : ("NGO".equalsIgnoreCase(user.getRole()) ? new NGODashboard() : new VolunteerDashboard());
                    dashboard.setVisible(true);
                }
            }
        });
        fadeTimer.start();
    }

    /**
     * Smooth fade & slide transition to RegisterFrame.
     */
    private void transitionToRegister() {
        Timer fadeTimer = new Timer(15, null);
        final float[] opacity = {1.0f};

        fadeTimer.addActionListener(e -> {
            opacity[0] -= 0.08f;
            if (opacity[0] <= 0.05f) {
                fadeTimer.stop();
                dispose();
                SwingUtilities.invokeLater(() -> {
                    RegisterFrame regFrame = new RegisterFrame();
                    regFrame.setVisible(true);
                });
            } else {
                try {
                    setOpacity(Math.max(0.0f, opacity[0]));
                } catch (Exception ex) {
                    fadeTimer.stop();
                    dispose();
                    RegisterFrame regFrame = new RegisterFrame();
                    regFrame.setVisible(true);
                }
            }
        });
        fadeTimer.start();
    }
}
