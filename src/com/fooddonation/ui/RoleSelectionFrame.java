package com.fooddonation.ui;

import com.fooddonation.util.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Role Selection Portal Frame for Eegai (ஈகை).
 * Serves as the welcoming entry point where users select their portal:
 * 1. Food Donor (Restaurants, Hotels, Individuals)
 * 2. NGO & Shelter (Charities, Community Kitchens, Shelters)
 * 3. Volunteer Courier (Drivers, Cyclists, Delivery Responders)
 *
 * Adheres to the emerald/forest green aesthetic, rounded elevated cards,
 * Segoe UI typography, smooth hover highlights, wand cursor + glitter trail,
 * and soft fade transitions into each role's login flow.
 */
public class RoleSelectionFrame extends JFrame {

    public RoleSelectionFrame() {
        initWindow();
        initUI();
    }

    private void initWindow() {
        setTitle("ஈகை — பகுத்துண்டு பல்லுயிர் ஓம்புதல்");
        setSize(1060, 720);
        setMinimumSize(new Dimension(960, 640));
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

        // 1. Top Header Banner with Deep Forest Gradient
        JPanel headerPanel = createHeaderPanel();

        // 2. Center Role Selection Cards (3 Columns)
        JPanel cardsContainer = createCardsContainer();

        // 3. Bottom Action Bar (Register link + Public Reports button)
        JPanel footerPanel = createFooterPanel();

        rootPanel.add(headerPanel, BorderLayout.NORTH);
        rootPanel.add(cardsContainer, BorderLayout.CENTER);
        rootPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(rootPanel);
    }

