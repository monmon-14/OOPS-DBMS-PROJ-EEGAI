package com.fooddonation.ui;

import com.fooddonation.model.User;
import com.fooddonation.service.AuthService;
import com.fooddonation.util.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Dedicated Login Frame for Food Donors (Restaurants, Hotels, Individuals).
 * Matches the split layout, green palette, wand cursor, and glitter particle trail of LoginFrame.
 * No role toggle needed; authenticates specifically against role = 'Donor'.
 */
public class DonorLoginFrame extends JFrame {

    private final AuthService authService;

    // UI Components
    private UITheme.RoundedTextField emailField;
    private UITheme.RoundedPasswordField passwordField;
    private JButton eyeButton;
    private boolean isPasswordVisible = false;
    private JPanel inlineErrorPanel;
    private JLabel inlineErrorLabel;
    private UITheme.RoundedButton loginButton;

    public DonorLoginFrame() {
        this(new AuthService(), null);
    }

    public DonorLoginFrame(String prefilledEmail) {
        this(new AuthService(), prefilledEmail);
    }

    public DonorLoginFrame(AuthService authService, String prefilledEmail) {
        this.authService = authService;

        // Initialize Database schema asynchronously
        new Thread(DBConnection::initializeDatabase).start();

        initWindow();
        initUI();

        if (prefilledEmail != null && !prefilledEmail.trim().isEmpty()) {
            emailField.setText(prefilledEmail.trim());
            passwordField.requestFocusInWindow();
        }
    }

    private void initWindow() {
        setTitle("ஈகை — உணவு கொடையாளர் உள்நுழைவு (Food Donor Login)");
        setSize(980, 640);
        setMinimumSize(new Dimension(880, 580));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Apply Custom Magic Wand Cursor & Glitter Trail Glass Pane
        GlitterTrailGlassPane glassPane = new GlitterTrailGlassPane(this);
        setGlassPane(glassPane);
        glassPane.setVisible(true);
    }

    private void initUI() {
        JPanel rootPanel = new JPanel(new GridLayout(1, 2, 0, 0));
        rootPanel.setBackground(UITheme.SURFACE_WHITE);

        // 1. Left Hero Panel (Vector food donation illustration & mission statement)
        JPanel leftHeroPanel = createLeftHeroPanel();

        // 2. Right Form Panel (Donor Login Card)
        JPanel rightPanel = createRightFormPanel();

        rootPanel.add(leftHeroPanel);
        rootPanel.add(rightPanel);

        setContentPane(rootPanel);
    }

