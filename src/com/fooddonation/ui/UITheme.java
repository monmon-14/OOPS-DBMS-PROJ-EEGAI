package com.fooddonation.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Theme configuration and custom styled Swing UI components
 * implementing a clean, elegant green/white palette with rounded inputs and soft shadows.
 */
public class UITheme {

    // Primary Burgundy & Sage Palette
    public static final Color PRIMARY_DARK = new Color(0x4A, 0x15, 0x21);      // Deep Burgundy Dark (#4A1521)
    public static final Color PRIMARY = new Color(0x6B, 0x1D, 0x2F);           // Deep Burgundy (#6B1D2F)
    public static final Color PRIMARY_ACCENT = new Color(0x72, 0x8C, 0x6E);    // Muted Sage Green (#728C6E)
    public static final Color PRIMARY_LIGHT = new Color(0x9B, 0xB3, 0x9A);     // Pastel Sage Light (#9BB39A)
    public static final Color PRIMARY_BG_TINT = new Color(0xE8, 0xEF, 0xE8);   // Pastel Sage Tint (#E8EFE8)
    public static final Color PRIMARY_HOVER = new Color(0x54, 0x15, 0x23);     // Deep Burgundy Hover (#541523)

    // Neutral Surfaces & Borders (Warm Cream Beige)
    public static final Color SURFACE_WHITE = new Color(0xFF, 0xFD, 0xF9);     // Cream White (#FFFDF9)
    public static final Color BG_OFFWHITE = new Color(0xF7, 0xF4, 0xEE);       // Warm Cream Beige (#F7F4EE)
    public static final Color BORDER_GRAY = new Color(0xE0, 0xD7, 0xC9);       // Warm Beige Border (#E0D7C9)
    public static final Color BORDER_FOCUSED = new Color(0x6B, 0x1D, 0x2F);    // Burgundy Border Focused
    public static final Color TEXT_DARK = new Color(0x2E, 0x26, 0x23);         // Warm Dark Espresso (#2E2623)
    public static final Color TEXT_MUTED = new Color(0x6B, 0x60, 0x57);        // Warm Muted Taupe (#6B6057)
    public static final Color TEXT_LIGHT = new Color(0x9C, 0x90, 0x84);        // Warm Soft Taupe (#9C9084)

    // Feedback Colors
    public static final Color ERROR_BG = new Color(0xFD, 0xEA, 0xEE);          // Soft Pastel Burgundy/Rose
    public static final Color ERROR_TEXT = new Color(0x9E, 0x1F, 0x36);        // Deep Burgundy Red
    public static final Color ERROR_BORDER = new Color(0xF5, 0xC6, 0xD0);      // Muted Rose Border
    public static final Color SUCCESS_BG = new Color(0xE8, 0xEF, 0xE8);        // Pastel Sage Tint
    public static final Color SUCCESS_TEXT = new Color(0x4E, 0x65, 0x4B);      // Dark Muted Sage

    // Tamil Culture & Bohemian Palette (ஈகை Theme - Harmonized)
    public static final Color CREAM_BG = new Color(0xF7, 0xF4, 0xEE);          // Warm Cream Beige (#F7F4EE)
    public static final Color MAROON_PRIMARY = new Color(0x6B, 0x1D, 0x2F);    // Deep Burgundy (#6B1D2F)
    public static final Color MAROON_ACCENT = new Color(0x84, 0x25, 0x3B);     // Muted Wine Accent (#84253B)
    public static final Color GOLD_ACCENT = new Color(0x8C, 0x6A, 0x3E);       // Muted Warm Amber / Gold (#8C6A3E)
    public static final Color GOLD_BG = new Color(0xF5, 0xEF, 0xE4);           // Warm Cream Amber Tint (#F5EFE4)
    public static final Color EARTHY_BROWN = new Color(0x5A, 0x47, 0x3E);      // Earthy Warm Taupe Brown (#5A473E)
    public static final Color BANANA_LEAF_GREEN = new Color(0x72, 0x8C, 0x6E); // Muted Sage Green (#728C6E)

    // Dedicated Palette Tokens
    public static final Color SAGE_MUTED = new Color(0x72, 0x8C, 0x6E);        // Muted Sage Green
    public static final Color SAGE_LIGHT = new Color(0xE8, 0xEF, 0xE8);        // Pastel Sage Light
    public static final Color SAGE_DARK = new Color(0x4E, 0x65, 0x4B);         // Deep Muted Sage
    public static final Color BURGUNDY_DEEP = new Color(0x6B, 0x1D, 0x2F);     // Deep Burgundy
    public static final Color BURGUNDY_DARK = new Color(0x4A, 0x15, 0x21);     // Dark Velvety Burgundy
    public static final Color BURGUNDY_TINT = new Color(0xF9, 0xEB, 0xEF);     // Pastel Burgundy Tint
    public static final Color CREAM_BEIGE = new Color(0xF7, 0xF4, 0xEE);       // Warm Cream Beige
    public static final Color CREAM_CARD = new Color(0xFF, 0xFD, 0xF9);        // Cream White

