package com.fooddonation.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Custom GlassPane rendering a Custom Magic Wand Cursor with an animated Glitter Trail.
 * Spawns radiant star particles as the cursor glides across the window.
 * Passes all input events through to underlying components without interference.
 */
public class GlitterTrailGlassPane extends JComponent {

    private final List<SparkleParticle> particles = new ArrayList<>();
    private final Timer animationTimer;
    private final Random random = new Random();
    private Cursor magicWandCursor;
    private Point lastMousePoint = null;

    // Palette for sparkling glitter harmonized with Burgundy, Sage, Warm Cream & Diamond
    private static final Color[] GLITTER_COLORS = {
            new Color(0xD4, 0xAF, 0x37), // Warm Antique Gold
            new Color(0x9B, 0xB3, 0x9A), // Pastel Sage Glow
            new Color(0x84, 0x25, 0x3B), // Velvet Burgundy Sparkle
            new Color(0xFF, 0xFD, 0xF9), // Pure Cream Diamond
            new Color(0xE8, 0xEF, 0xE8)  // Soft Sage Starlight
    };

    public GlitterTrailGlassPane(Window parentWindow) {
        setOpaque(false);
        setFocusable(false);

        // Generate Custom Magic Wand Cursor
        createMagicWandCursor();
        if (magicWandCursor != null) {
            parentWindow.setCursor(magicWandCursor);
            setCursor(magicWandCursor);
        }

        // 60 FPS Physics & Render Loop
        animationTimer = new Timer(16, e -> updateParticles());

        // Global AWT Event Listener to reliably capture mouse motion anywhere in the window
        long eventMask = AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK;
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (event instanceof MouseEvent) {
                    MouseEvent me = (MouseEvent) event;
                    if (parentWindow != null && parentWindow.isShowing() && SwingUtilities.isDescendingFrom(me.getComponent(), parentWindow)) {
                        Point p = SwingUtilities.convertPoint(me.getComponent(), me.getPoint(), GlitterTrailGlassPane.this);
                        if (event.getID() == MouseEvent.MOUSE_MOVED || event.getID() == MouseEvent.MOUSE_DRAGGED) {
                            spawnSparkles(p.x, p.y);
                        }
                    }
                }
            }
        }, eventMask);
    }

    public GlitterTrailGlassPane(JFrame parentFrame) {
        this((Window) parentFrame);
    }

    /**
     * Spawns multiple shimmering glitter particles at the given cursor coordinate.
     */
    public void spawnSparkles(int x, int y) {
        // Rate-limit by distance to keep density pleasant and crisp
        if (lastMousePoint != null && lastMousePoint.distance(x, y) < 4) {
            return;
        }
        lastMousePoint = new Point(x, y);

        int count = 2 + random.nextInt(3); // 2-4 sparkles per motion burst
        for (int i = 0; i < count; i++) {
            float vx = (random.nextFloat() - 0.5f) * 2.8f;
            float vy = (random.nextFloat() * 1.5f) - 1.2f; // float slightly upward/spread
            float size = 5.0f + random.nextFloat() * 8.0f;
            Color color = GLITTER_COLORS[random.nextInt(GLITTER_COLORS.length)];
            int maxLife = 22 + random.nextInt(18);

            particles.add(new SparkleParticle(x, y, vx, vy, size, color, maxLife));
        }

        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
        repaint();
    }

    /**
     * Updates physics, fading, and rotation for all active sparkles.
     */
    private void updateParticles() {
        if (particles.isEmpty()) {
            animationTimer.stop();
            repaint();
            return;
        }

        Iterator<SparkleParticle> it = particles.iterator();
        while (it.hasNext()) {
            SparkleParticle p = it.next();
            p.update();
            if (p.isDead()) {
                it.remove();
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (particles.isEmpty()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        for (SparkleParticle p : particles) {
            p.render(g2);
        }

        g2.dispose();
    }

    /**
     * Creates a high-definition 32x32 Magic Wand cursor with metallic stem and radiant star tip.
     */
    private void createMagicWandCursor() {
        int size = 32;
        BufferedImage cursorImg = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = cursorImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Wand Tip Hotspot coordinate: (4, 4)
        // Wand Shaft extending diagonally from (8, 8) to (26, 26)
        g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(0x1E, 0x29, 0x3B)); // Dark Titanium shaft
        g2.drawLine(9, 9, 27, 27);

        // Silver/Gold Rings on Wand handle
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(0xFB, 0xBF, 0x24)); // Gold Ring
        g2.drawLine(14, 14, 16, 16);
        g2.setColor(new Color(0xE2, 0xE8, 0xF0)); // Silver Tip cap
        g2.drawLine(8, 8, 10, 10);

        // Sparkling Magic Star at Tip (hotspot center: 4, 4)
        drawSparkleShape(g2, 4, 4, 6.0f, new Color(0xFE, 0xF0, 0x8A), 0f);
        drawSparkleShape(g2, 4, 4, 3.5f, Color.WHITE, 0f);

        g2.dispose();

        magicWandCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg,
                new Point(4, 4),
                "MagicWandCursor"
        );
    }

    /**
     * Static helper to draw a 4-pointed glittering star.
     */
    private static void drawSparkleShape(Graphics2D g2, float cx, float cy, float radius, Color color, float angle) {
        AffineTransform oldTx = g2.getTransform();
        g2.translate(cx, cy);
        if (angle != 0) {
            g2.rotate(angle);
        }

        Path2D.Float star = new Path2D.Float();
        float inner = radius * 0.22f;

        star.moveTo(0, -radius);
        star.quadTo(0, -inner, inner, 0);
        star.quadTo(inner, 0, radius, 0);
        star.quadTo(0, inner, 0, radius);
        star.quadTo(-inner, 0, -radius, 0);
        star.quadTo(0, -inner, 0, -radius);
        star.closePath();

        g2.setColor(color);
        g2.fill(star);

        g2.setTransform(oldTx);
    }

    /**
     * Represents an individual glitter sparkle particle in the physics trail.
     */
    private static class SparkleParticle {
        float x, y;
        float vx, vy;
        float size;
        Color baseColor;
        int maxLife;
        int age = 0;
        float rotation = 0;
        float rotSpeed;

        public SparkleParticle(float x, float y, float vx, float vy, float size, Color baseColor, int maxLife) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.baseColor = baseColor;
            this.maxLife = maxLife;
            this.rotSpeed = (new Random().nextFloat() - 0.5f) * 0.25f;
        }

        public void update() {
            x += vx;
            y += vy;
            vy += 0.08f; // Gentle downward drift
            vx *= 0.94f; // Air drag
            vy *= 0.94f;
            rotation += rotSpeed;
            age++;
        }

        public boolean isDead() {
            return age >= maxLife;
        }

        public void render(Graphics2D g2) {
            float progress = (float) age / (float) maxLife;
            float alpha = Math.max(0.0f, 1.0f - progress);
            float currentSize = size * (1.0f - (progress * 0.4f));

            Color renderColor = new Color(
                    baseColor.getRed(),
                    baseColor.getGreen(),
                    baseColor.getBlue(),
                    (int) (alpha * 240)
            );

            drawSparkleShape(g2, x, y, currentSize, renderColor, rotation);

            // Shimmering white central core
            Color coreColor = new Color(255, 255, 255, (int) (alpha * 255));
            drawSparkleShape(g2, x, y, currentSize * 0.45f, coreColor, rotation + 0.78f);
        }
    }

    public Cursor getMagicWandCursor() {
        return magicWandCursor;
    }
}
