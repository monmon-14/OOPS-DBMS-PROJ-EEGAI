package com.fooddonation.ui;

import com.fooddonation.model.Delivery;
import com.fooddonation.model.DeliveryStatus;
import com.fooddonation.service.DeliveryService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Delivery Tracking Component supporting two distinct operational modes:
 *
 * 1. Volunteer Stepper Mode:
 *    Interactive 3-stage status stepper (Accepted -> Picked Up -> Delivered).
 *    Includes an advance action button that steps forward one stage at a time and timestamps each milestone.
 *    Prompts for actual meals delivered upon reaching the 'Delivered' stage.
 *
 * 2. Read-Only Live Timeline Mode (for NGO and Donor):
 *    Comprehensive milestone audit timeline per donation with auto-refresh every 10 seconds.
 */
public class DeliveryTrackingPanel extends JPanel {

    public enum Mode {
        VOLUNTEER_STEPPER,
        READONLY_LIVE_TIMELINE
    }

    private final Mode mode;
    private final DeliveryService deliveryService;
    private Delivery currentDelivery;
    private int donationId;
    private Runnable onStatusUpdated;

    // Stepper UI components (Volunteer Mode)
    private JButton advanceStepBtn;
    private JLabel statusBannerLabel;
    private JPanel stepperCanvas;

    // Timeline UI components (Read-Only Mode)
    private JPanel timelineContainer;
    private JLabel liveStatusPill;
    private JLabel lastRefreshedLabel;
    private Timer autoRefreshTimer;

    /**
     * Factory method: Creates an interactive Volunteer Stepper Panel.
     */
    public static DeliveryTrackingPanel createVolunteerStepper(Delivery delivery, DeliveryService service, Runnable onStatusUpdated) {
        return new DeliveryTrackingPanel(Mode.VOLUNTEER_STEPPER, delivery, (delivery != null) ? delivery.getDonationId() : 0, service, onStatusUpdated);
    }

    /**
     * Factory method: Creates a Read-Only Live Timeline Panel for Donors and NGOs.
     */
    public static DeliveryTrackingPanel createLiveTimeline(int donationId, DeliveryService service) {
        return new DeliveryTrackingPanel(Mode.READONLY_LIVE_TIMELINE, null, donationId, service, null);
    }