    /**
     * Left Hero Panel with vector food donation artwork and donor-centric mission tagline.
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

                // Ambient Glow Orbs
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
                g2.setColor(new Color(0x9B, 0xB3, 0x9A)); // Pastel Sage
                g2.fillOval(-60, -60, 320, 320);
                g2.fillOval(w - 200, h - 220, 280, 280);

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
                g2.setColor(Color.WHITE);
                g2.drawOval(-40, -40, 280, 280);
                g2.drawOval(w - 220, h - 240, 320, 320);

                g2.setComposite(AlphaComposite.SrcOver);

                // App Branding Header
                int brandY = 48;
                g2.setFont(UITheme.getTamilFont(Font.BOLD, 26));
                g2.setColor(Color.WHITE);
                g2.drawString("ஈகை", 48, brandY);

                g2.setFont(UITheme.getTamilFont(Font.BOLD, 13));
                g2.setColor(new Color(0x9B, 0xB3, 0x9A)); // Pastel Sage
                g2.drawString("பகுத்துண்டு பல்லுயிர் ஓம்புதல்", 50, brandY + 24);

                // Vector Food Donation Illustration
                int cx = w / 2;
                int cy = h / 2 - 30;
                drawFoodCrateIllustration(g2, cx, cy);

                // Floating Badges
                drawBadgePill(g2, 44, cy - 100, "Zero Food Waste Goal");
                drawBadgePill(g2, w - 190, cy + 50, "100% Tax Deductible");

                // Tagline at Bottom
                int textY = h - 130;
                g2.setFont(new Font("Segoe UI", Font.BOLD, 21));
                g2.setColor(Color.WHITE);
                g2.drawString("Share Surplus Food,", 48, textY);
                g2.drawString("Feed Hungry Families", 48, textY + 28);

                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                g2.setColor(new Color(0xF7, 0xF4, 0xEE)); // Warm Cream Beige
                g2.drawString("Connect surplus kitchen inventory directly to verified community shelters.", 48, textY + 62);

                g2.dispose();
            }

            private void drawFoodCrateIllustration(Graphics2D g2, int cx, int cy) {
                // Outer radiating halo
                g2.setColor(new Color(255, 255, 255, 25));
                g2.fillOval(cx - 105, cy - 85, 210, 210);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillOval(cx - 85, cy - 65, 170, 170);

                int boxW = 140;
                int boxH = 65;
                int boxX = cx - (boxW / 2);
                int boxY = cy + 5;

                // Crate Body
                g2.setColor(new Color(0xD9, 0x77, 0x06));
                g2.fillRoundRect(boxX, boxY, boxW, boxH, 10, 10);
                g2.setColor(new Color(0xB4, 0x53, 0x09));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(boxX, boxY, boxW, boxH, 10, 10);

                // Slat details
                g2.drawLine(boxX + 6, boxY + 22, boxX + boxW - 6, boxY + 22);
                g2.drawLine(boxX + 6, boxY + 44, boxX + boxW - 6, boxY + 44);

                // Handle slot
                g2.setColor(new Color(0x78, 0x35, 0x0F));
                g2.fillRoundRect(cx - 18, boxY + 10, 36, 12, 6, 6);

                // Artisan Bread
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
                g2.setTransform(old);

                // Milk bottle
                g2.setColor(new Color(0xE2, 0xE8, 0xF0));
                g2.fillRoundRect(cx + 15, cy - 38, 26, 54, 8, 8);
                g2.fillRoundRect(cx + 22, cy - 48, 12, 14, 4, 4);
                g2.setColor(new Color(0x10, 0xB9, 0x81));
                g2.fillRect(cx + 21, cy - 52, 14, 6);

                // Red Apple
                g2.setColor(new Color(0xEF, 0x44, 0x44));
                g2.fillOval(cx - 15, cy - 14, 38, 36);
                g2.setColor(new Color(0x22, 0xC5, 0x5E));
                g2.fillOval(cx, cy - 22, 12, 8);
                g2.setColor(new Color(0x78, 0x35, 0x0F));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawLine(cx + 4, cy - 14, cx + 5, cy - 20);

                // Radiating Heart
                Path2D.Float heart = new Path2D.Float();
                int hx = cx;
                int hy = boxY + 34;
                int s = 14;
                heart.moveTo(hx, hy + s * 0.7f);
                heart.curveTo(hx - s * 1.1f, hy, hx - s * 0.9f, hy - s * 0.8f, hx, hy - s * 0.2f);
                heart.curveTo(hx + s * 0.9f, hy - s * 0.8f, hx + s * 1.1f, hy, hx, hy + s * 0.7f);
                heart.closePath();

                g2.setColor(new Color(255, 255, 255, 90));
                g2.fillOval(hx - s - 4, hy - s - 2, (s + 4) * 2, (s + 4) * 2);
                g2.setColor(new Color(0xDC, 0x26, 0x26));
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
     * Right Form Panel for Donor Login.
     */
    private JPanel createRightFormPanel() {
        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(UITheme.SURFACE_WHITE);

        JPanel formCard = new JPanel();
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setOpaque(false);
        formCard.setPreferredSize(new Dimension(380, 520));
        formCard.setMaximumSize(new Dimension(380, 520));

        // Back to Role Selection Hub link
        JLabel backLink = new JLabel("<html>← <u>Back to Portal Selection</u></html>");
        backLink.setFont(UITheme.FONT_SMALL);
        backLink.setForeground(UITheme.TEXT_MUTED);
        backLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backLink.setAlignmentX(Component.LEFT_ALIGNMENT);
        backLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UITheme.fadeTransition(DonorLoginFrame.this, () -> new RoleSelectionFrame().setVisible(true));
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

        // Header Title
        JLabel badgeLabel = new JLabel("FOOD DONOR NETWORK");
        badgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badgeLabel.setForeground(UITheme.PRIMARY);
        badgeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel headerLabel = new JLabel("Donor Login");
        headerLabel.setFont(UITheme.FONT_TITLE);
        headerLabel.setForeground(UITheme.TEXT_DARK);
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subHeaderLabel = new JLabel("Sign in to manage and track your commercial food donations");
        subHeaderLabel.setFont(UITheme.FONT_SUBTITLE);
        subHeaderLabel.setForeground(UITheme.TEXT_MUTED);
        subHeaderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        formCard.add(badgeLabel);
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(headerLabel);
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(subHeaderLabel);
        formCard.add(Box.createVerticalStrut(20));

        // Inline Error Alert Banner
        inlineErrorPanel = createInlineErrorPanel();
        inlineErrorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        inlineErrorPanel.setVisible(false);
        formCard.add(inlineErrorPanel);
        formCard.add(Box.createVerticalStrut(12));

        // Email Field
        JLabel emailLabel = new JLabel("Donor Email Address");
        emailLabel.setFont(UITheme.FONT_LABEL);
        emailLabel.setForeground(UITheme.TEXT_DARK);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(emailLabel);
        formCard.add(Box.createVerticalStrut(6));

        emailField = new UITheme.RoundedTextField("e.g. bistro@greengarden.com");
        emailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        emailField.setPreferredSize(new Dimension(380, 42));
        emailField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) { clearInlineError(); }
        });
        formCard.add(emailField);
        formCard.add(Box.createVerticalStrut(14));

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
        formCard.add(Box.createVerticalStrut(14));

        // Quick Fill Demo Button
        JPanel quickFillRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        quickFillRow.setOpaque(false);
        quickFillRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel demoHint = new JLabel("Demo fill:");
        demoHint.setFont(UITheme.FONT_SMALL);
        demoHint.setForeground(UITheme.TEXT_MUTED);
        quickFillRow.add(demoHint);

        JButton annamDemoBtn = new JButton("Annam Mess");
        annamDemoBtn.setFont(UITheme.FONT_SMALL);
        annamDemoBtn.setForeground(UITheme.PRIMARY);
        annamDemoBtn.setBackground(UITheme.PRIMARY_BG_TINT);
        annamDemoBtn.setFocusPainted(false);
        annamDemoBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        annamDemoBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xA9, 0xBF, 0xA8), 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));
        annamDemoBtn.addActionListener(e -> {
            emailField.setText("annam.mess@demo.eegai.org");
            passwordField.setText("Password@123");
            clearInlineError();
        });
        quickFillRow.add(annamDemoBtn);

        JButton bistroDemoBtn = new JButton("Green Garden Bistro");
        bistroDemoBtn.setFont(UITheme.FONT_SMALL);
        bistroDemoBtn.setForeground(UITheme.PRIMARY);
        bistroDemoBtn.setBackground(UITheme.PRIMARY_BG_TINT);
        bistroDemoBtn.setFocusPainted(false);
        bistroDemoBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        bistroDemoBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xA9, 0xBF, 0xA8), 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));
        bistroDemoBtn.addActionListener(e -> {
            emailField.setText("bistro@greengarden.com");
            passwordField.setText("Password@123");
            clearInlineError();
        });
        quickFillRow.add(bistroDemoBtn);

        formCard.add(quickFillRow);
        formCard.add(Box.createVerticalStrut(18));

        // Login Action Button
        loginButton = new UITheme.RoundedButton("Sign In as Food Donor");
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginButton.setPreferredSize(new Dimension(380, 44));
        loginButton.addActionListener(e -> executeLogin());
        formCard.add(loginButton);
        formCard.add(Box.createVerticalStrut(14));

        ActionListener enterSubmit = e -> executeLogin();
        emailField.addActionListener(enterSubmit);
        passwordField.addActionListener(enterSubmit);

        // Role Switch Link to NGO Portal (matching LoginFrame layout)
        JPanel roleSwitchPanel = createRoleSwitchPanel();
        roleSwitchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(roleSwitchPanel);
        formCard.add(Box.createVerticalStrut(8));

        // Register Link Footer
        JPanel footerPanel = createRegisterFooterPanel();
        footerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(footerPanel);

        container.add(formCard);
        return container;
    }

    private JPanel createPasswordFieldWithEye() {
        JPanel wrapper = new JPanel(null);
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(380, 42));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        passwordField = new UITheme.RoundedPasswordField("Enter your password");
        passwordField.setBounds(0, 0, 380, 42);
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
        eyeButton.setBounds(342, 6, 30, 30);
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
        panel.setPreferredSize(new Dimension(380, 40));
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

    private JPanel createRoleSwitchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        panel.setOpaque(false);

        JLabel switchPromptLabel = new JLabel("Representing an NGO or Shelter?");
        switchPromptLabel.setFont(UITheme.FONT_SMALL);
        switchPromptLabel.setForeground(UITheme.TEXT_MUTED);

        JLabel switchActionLink = new JLabel("<html><u><b>Switch to NGO Login →</b></u></html>");
        switchActionLink.setFont(UITheme.FONT_SMALL);
        switchActionLink.setForeground(new Color(0x72, 0x8C, 0x6E)); // Muted Sage Green
        switchActionLink.setCursor(new Cursor(Cursor.HAND_CURSOR));

        switchActionLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                transitionToFrame(new LoginFrame(null, "NGO"));
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                switchActionLink.setForeground(new Color(0x4E, 0x65, 0x4B));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                switchActionLink.setForeground(new Color(0x72, 0x8C, 0x6E));
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
                transitionToFrame(new RegisterFrame());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                registerLink.setForeground(UITheme.PRIMARY_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                registerLink.setForeground(UITheme.PRIMARY);
            }
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
                AuthService.AuthResult result = authService.login(email, password, "Donor");

                if (result.isSuccess()) {
                    User user = result.getUser();
                    performTransitionToDashboard(user);
                } else {
                    showInlineError(result.getErrorMessage());
                    loginButton.setEnabled(true);
                    loginButton.setText("Sign In as Food Donor");
                }
            } catch (Exception ex) {
                showInlineError("Authentication error: " + ex.getMessage());
                loginButton.setEnabled(true);
                loginButton.setText("Sign In as Food Donor");
            }
        });
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
                    DonorDashboard dashboard = new DonorDashboard();
                    dashboard.setVisible(true);
                });
            } else {
                try {
                    setOpacity(Math.max(0.0f, opacity[0]));
                } catch (Exception ex) {
                    fadeTimer.stop();
                    dispose();
                    DonorDashboard dashboard = new DonorDashboard();
                    dashboard.setVisible(true);
                }
            }
        });
        fadeTimer.start();
    }

    private void transitionToFrame(JFrame targetFrame) {
        Timer fadeTimer = new Timer(15, null);
        final float[] opacity = {1.0f};

        fadeTimer.addActionListener(e -> {
            opacity[0] -= 0.08f;
            if (opacity[0] <= 0.05f) {
                fadeTimer.stop();
                dispose();
                SwingUtilities.invokeLater(() -> targetFrame.setVisible(true));
            } else {
                try {
                    setOpacity(Math.max(0.0f, opacity[0]));
                } catch (Exception ex) {
                    fadeTimer.stop();
                    dispose();
                    targetFrame.setVisible(true);
                }
            }
        });
        fadeTimer.start();
    }
}
