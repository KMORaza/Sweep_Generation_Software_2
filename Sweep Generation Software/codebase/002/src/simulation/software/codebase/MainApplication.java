package simulation.software.codebase;

import javax.swing.*;

public class MainApplication {
    public static void main(String[] args) {
        // Set Windows Classic Look and Feel
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create and show the UI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            SweepGeneratorUI ui = new SweepGeneratorUI();
            ui.setVisible(true);
        });
    }
}