    /**
     * Opens a modal dialog displaying the Live Delivery Timeline with 10s auto-refresh.
     */
    public static void showLiveTimelineDialog(Window owner, int donationId, String foodTitle, DeliveryService service) {
        JDialog dialog = new JDialog(owner, "Live Delivery Route Tracking — " + foodTitle, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(680, 620);
        dialog.setLocationRelativeTo(owner);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        DeliveryTrackingPanel panel = createLiveTimeline(donationId, service);
        dialog.setContentPane(panel);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                panel.stopAutoRefresh();
            }
        });

        dialog.setVisible(true);
    }

    private DeliveryTrackingPanel(Mode mode, Delivery delivery, int donationId, DeliveryService service, Runnable onStatusUpdated) {
        this.mode = mode;
        this.currentDelivery = delivery;
        this.donationId = donationId;
        this.deliveryService = (service != null) ? service : new DeliveryService();
        this.onStatusUpdated = onStatusUpdated;

        setLayout(new BorderLayout());
        setBackground(UITheme.SURFACE_WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true),
                new EmptyBorder(16, 20, 16, 20)
        ));

        if (mode == Mode.VOLUNTEER_STEPPER) {
            initVolunteerStepperUI();
        } else {
            initReadonlyTimelineUI();
            startAutoRefreshTimer();
        }

        // Clean up timer when panel is removed from hierarchy
        addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {}
            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) { stopAutoRefresh(); }
            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
        });
    }

    // =========================================================================
    // 1. VOLUNTEER INTERACTIVE STEPPER MODE
    // =========================================================================
    private void initVolunteerStepperUI() {
        removeAll();
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);

        // Header info: Mission Title & Details
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        String foodTitle = (currentDelivery != null && currentDelivery.getFoodName() != null)
                ? currentDelivery.getFoodName() : "Active Delivery Route";
        int qty = (currentDelivery != null) ? currentDelivery.getQuantity() : 40;

        JLabel titleLbl = new JLabel(foodTitle + " (" + qty + " meals)");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(UITheme.TEXT_DARK);

        String donorNgoRoute = (currentDelivery != null)
                ? "From: " + currentDelivery.getDonorName() + "  ->  To: " + currentDelivery.getNgoName()
                : "Courier dispatch route";
        JLabel routeLbl = new JLabel(donorNgoRoute);
        routeLbl.setFont(UITheme.FONT_SMALL);
        routeLbl.setForeground(UITheme.TEXT_MUTED);

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);
        textStack.add(titleLbl);
        textStack.add(Box.createVerticalStrut(2));
        textStack.add(routeLbl);
        headerPanel.add(textStack, BorderLayout.WEST);

        // Status badge on the right
        DeliveryStatus currentStatus = (currentDelivery != null) ? currentDelivery.getStatus() : DeliveryStatus.ACCEPTED;
        statusBannerLabel = new JLabel("● " + currentStatus.getDisplayName());
        statusBannerLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusBannerLabel.setForeground(currentStatus.getBadgeTextColor());
        statusBannerLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(currentStatus.getBadgeBgColor().darker(), 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        statusBannerLabel.setOpaque(true);
        statusBannerLabel.setBackground(currentStatus.getBadgeBgColor());
        headerPanel.add(statusBannerLabel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Center: Custom Painted Stepper Canvas
        stepperCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawHorizontalStepper(g, getWidth(), getHeight());
            }
        };
        stepperCanvas.setPreferredSize(new Dimension(500, 120));
        stepperCanvas.setOpaque(false);
        add(stepperCanvas, BorderLayout.CENTER);

        // Bottom Controls: Action Advance Button
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setOpaque(false);

        advanceStepBtn = new JButton();
        advanceStepBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        advanceStepBtn.setFocusPainted(false);
        advanceStepBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        advanceStepBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.PRIMARY_DARK, 1, true),
                new EmptyBorder(9, 20, 9, 20)
        ));

        updateAdvanceButtonState();

        advanceStepBtn.addActionListener(e -> handleAdvanceStepClick());
        bottomBar.add(advanceStepBtn);
        add(bottomBar, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    private void updateAdvanceButtonState() {
        if (currentDelivery == null) {
            advanceStepBtn.setEnabled(false);
            advanceStepBtn.setText("No Mission Selected");
            return;
        }

        DeliveryStatus status = currentDelivery.getStatus();
        if (status == DeliveryStatus.ACCEPTED) {
            advanceStepBtn.setEnabled(true);
            advanceStepBtn.setText("Advance: Mark as Picked Up");
            advanceStepBtn.setBackground(new Color(0x6B, 0x1D, 0x2F)); // Deep Burgundy
            advanceStepBtn.setForeground(Color.WHITE);
        } else if (status == DeliveryStatus.PICKED_UP) {
            advanceStepBtn.setEnabled(true);
            advanceStepBtn.setText("Advance: Mark as Delivered");
            advanceStepBtn.setBackground(new Color(0x72, 0x8C, 0x6E)); // Muted Sage Green
            advanceStepBtn.setForeground(Color.WHITE);
        } else {
            // DELIVERED
            advanceStepBtn.setEnabled(false);
            int meals = (currentDelivery.getMealsDelivered() != null) ? currentDelivery.getMealsDelivered() : currentDelivery.getQuantity();
            advanceStepBtn.setText("Fully Delivered (" + meals + " meals)");
            advanceStepBtn.setBackground(new Color(0xE0, 0xD7, 0xC9)); // Warm Beige
            advanceStepBtn.setForeground(UITheme.TEXT_MUTED);
        }
    }

    private void handleAdvanceStepClick() {
        if (currentDelivery == null) return;
        DeliveryStatus status = currentDelivery.getStatus();

        if (status == DeliveryStatus.ACCEPTED) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "<html><div style='font-family: Segoe UI; width: 320px;'>" +
                            "<h3 style='color: #1D4ED8; margin-bottom: 4px;'>Confirm Food Pickup</h3>" +
                            "<p>Have you arrived at <b>" + currentDelivery.getDonorName() + "</b> and collected the surplus food?</p>" +
                            "<p style='color: #64748B;'>Pickup address: " + currentDelivery.getDonorAddress() + "</p>" +
                            "</div></html>",
                    "Advance Delivery Step",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                advanceStepBtn.setEnabled(false);
                advanceStepBtn.setText("Advancing...");

                SwingUtilities.invokeLater(() -> {
                    var res = deliveryService.updateStatus(currentDelivery.getId(), DeliveryStatus.PICKED_UP, null);
                    if (res.isSuccess()) {
                        this.currentDelivery = res.getDelivery();
                        initVolunteerStepperUI();
                        if (onStatusUpdated != null) onStatusUpdated.run();
                    } else {
                        JOptionPane.showMessageDialog(this, res.getErrorMessage(), "Step Failed", JOptionPane.ERROR_MESSAGE);
                        updateAdvanceButtonState();
                    }
                });
            }

        } else if (status == DeliveryStatus.PICKED_UP) {
            // Ask for actual meals delivered
            int plannedQuantity = currentDelivery.getQuantity();
            String inputStr = (String) JOptionPane.showInputDialog(this,
                    "<html><div style='font-family: Segoe UI; width: 330px;'>" +
                            "<h3 style='color: #15803D; margin-bottom: 4px;'>Confirm Final Shelter Drop-off</h3>" +
                            "<p>Delivered to: <b>" + currentDelivery.getNgoName() + "</b> (" + currentDelivery.getNgoAddress() + ")</p>" +
                            "<p><b>Enter actual number of meal portions delivered:</b></p>" +
                            "</div></html>",
                    "Complete Delivery",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    String.valueOf(plannedQuantity));

            if (inputStr != null && !inputStr.trim().isEmpty()) {
                try {
                    int actualMeals = Integer.parseInt(inputStr.trim());
                    if (actualMeals <= 0) {
                        JOptionPane.showMessageDialog(this, "Meals delivered must be greater than 0.", "Invalid Quantity", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    advanceStepBtn.setEnabled(false);
                    advanceStepBtn.setText("Completing...");

                    SwingUtilities.invokeLater(() -> {
                        var res = deliveryService.updateStatus(currentDelivery.getId(), DeliveryStatus.DELIVERED, actualMeals);
                        if (res.isSuccess()) {
                            this.currentDelivery = res.getDelivery();
                            JOptionPane.showMessageDialog(this,
                                    "<html><div style='font-family: Segoe UI; width: 320px;'>" +
                                            "<h3 style='color: #15803D; margin-bottom: 4px;'>Delivery Completed!</h3>" +
                                            "<p style='color: #334155;'><b>" + actualMeals + " meals</b> safely received by " + currentDelivery.getNgoName() + ".</p>" +
                                            "<p style='color: #64748B;'>Donation and claim records have been synchronized.</p>" +
                                            "</div></html>",
                                    "Mission Complete",
                                    JOptionPane.INFORMATION_MESSAGE);

                            initVolunteerStepperUI();
                            if (onStatusUpdated != null) onStatusUpdated.run();
                        } else {
                            JOptionPane.showMessageDialog(this, res.getErrorMessage(), "Delivery Failed", JOptionPane.ERROR_MESSAGE);
                            updateAdvanceButtonState();
                        }
                    });
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid numeric number of meals.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void drawHorizontalStepper(Graphics g, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int currentStep = (currentDelivery != null) ? currentDelivery.getStatus().getStepIndex() : 0;

        int nodeY = 32;
        int nodeRadius = 16;
        int stepCount = 3;
        int paddingX = 70;
        int usableWidth = w - (paddingX * 2);
        int stepSpacing = usableWidth / (stepCount - 1);

        String[] stepTitles = {"1. Accepted", "2. Picked Up", "3. Delivered"};
        String[] stepSubs = {
                (currentDelivery != null) ? currentDelivery.getFormattedCreatedAt() : "Pending",
                (currentDelivery != null) ? currentDelivery.getFormattedPickedAt() : "Pending",
                (currentDelivery != null) ? currentDelivery.getFormattedDeliveredAt() : "Pending"
        };

        // 1. Draw Connecting Progress Lines
        for (int i = 0; i < stepCount - 1; i++) {
            int x1 = paddingX + (i * stepSpacing);
            int x2 = paddingX + ((i + 1) * stepSpacing);

            boolean isDone = currentStep > i;
            g2.setColor(isDone ? new Color(0x72, 0x8C, 0x6E) : new Color(0xE0, 0xD7, 0xC9));
            g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x1, nodeY, x2, nodeY);
        }

        // 2. Draw Nodes & Text
        for (int i = 0; i < stepCount; i++) {
            int cx = paddingX + (i * stepSpacing);
            boolean isCompleted = i < currentStep;
            boolean isCurrent = i == currentStep;

            // Outer ring for current step
            if (isCurrent) {
                g2.setColor(new Color(0xE8, 0xEF, 0xE8));
                g2.fill(new Ellipse2D.Float(cx - nodeRadius - 5, nodeY - nodeRadius - 5, (nodeRadius + 5) * 2, (nodeRadius + 5) * 2));
            }

            // Node Circle
            if (isCompleted || isCurrent) {
                g2.setColor(new Color(0x72, 0x8C, 0x6E));
            } else {
                g2.setColor(new Color(0xD8, 0xCE, 0xBF));
            }
            g2.fill(new Ellipse2D.Float(cx - nodeRadius, nodeY - nodeRadius, nodeRadius * 2, nodeRadius * 2));

            // Inner icon / text
            g2.setColor(Color.WHITE);
            if (isCompleted) {
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx - 5, nodeY, cx - 1, nodeY + 4);
                g2.drawLine(cx - 1, nodeY + 4, cx + 5, nodeY - 4);
            } else {
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                String symbol = String.valueOf(i + 1);
                int sw = fm.stringWidth(symbol);
                g2.drawString(symbol, cx - (sw / 2), nodeY + 4);
            }

            // Step Title
            g2.setColor(isCurrent ? UITheme.TEXT_DARK : (isCompleted ? UITheme.PRIMARY : UITheme.TEXT_MUTED));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            FontMetrics fmTitle = g2.getFontMetrics();
            int titleW = fmTitle.stringWidth(stepTitles[i]);
            g2.drawString(stepTitles[i], cx - (titleW / 2), nodeY + nodeRadius + 18);

            // Subtitle / Timestamp
            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            FontMetrics fmSub = g2.getFontMetrics();
            String subText = stepSubs[i];
            if (subText.length() > 22) subText = subText.substring(0, 20) + "..";
            int subW = fmSub.stringWidth(subText);
            g2.drawString(subText, cx - (subW / 2), nodeY + nodeRadius + 32);
        }

        g2.dispose();
    }

    public void setDelivery(Delivery delivery) {
        this.currentDelivery = delivery;
        if (mode == Mode.VOLUNTEER_STEPPER) {
            initVolunteerStepperUI();
        }
    }

    // =========================================================================
    // 2. READ-ONLY LIVE TIMELINE MODE (For NGOs & Donors)
    // =========================================================================
    private void initReadonlyTimelineUI() {
        removeAll();
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);

        // Header Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JPanel leftHeader = new JPanel();
        leftHeader.setLayout(new BoxLayout(leftHeader, BoxLayout.Y_AXIS));
        leftHeader.setOpaque(false);

        JLabel title = new JLabel("Live Food Delivery Pipeline");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(UITheme.TEXT_DARK);

        lastRefreshedLabel = new JLabel("Auto-refreshing every 10s...");
        lastRefreshedLabel.setFont(UITheme.FONT_SMALL);
        lastRefreshedLabel.setForeground(UITheme.TEXT_MUTED);

        leftHeader.add(title);
        leftHeader.add(Box.createVerticalStrut(2));
        leftHeader.add(lastRefreshedLabel);
        topBar.add(leftHeader, BorderLayout.WEST);

        // Right status indicator & manual refresh
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightHeader.setOpaque(false);

        liveStatusPill = new JLabel("● LIVE TRACKING");
        liveStatusPill.setFont(new Font("Segoe UI", Font.BOLD, 11));
        liveStatusPill.setForeground(new Color(0x4E, 0x65, 0x4B)); // Dark Sage
        liveStatusPill.setBackground(new Color(0xE8, 0xEF, 0xE8)); // Pastel Sage
        liveStatusPill.setOpaque(true);
        liveStatusPill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xA9, 0xBF, 0xA8), 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setToolTipText("Refresh timeline now");
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setBackground(Color.WHITE);
        refreshBtn.setForeground(UITheme.TEXT_DARK);
        refreshBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        refreshBtn.addActionListener(e -> reloadTimelineData());

        rightHeader.add(liveStatusPill);
        rightHeader.add(refreshBtn);
        topBar.add(rightHeader, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // Center: Scrollable Vertical Timeline
        timelineContainer = new JPanel();
        timelineContainer.setLayout(new BoxLayout(timelineContainer, BoxLayout.Y_AXIS));
        timelineContainer.setOpaque(false);
        timelineContainer.setBorder(new EmptyBorder(8, 6, 8, 6));

        JScrollPane scrollPane = new JScrollPane(timelineContainer);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true));
        scrollPane.getViewport().setBackground(UITheme.SURFACE_WHITE);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);

        reloadTimelineData();
    }

    private void reloadTimelineData() {
        new Thread(() -> {
            DeliveryService.DeliveryTimeline timeline = deliveryService.getTimeline(donationId);
            SwingUtilities.invokeLater(() -> {
                renderTimelineNodes(timeline);
                lastRefreshedLabel.setText("Last updated: " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm:ss a")));
            });
        }).start();
    }

    private void renderTimelineNodes(DeliveryService.DeliveryTimeline timeline) {
        timelineContainer.removeAll();

        // Summary Card at top of timeline
        JPanel summaryCard = new JPanel(new BorderLayout());
        summaryCard.setBackground(UITheme.BG_OFFWHITE);
        summaryCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_GRAY, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel foodLbl = new JLabel(timeline.getFoodName() + " (" + timeline.getQuantity() + " meals)");
        foodLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        foodLbl.setForeground(UITheme.TEXT_DARK);

        JLabel statusBadge = new JLabel(" " + timeline.getCurrentStatus() + " ");
        statusBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusBadge.setForeground(UITheme.PRIMARY);
        statusBadge.setBackground(UITheme.PRIMARY_BG_TINT);
        statusBadge.setOpaque(true);
        statusBadge.setBorder(BorderFactory.createLineBorder(new Color(0xA9, 0xBF, 0xA8), 1, true));

        summaryCard.add(foodLbl, BorderLayout.WEST);
        summaryCard.add(statusBadge, BorderLayout.EAST);
        timelineContainer.add(summaryCard);
        timelineContainer.add(Box.createVerticalStrut(14));

        List<DeliveryService.TimelineEvent> events = timeline.getEvents();
        for (int i = 0; i < events.size(); i++) {
            DeliveryService.TimelineEvent ev = events.get(i);
            boolean isLast = (i == events.size() - 1);
            timelineContainer.add(createTimelineEventRow(ev, isLast));
        }

        timelineContainer.revalidate();
        timelineContainer.repaint();
    }

    private JPanel createTimelineEventRow(DeliveryService.TimelineEvent event, boolean isLast) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));

        // Left Node & vertical connector line
        JPanel leftIndicator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int cx = getWidth() / 2;
                int cy = 14;
                int r = 10;

                // Vertical line
                if (!isLast) {
                    g2.setColor(event.isCompleted() ? new Color(0x72, 0x8C, 0x6E) : new Color(0xE0, 0xD7, 0xC9));
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.drawLine(cx, cy + r, cx, getHeight());
                }

                // Circle Node
                if (event.isCompleted()) {
                    g2.setColor(new Color(0x72, 0x8C, 0x6E)); // Muted Sage Green
                    g2.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(cx - 4, cy, cx - 1, cy + 3);
                    g2.drawLine(cx - 1, cy + 3, cx + 4, cy - 3);
                } else if (event.isCurrentActive()) {
                    g2.setColor(new Color(0xE8, 0xEF, 0xE8)); // Pastel Sage
                    g2.fill(new Ellipse2D.Float(cx - r - 4, cy - r - 4, (r + 4) * 2, (r + 4) * 2));
                    g2.setColor(new Color(0x6B, 0x1D, 0x2F)); // Deep Burgundy
                    g2.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                } else {
                    g2.setColor(new Color(0xD8, 0xCE, 0xBF)); // Warm Taupe
                    g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                }
                g2.dispose();
            }
        };
        leftIndicator.setPreferredSize(new Dimension(32, 75));
        leftIndicator.setOpaque(false);
        row.add(leftIndicator, BorderLayout.WEST);

        // Right details panel
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);
        detailsPanel.setBorder(new EmptyBorder(2, 0, 10, 0));

        JPanel titleLine = new JPanel(new BorderLayout());
        titleLine.setOpaque(false);

        String stagePrefix = (event.getIcon() != null && !event.getIcon().trim().isEmpty())
                ? event.getIcon() + "  " : "";
        JLabel stageLbl = new JLabel(stagePrefix + event.getStageName());
        stageLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        stageLbl.setForeground(event.isCompleted() ? UITheme.TEXT_DARK : UITheme.TEXT_MUTED);

        JLabel timeLbl = new JLabel(event.getFormattedTimestamp());
        timeLbl.setFont(UITheme.FONT_SMALL);
        timeLbl.setForeground(event.isCompleted() ? UITheme.PRIMARY : UITheme.TEXT_LIGHT);

        titleLine.add(stageLbl, BorderLayout.WEST);
        titleLine.add(timeLbl, BorderLayout.EAST);

        JLabel descLbl = new JLabel(event.getTitle());
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLbl.setForeground(UITheme.TEXT_MUTED);

        JLabel detailLbl = new JLabel(event.getDetail());
        detailLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detailLbl.setForeground(UITheme.TEXT_LIGHT);

        detailsPanel.add(titleLine);
        detailsPanel.add(Box.createVerticalStrut(2));
        detailsPanel.add(descLbl);
        detailsPanel.add(Box.createVerticalStrut(1));
        detailsPanel.add(detailLbl);

        row.add(detailsPanel, BorderLayout.CENTER);
        return row;
    }

    private void startAutoRefreshTimer() {
        if (autoRefreshTimer != null) autoRefreshTimer.stop();
        autoRefreshTimer = new Timer(10000, e -> reloadTimelineData());
        autoRefreshTimer.start();
    }

    public void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
            autoRefreshTimer = null;
        }
    }
}
