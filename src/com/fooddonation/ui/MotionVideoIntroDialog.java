package com.fooddonation.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * MotionVideoIntroDialog
 * 
 * Plays the introductory motion video ("Hands placing food on plate")
 * in front of the application. Upon video completion, reveals the Title "ஈகை"
 * and Tagline "பகுத்துண்டு பல்லுயிர் ஓம்புதல்" with glowing cultural motif,
 * and then automatically transitions into the main application.
 */
public class MotionVideoIntroDialog extends JWindow {

    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;
    private static final int TOTAL_FRAMES = 192;
    private static final int FRAME_DELAY_MS = 41; // ~24 FPS

    private final Runnable onComplete;
    private final List<File> frameFiles = new ArrayList<>();
    private final BufferedImage[] frameCache = new BufferedImage[TOTAL_FRAMES];

    private int currentFrame = 0;
    private boolean showingTitleCard = false;
    private Timer playbackTimer;
    private BufferedImage currentImage = null;

    public MotionVideoIntroDialog(Window owner, Runnable onComplete) {
        super(owner);
        this.onComplete = onComplete;
        initUI();
        locateFrames();
    }

    private void initUI() {
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setBackground(new Color(0x14, 0x0A, 0x0D));

        // Content panel
        JPanel canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                if (!showingTitleCard) {
                    // 1. Draw video frame
                    if (currentImage != null) {
                        g2.drawImage(currentImage, 0, 0, w, h, null);
                    } else {
                        g2.setColor(new Color(0x14, 0x0A, 0x0D));
                        g2.fillRect(0, 0, w, h);
                    }

                    // Vignette gradient
                    GradientPaint vignette = new GradientPaint(
                            0, h - 80, new Color(20, 10, 13, 0),
                            0, h, new Color(20, 10, 13, 180));
                    g2.setPaint(vignette);
                    g2.fillRect(0, h - 80, w, 80);

                    // 2. Progress Bar
                    int barH = 5;
                    float pct = (float) currentFrame / (float) Math.max(1, TOTAL_FRAMES);
                    int fillW = (int) (w * pct);

                    g2.setColor(new Color(255, 255, 255, 40));
                    g2.fillRect(0, h - barH, w, barH);

                    GradientPaint barGrad = new GradientPaint(
                            0, 0, new Color(0x72, 0x8C, 0x6E), // Muted Sage
                            w, 0, new Color(0x6B, 0x1D, 0x2F)); // Deep Burgundy
                    g2.setPaint(barGrad);
                    g2.fillRect(0, h - barH, fillW, barH);

                } else {
                    // Title & Tagline Card
                    // Rich deep burgundy gradient backdrop
                    GradientPaint bg = new GradientPaint(
                            0, 0, new Color(0x28, 0x0A, 0x11),
                            w, h, new Color(0x14, 0x0A, 0x0D));
                    g2.setPaint(bg);
                    g2.fillRect(0, 0, w, h);

                    // Subtle radial aura
                    g2.setColor(new Color(0x8C, 0x6A, 0x3E, 45));
                    g2.fillOval(w / 2 - 220, h / 2 - 180, 440, 360);

                    // Emblem "ஈ"
                    int emblemSize = 74;
                    int emblemX = (w - emblemSize) / 2;
                    int emblemY = h / 2 - 120;

                    GradientPaint embBg = new GradientPaint(
                            emblemX, emblemY, new Color(0x6B, 0x1D, 0x2F),
                            emblemX + emblemSize, emblemY + emblemSize, new Color(0x3B, 0x0E, 0x18));
                    g2.setPaint(embBg);
                    g2.fill(new RoundRectangle2D.Float(emblemX, emblemY, emblemSize, emblemSize, 20, 20));

                    g2.setColor(new Color(0x8C, 0x6A, 0x3E));
                    g2.setStroke(new BasicStroke(2f));
                    g2.draw(new RoundRectangle2D.Float(emblemX, emblemY, emblemSize, emblemSize, 20, 20));

                    g2.setFont(UITheme.getTamilFont(Font.BOLD, 40));
                    g2.setColor(Color.WHITE);
                    FontMetrics fmEmb = g2.getFontMetrics();
                    int embTextX = emblemX + (emblemSize - fmEmb.stringWidth("ஈ")) / 2;
                    int embTextY = emblemY + ((emblemSize - fmEmb.getHeight()) / 2) + fmEmb.getAscent();
                    g2.drawString("ஈ", embTextX, embTextY);

                    // Main Title: ஈகை
                    g2.setFont(UITheme.getTamilFont(Font.BOLD, 52));
                    g2.setColor(new Color(0xF7, 0xF4, 0xEE)); // Warm Cream Beige
                    FontMetrics fmTitle = g2.getFontMetrics();
                    String titleStr = "ஈகை";
                    int titleX = (w - fmTitle.stringWidth(titleStr)) / 2;
                    int titleY = emblemY + emblemSize + 48;
                    g2.drawString(titleStr, titleX, titleY);

                    // Kolam divider bar
                    int divW = 140;
                    int divX = (w - divW) / 2;
                    int divY = titleY + 12;
                    GradientPaint divGrad = new GradientPaint(
                            divX, divY, new Color(0x72, 0x8C, 0x6E),
                            divX + divW, divY, new Color(0x8C, 0x6A, 0x3E));
                    g2.setPaint(divGrad);
                    g2.fillRoundRect(divX, divY, divW, 3, 3, 3);

                    // Tagline: பகுத்துண்டு பல்லுயிர் ஓம்புதல்
                    g2.setFont(UITheme.getTamilFont(Font.BOLD, 22));
                    g2.setColor(new Color(0x9B, 0xB3, 0x9A)); // Pastel Sage Green
                    FontMetrics fmTag = g2.getFontMetrics();
                    String tagStr = "பகுத்துண்டு பல்லுயிர் ஓம்புதல்";
                    int tagX = (w - fmTag.stringWidth(tagStr)) / 2;
                    int tagY = divY + 34;
                    g2.drawString(tagStr, tagX, tagY);

                    // Subtle loading hint
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    g2.setColor(new Color(0xE5, 0xDF, 0xD5, 160));
                    String sub = "வலைத்தளத்திற்குச் செல்கிறது...";
                    FontMetrics fmSub = g2.getFontMetrics();
                    g2.drawString(sub, (w - fmSub.stringWidth(sub)) / 2, tagY + 36);
                }

                g2.dispose();
            }
        };

        setContentPane(canvas);
    }

    private void locateFrames() {
        String[] candidateDirs = {
                "assets/intro_frames",
                "project/assets/intro_frames",
                "../assets/intro_frames",
                "C:/Users/monis/Downloads/project/project/assets/intro_frames",
                "C:/Users/monis/Downloads/project/assets/intro_frames"
        };

        File foundDir = null;
        for (String c : candidateDirs) {
            File d = new File(c);
            if (d.exists() && d.isDirectory()) {
                File[] list = d.listFiles((dir, name) -> name.endsWith(".jpg"));
                if (list != null && list.length > 0) {
                    foundDir = d;
                    break;
                }
            }
        }

        if (foundDir != null) {
            for (int i = 0; i < TOTAL_FRAMES; i++) {
                File f = new File(foundDir, String.format("frame_%03d.jpg", i));
                if (f.exists()) {
                    frameFiles.add(f);
                }
            }
        }
    }

    public void playIntro() {
        if (frameFiles.isEmpty()) {
            // If frames are missing, proceed directly to app
            finishAndOpenApp();
            return;
        }

        setVisible(true);

        // Preload first 20 frames into cache for instant smooth start
        int initialPreload = Math.min(25, frameFiles.size());
        for (int i = 0; i < initialPreload; i++) {
            loadFrame(i);
        }
        currentImage = frameCache[0];
        repaint();

        // Background worker to keep frame cache filled
        new Thread(() -> {
            for (int i = initialPreload; i < frameFiles.size(); i++) {
                loadFrame(i);
                try {
                    Thread.sleep(15);
                } catch (InterruptedException ignored) {}
            }
        }).start();

        // Start playback timer
        playbackTimer = new Timer(FRAME_DELAY_MS, e -> {
            currentFrame++;
            if (currentFrame < frameFiles.size()) {
                currentImage = getFrame(currentFrame);
                repaint();
            } else {
                // Video finished! Stop timer and show Title Card
                playbackTimer.stop();
                showTitleCardAndTransition();
            }
        });
        playbackTimer.start();
    }

    private BufferedImage getFrame(int index) {
        if (index >= 0 && index < frameCache.length && frameCache[index] != null) {
            return frameCache[index];
        }
        return loadFrame(index);
    }

    private BufferedImage loadFrame(int index) {
        if (index < 0 || index >= frameFiles.size()) return null;
        if (frameCache[index] != null) return frameCache[index];
        try {
            BufferedImage img = ImageIO.read(frameFiles.get(index));
            frameCache[index] = img;
            return img;
        } catch (Exception ex) {
            return null;
        }
    }

    private void showTitleCardAndTransition() {
        showingTitleCard = true;
        repaint();

        // Hold title card for 1.8 seconds, then open main app
        Timer holdTimer = new Timer(1800, ev -> {
            ((Timer) ev.getSource()).stop();
            finishAndOpenApp();
        });
        holdTimer.setRepeats(false);
        holdTimer.start();
    }

    private void finishAndOpenApp() {
        SwingUtilities.invokeLater(() -> {
            setVisible(false);
            dispose();
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
}
