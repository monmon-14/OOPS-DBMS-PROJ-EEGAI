package com.fooddonation;

import com.fooddonation.ui.MotionVideoIntroDialog;
import com.fooddonation.ui.RoleSelectionFrame;
import com.fooddonation.util.DBConnection;

import javax.swing.*;

/**
 * Application Entry Point for the Food Donation & Rescue Network.
 * Launches the modern Role Selection portal on the Swing Event Dispatch Thread (EDT).
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("========================================================");
        System.out.println("  ஈகை — பகுத்துண்டு பல்லுயிர் ஓம்புதல்");
        System.out.println("  Community Food Donation & Rescue Network");
        System.out.println("========================================================");

        // Configure Modern Look & Feel and Antialiasing
        try {
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Notice: Could not set system look and feel: " + e.getMessage());
        }

        // Test database connectivity and log status
        new Thread(() -> {
            boolean connected = DBConnection.testConnection();
            if (connected) {
                System.out.println("[DB] Successfully connected to MySQL database: " + DBConnection.getUrl());
            } else {
                System.out.println("[DB] MySQL not connected with default credentials. Running in Seamless Local In-Memory Fallback Mode.");
                System.out.println("[DB] To connect your MySQL server, update credentials in 'db.properties'.");
            }
        }).start();

        // Launch Motion Video Intro in front of the existing app
        SwingUtilities.invokeLater(() -> {
            RoleSelectionFrame frame = new RoleSelectionFrame();
            MotionVideoIntroDialog intro = new MotionVideoIntroDialog(null, () -> {
                frame.setVisible(true);
            });
            intro.playIntro();
        });
    }
}