    /**
     * Top Header Banner with Deep Forest Gradient and mission branding.
     */
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0x4A, 0x15, 0x21), // Deep Burgundy
                        getWidth(), getHeight(), new Color(0x28, 0x0A, 0x11) // Dark Wine Burgundy
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Soft luminous arc
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillOval(-100, -150, getWidth() + 200, 260);

                g2.dispose();
            }
        };
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(32, 28, 28, 28));

        JLabel logo = new JLabel("ஈகை");
        logo.setFont(UITheme.getTamilFont(Font.BOLD, 32));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subTitle = new JLabel("பகுத்துண்டு பல்லுயிர் ஓம்புதல்");
        subTitle.setFont(UITheme.getTamilFont(Font.BOLD, 15));
        subTitle.setForeground(new Color(0x9B, 0xB3, 0x9A)); // Pastel Sage Green
        subTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tagline = new JLabel("Connecting local food donors, rescue charities, and courier volunteers to end hunger.");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tagline.setForeground(new Color(0xF7, 0xF4, 0xEE)); // Warm Cream Beige
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(logo);
        header.add(Box.createVerticalStrut(4));
        header.add(subTitle);
        header.add(Box.createVerticalStrut(8));
        header.add(tagline);

        return header;
    }

    /**
     * 3-Card Interactive Grid for Donor, NGO, and Volunteer roles.
     */
    private JPanel createCardsContainer() {
        JPanel container = new JPanel(new GridBagLayout());
        container.setOpaque(false);
        container.setBorder(new EmptyBorder(24, 28, 20, 28));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // Card 1: Food Donor (Deep Burgundy)
        gbc.gridx = 0;
        container.add(createRoleCard(
                "DONOR",
                "Food Donor Portal",
                "Restaurants, Messes, Caterers & Halls",
                "List surplus commercial kitchen meals, coordinate courier pickups, and track your environmental food rescue impact.",
                "Enter Donor Portal ->",
                new Color(0x6B, 0x1D, 0x2F),
                () -> UITheme.fadeTransition(this, () -> new DonorLoginFrame().setVisible(true))
        ), gbc);

        // Card 2: NGO & Shelter (Muted Sage Green)
        gbc.gridx = 1;
        container.add(createRoleCard(
                "NGO",
                "NGO & Shelter Hub",
                "Charities & Community Kitchens",
                "Discover fresh surplus food across your city, claim meals atomically, and coordinate courier pickups.",
                "Enter NGO Hub ->",
                new Color(0x72, 0x8C, 0x6E),
                () -> UITheme.fadeTransition(this, () -> new LoginFrame(null, "NGO").setVisible(true))
        ), gbc);

        // Card 3: Volunteer Courier (Warm Earthy Beige Amber)
        gbc.gridx = 2;
        container.add(createRoleCard(
                "VOLUNTEER",
                "Volunteer Courier Hub",
                "Drivers, Cyclists & Responders",
                "Discover open pickup tasks in your area, advance live delivery milestones, and log hours contributed.",
                "Enter Courier Hub ->",
                new Color(0x8C, 0x6A, 0x3E),
                () -> UITheme.fadeTransition(this, () -> new LoginFrame(null, "Volunteer").setVisible(true))
        ), gbc);

        return container;
    }

    /**
     * Factory for an interactive Role Card with hover animations and custom button.
     */
    private JPanel createRoleCard(String roleKey, String title, String subtitle, String description,
                                  String buttonText, Color accentColor, Runnable onSelect) {
        final boolean[] hovered = {false};

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Drop shadow
                if (hovered[0]) {
                    g2.setColor(new Color(0, 0, 0, 20));
                    g2.fill(new RoundRectangle2D.Float(2, 6, w - 4, h - 6, 16, 16));
                } else {
                    g2.setColor(new Color(0, 0, 0, 10));
                    g2.fill(new RoundRectangle2D.Float(1, 3, w - 2, h - 3, 16, 16));
                }

                // White card background
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h - 4, 16, 16));

                // Border (accent highlight on hover)
                if (hovered[0]) {
                    g2.setColor(accentColor);
                    g2.setStroke(new BasicStroke(2.0f));
                } else {
                    g2.setColor(new Color(0xE2, 0xE8, 0xF0));
                    g2.setStroke(new BasicStroke(1.0f));
                }
                g2.draw(new RoundRectangle2D.Float(0, 0, w - 1, h - 5, 16, 16));

                // Top Accent Strip
                g2.setColor(accentColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, 6, 16, 16));
                g2.fillRect(0, 4, w, 2);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(22, 20, 20, 20));

        // Center Content
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JComponent iconComp = createRoleIcon(roleKey, accentColor);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLbl.setForeground(UITheme.TEXT_DARK);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        subLbl.setForeground(accentColor);
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLbl = new JLabel("<html><div style='text-align: center; color: #475569; font-size: 11px; line-height: 1.4;'>"
                + description + "</div></html>");
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(iconComp);
        content.add(Box.createVerticalStrut(10));
        content.add(titleLbl);
        content.add(Box.createVerticalStrut(3));
        content.add(subLbl);
        content.add(Box.createVerticalStrut(12));
        content.add(descLbl);

        // Bottom Action Button
        UITheme.RoundedButton actionBtn = new UITheme.RoundedButton(buttonText);
        actionBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        actionBtn.setButtonColors(accentColor, accentColor.darker());
        actionBtn.addActionListener(e -> {
            if (onSelect != null) onSelect.run();
        });

        card.add(content, BorderLayout.CENTER);
        card.add(actionBtn, BorderLayout.SOUTH);

        // Hover listener on entire card
        MouseAdapter hoverHandler = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered[0] = true;
                card.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), card);
                if (!card.contains(p)) {
                    hovered[0] = false;
                    card.repaint();
                }
            }
        };

        card.addMouseListener(hoverHandler);
        content.addMouseListener(hoverHandler);

        return card;
    }

    /**
     * Vector icon drawer for role portals (eliminates font emoji tofu boxes).
     */
    private JComponent createRoleIcon(String roleKey, Color accentColor) {
        JComponent comp = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Soft circular background tint
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 26));
                g2.fillOval(2, 2, w - 4, h - 4);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 60));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawOval(2, 2, w - 4, h - 4);

                g2.setColor(accentColor);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int cx = w / 2;
                int cy = h / 2;

                if ("DONOR".equalsIgnoreCase(roleKey)) {
                    // Dining Cloche / Harvest Plate Vector Icon
                    g2.drawLine(cx - 14, cy + 9, cx + 14, cy + 9);
                    Path2D.Float dome = new Path2D.Float();
                    dome.moveTo(cx - 12, cy + 7);
                    dome.curveTo(cx - 12, cy - 8, cx + 12, cy - 8, cx + 12, cy + 7);
                    dome.closePath();
                    g2.draw(dome);
                    g2.fillOval(cx - 2, cy - 11, 4, 4);
                } else if ("NGO".equalsIgnoreCase(roleKey)) {
                    // Shelter / Community Building Vector Icon
                    Path2D.Float roof = new Path2D.Float();
                    roof.moveTo(cx - 14, cy - 2);
                    roof.lineTo(cx, cy - 12);
                    roof.lineTo(cx + 14, cy - 2);
                    roof.closePath();
                    g2.draw(roof);
                    g2.drawRect(cx - 10, cy - 2, 20, 14);
                    g2.fillRect(cx - 3, cy + 4, 6, 8);
                } else {
                    // Volunteer Delivery Parcel / Courier Vector Icon
                    g2.drawRoundRect(cx - 11, cy - 8, 22, 18, 4, 4);
                    g2.drawLine(cx, cy - 8, cx, cy + 10);
                    g2.drawLine(cx - 11, cy + 1, cx + 11, cy + 1);
                }

                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(54, 54);
            }

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(54, 54);
            }
        };
        comp.setAlignmentX(Component.CENTER_ALIGNMENT);
        return comp;
    }

    /**
     * Bottom Footer with Registration redirect, Public Reports launcher, and DB status.
     */
    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(12, 32, 20, 32));

        // Left: Register Account prompt
        JPanel leftPrompt = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftPrompt.setOpaque(false);

        JLabel registerText = new JLabel("New to ஈகை?");
        registerText.setFont(UITheme.FONT_SUBTITLE);
        registerText.setForeground(UITheme.TEXT_MUTED);

        JLabel registerLink = new JLabel("<html><u><b>Create New Account</b></u></html>");
        registerLink.setFont(UITheme.FONT_SUBTITLE);
        registerLink.setForeground(UITheme.PRIMARY);
        registerLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UITheme.fadeTransition(RoleSelectionFrame.this, () -> new RegisterFrame().setVisible(true));
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

        leftPrompt.add(registerText);
        leftPrompt.add(registerLink);

        // Right: Public Reports & Analytics Button
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        JButton reportsBtn = new JButton("View Community Impact Report");
        reportsBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        reportsBtn.setForeground(UITheme.PRIMARY);
        reportsBtn.setBackground(new Color(0xE8, 0xEF, 0xE8));
        reportsBtn.setFocusPainted(false);
        reportsBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xA9, 0xBF, 0xA8), 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));
        reportsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        reportsBtn.addActionListener(e -> ReportsPanel.showReportsDialog(this));

        rightActions.add(reportsBtn);

        footer.add(leftPrompt, BorderLayout.WEST);
        footer.add(rightActions, BorderLayout.EAST);

        return footer;
    }
}