    // Standard Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_INPUT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    // Cached Tamil Font Family
    private static String cachedTamilFontFamily = null;

    /**
     * Resolves an authentic Tamil-compatible font installed on the system (Noto Sans Tamil, Nirmala UI, Noto Serif Tamil, Mukta Malar, Latha, Segoe UI)
     */
    public static Font getTamilFont(int style, float size) {
        if (cachedTamilFontFamily == null) {
            String[] preferredFonts = {
                "Noto Sans Tamil", "Noto Serif Tamil", "Nirmala UI", "Mukta Malar", 
                "Latha", "InaiMathi", "Tamil Sangam MN", "Segoe UI", "Dialog", "SansSerif"
            };
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] available = ge.getAvailableFontFamilyNames();
            String testTamil = "ஈகை பகுத்துண்டு பல்லுயிர் ஓம்புதல்";

            for (String pref : preferredFonts) {
                for (String av : available) {
                    if (av.equalsIgnoreCase(pref)) {
                        Font f = new Font(av, Font.PLAIN, 12);
                        if (f.canDisplayUpTo(testTamil) == -1) {
                            cachedTamilFontFamily = av;
                            break;
                        }
                    }
                }
                if (cachedTamilFontFamily != null) break;
            }

            // Fallback: search any available font capable of rendering Tamil
            if (cachedTamilFontFamily == null) {
                for (String av : available) {
                    Font f = new Font(av, Font.PLAIN, 12);
                    if (f.canDisplayUpTo(testTamil) == -1) {
                        cachedTamilFontFamily = av;
                        break;
                    }
                }
            }
            if (cachedTamilFontFamily == null) cachedTamilFontFamily = "SansSerif";
        }
        return new Font(cachedTamilFontFamily, style, (int) size);
    }

    /**
     * Custom Rounded Text Field with placeholder and focus glow border.
     */
    public static class RoundedTextField extends JTextField {
        private String placeholder;
        private boolean isFocused = false;
        private int cornerRadius = 12;

        public RoundedTextField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setFont(FONT_INPUT);
            setForeground(TEXT_DARK);
            setCaretColor(PRIMARY);
            setBorder(new EmptyBorder(10, 14, 10, 14));

            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    isFocused = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    isFocused = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Background
            g2.setColor(isFocused ? SURFACE_WHITE : BG_OFFWHITE);
            g2.fill(new RoundRectangle2D.Float(1, 1, width - 2, height - 2, cornerRadius, cornerRadius));

            // Border
            if (isFocused) {
                g2.setColor(BORDER_FOCUSED);
                g2.setStroke(new BasicStroke(1.8f));
            } else {
                g2.setColor(BORDER_GRAY);
                g2.setStroke(new BasicStroke(1.0f));
            }
            g2.draw(new RoundRectangle2D.Float(1, 1, width - 3, height - 3, cornerRadius, cornerRadius));

            // Placeholder
            if (getText().isEmpty() && !isFocused && placeholder != null) {
                g2.setFont(FONT_INPUT);
                g2.setColor(TEXT_LIGHT);
                FontMetrics fm = g2.getFontMetrics();
                int textY = (height - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, 14, textY);
            }

            g2.dispose();
            super.paintComponent(g);
        }

        public void setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
            repaint();
        }
    }

    /**
     * Custom Rounded Password Field with placeholder and focus glow border.
     */
    public static class RoundedPasswordField extends JPasswordField {
        private String placeholder;
        private boolean isFocused = false;
        private int cornerRadius = 12;

        public RoundedPasswordField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setFont(FONT_INPUT);
            setForeground(TEXT_DARK);
            setCaretColor(PRIMARY);
            setBorder(new EmptyBorder(10, 14, 10, 42)); // Right margin for show/hide eye

            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    isFocused = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    isFocused = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Background
            g2.setColor(isFocused ? SURFACE_WHITE : BG_OFFWHITE);
            g2.fill(new RoundRectangle2D.Float(1, 1, width - 2, height - 2, cornerRadius, cornerRadius));

            // Border
            if (isFocused) {
                g2.setColor(BORDER_FOCUSED);
                g2.setStroke(new BasicStroke(1.8f));
            } else {
                g2.setColor(BORDER_GRAY);
                g2.setStroke(new BasicStroke(1.0f));
            }
            g2.draw(new RoundRectangle2D.Float(1, 1, width - 3, height - 3, cornerRadius, cornerRadius));

            // Placeholder
            if (getPassword().length == 0 && !isFocused && placeholder != null) {
                g2.setFont(FONT_INPUT);
                g2.setColor(TEXT_LIGHT);
                FontMetrics fm = g2.getFontMetrics();
                int textY = (height - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, 14, textY);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Modern Rounded Button with gradient fill, hover animation and drop shadow effect.
     */
    public static class RoundedButton extends JButton {
        private Color normalColor = PRIMARY;
        private Color hoverColor = PRIMARY_HOVER;
        private boolean isHovered = false;
        private boolean isPressed = false;
        private int cornerRadius = 12;

        public RoundedButton(String text) {
            super(text);
            setFont(FONT_BUTTON);
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(12, 20, 12, 20));

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    isHovered = false;
                    isPressed = false;
                    repaint();
                }

                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    isPressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(java.awt.event.MouseEvent e) {
                    isPressed = false;
                    repaint();
                }
            });
        }

        public void setButtonColors(Color normal, Color hover) {
            this.normalColor = normal;
            this.hoverColor = hover;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Gradient Fill
            Color c1 = isPressed ? hoverColor.darker() : (isHovered ? hoverColor : normalColor);
            Color c2 = isPressed ? normalColor : (isHovered ? normalColor : normalColor.darker());
            GradientPaint gp = new GradientPaint(0, 0, c1, 0, height, c2);
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius));

            // Subtle highlight rim
            g2.setColor(new Color(255, 255, 255, isHovered ? 40 : 20));
            g2.draw(new RoundRectangle2D.Float(1, 1, width - 2, height - 2, cornerRadius, cornerRadius));

            // Draw Text
            FontMetrics fm = g2.getFontMetrics();
            Rectangle stringBounds = fm.getStringBounds(getText(), g2).getBounds();
            int textX = (width - stringBounds.width) / 2;
            int textY = (height - stringBounds.height) / 2 + fm.getAscent();

            g2.setColor(getForeground());
            g2.setFont(getFont());
            g2.drawString(getText(), textX, textY);

            g2.dispose();
        }
    }

    // =========================================================================
    // UNIVERSAL TRANSITION & FRIENDLY FEEDBACK HELPERS
    // =========================================================================

    /**
     * Performs a smooth opacity fade transition out of fromWindow and executes onComplete on EDT.
     */
    public static void fadeTransition(Window fromWindow, Runnable onComplete) {
        if (fromWindow == null) {
            if (onComplete != null) SwingUtilities.invokeLater(onComplete);
            return;
        }

        Timer fadeTimer = new Timer(15, null);
        final float[] opacity = {1.0f};

        fadeTimer.addActionListener(e -> {
            opacity[0] -= 0.08f;
            if (opacity[0] <= 0.05f) {
                fadeTimer.stop();
                fromWindow.dispose();
                if (onComplete != null) {
                    SwingUtilities.invokeLater(onComplete);
                }
            } else {
                try {
                    fromWindow.setOpacity(Math.max(0.0f, opacity[0]));
                } catch (Exception ex) {
                    fadeTimer.stop();
                    fromWindow.dispose();
                    if (onComplete != null) {
                        SwingUtilities.invokeLater(onComplete);
                    }
                }
            }
        });
        fadeTimer.start();
    }

    /**
     * Displays a friendly, elegantly styled error dialog.
     */
    public static void showFriendlyErrorDialog(Component parent, String title, String message) {
        String html = "<html><div style='width: 340px; font-family: Segoe UI, sans-serif; padding: 4px;'>" +
                "<h3 style='color: #9E1F36; margin: 0 0 6px 0; font-size: 15px;'>" + escapeHtml(title) + "</h3>" +
                "<p style='color: #2E2623; font-size: 13px; line-height: 1.4; margin: 0;'>" + escapeHtml(message) + "</p>" +
                "</div></html>";
        JOptionPane.showMessageDialog(parent, html, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Displays a friendly, elegantly styled success dialog.
     */
    public static void showFriendlySuccessDialog(Component parent, String title, String message) {
        String html = "<html><div style='width: 340px; font-family: Segoe UI, sans-serif; padding: 4px;'>" +
                "<h3 style='color: #4E654B; margin: 0 0 6px 0; font-size: 15px;'>" + escapeHtml(title) + "</h3>" +
                "<p style='color: #2E2623; font-size: 13px; line-height: 1.4; margin: 0;'>" + escapeHtml(message) + "</p>" +
                "</div></html>";
        JOptionPane.showMessageDialog(parent, html, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Displays a friendly, elegantly styled warning dialog.
     */
    public static void showFriendlyWarningDialog(Component parent, String title, String message) {
        String html = "<html><div style='width: 340px; font-family: Segoe UI, sans-serif; padding: 4px;'>" +
                "<h3 style='color: #8C6A3E; margin: 0 0 6px 0; font-size: 15px;'>" + escapeHtml(title) + "</h3>" +
                "<p style='color: #2E2623; font-size: 13px; line-height: 1.4; margin: 0;'>" + escapeHtml(message) + "</p>" +
                "</div></html>";
        JOptionPane.showMessageDialog(parent, html, title, JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Displays a friendly, elegantly styled confirmation dialog.
     */
    public static boolean showFriendlyConfirmDialog(Component parent, String title, String message) {
        String html = "<html><div style='width: 340px; font-family: Segoe UI, sans-serif; padding: 4px;'>" +
                "<h3 style='color: #6B1D2F; margin: 0 0 6px 0; font-size: 15px;'>" + escapeHtml(title) + "</h3>" +
                "<p style='color: #2E2623; font-size: 13px; line-height: 1.4; margin: 0;'>" + escapeHtml(message) + "</p>" +
                "</div></html>";
        int res = JOptionPane.showConfirmDialog(parent, html, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return res == JOptionPane.YES_OPTION;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\n", "<br>");
    }
}
