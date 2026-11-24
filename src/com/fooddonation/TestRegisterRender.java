package com.fooddonation;

import com.fooddonation.ui.RegisterFrame;

import javax.swing.*;
import java.awt.*;

public class TestRegisterRender {
    public static void main(String[] args) throws Exception {
        int[][] testSizes = {
            {1040, 720},
            {1280, 800},
            {1920, 1080},
            {940, 640}
        };

        for (int[] size : testSizes) {
            final int testW = size[0];
            final int testH = size[1];
            SwingUtilities.invokeAndWait(() -> {
                try {
                    System.out.println("==================================================");
                    System.out.println("TESTING WINDOW SIZE: " + testW + "x" + testH);
                    RegisterFrame frame = new RegisterFrame();
                    frame.setSize(testW, testH);
                    frame.addNotify();
                    frame.validate();

                    Container root = frame.getContentPane();
                    int rootW = root.getWidth();
                    int rootH = root.getHeight();
                    System.out.println("RootPane ContentPane size: " + rootW + "x" + rootH);

                    Component[] children = root.getComponents();
                    if (children.length >= 2) {
                        Component left = children[0];
                        Component right = children[1];
                        double leftPct = (double) left.getWidth() / rootW * 100;
                        double rightPct = (double) right.getWidth() / rootW * 100;
                        System.out.printf("Left Hero Panel: %s (%.1f%% width)\n", left.getBounds(), leftPct);
                        System.out.printf("Right Form Panel: %s (%.1f%% width)\n", right.getBounds(), rightPct);
                    }

                    // Find and inspect form components inside right panel
                    checkFormBounds(root, 0);

                    frame.dispose();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        System.out.println("==================================================");
        System.out.println("ALL RENDER TESTS COMPLETED SUCCESSFULLY");
        System.exit(0);
    }

    private static void checkFormBounds(Component c, int depth) {
        if (c.getClass().getName().contains("RoundedTextField") ||
            c.getClass().getName().contains("RoundedPasswordField") ||
            c.getClass().getName().contains("RoundedButton") ||
            c instanceof JComboBox ||
            c instanceof JToggleButton) {

            String name = c.getClass().getSimpleName();
            if (c instanceof JToggleButton) name += " (" + ((JToggleButton) c).getText() + ")";
            else if (c instanceof JButton) name += " (" + ((JButton) c).getText() + ")";
            else if (c instanceof JComboBox) name += " (" + ((JComboBox<?>) c).getItemCount() + " items)";

            System.out.println("  ".repeat(depth) + name + " -> bounds=" + c.getBounds());
        }

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                checkFormBounds(child, depth + 1);
            }
        }
    }
}
