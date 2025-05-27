package simulation.software.codebase;

import javax.swing.*;
import java.awt.*;

public class WaveformPanel extends JPanel {
    private SweepGenerator generator;
    private static final int POINTS = 1000;
    private static final double DISPLAY_WINDOW = 0.1; // 100ms window for waveform

    public WaveformPanel(SweepGenerator generator) {
        this.generator = generator;
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(500, 400));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw grid
        g2d.setColor(Color.DARK_GRAY);
        for (int i = 0; i < getHeight(); i += 50) {
            g2d.drawLine(0, i, getWidth(), i);
        }
        for (int i = 0; i < getWidth(); i += 50) {
            g2d.drawLine(i, 0, i, getHeight());
        }

        // Draw waveform
        g2d.setColor(Color.GREEN);
        if (generator.isRunning()) {
            int width = getWidth();
            int height = getHeight();
            int midY = height / 2;

            for (int i = 0; i < POINTS - 1; i++) {
                double t1 = (i / (double) POINTS) * DISPLAY_WINDOW;
                double t2 = ((i + 1) / (double) POINTS) * DISPLAY_WINDOW;
                double value1 = generator.calculateWaveform(t1);
                double value2 = generator.calculateWaveform(t2);
                int x1 = i * width / POINTS;
                int x2 = (i + 1) * width / POINTS;
                int y1 = (int) (midY - value1 * midY * 0.5);
                int y2 = (int) (midY - value2 * midY * 0.5);
                g2d.drawLine(x1, y1, x2, y2);
            }

            // Draw current value
            g2d.setFont(new Font("Bahnschrift", Font.BOLD, 14));
            g2d.setColor(Color.WHITE);
            if (generator.isTimeSweep()) {
                g2d.drawString(String.format("Current Amplitude: %.2f", generator.getCurrentValue()), 10, 30);
            } else {
                g2d.drawString(String.format("Current Freq: %.2f Hz", generator.getCurrentValue()), 10, 30);
            }
        }
    }
}