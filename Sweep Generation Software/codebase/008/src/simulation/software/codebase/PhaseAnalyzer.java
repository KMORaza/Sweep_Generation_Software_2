package simulation.software.codebase;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhaseAnalyzer extends JFrame {
    private SweepGenerator generator;
    private PhasePanel phasePanel;
    private JLabel phaseLabel;
    private JLabel fundamentalLabel;
    private JSlider windowSizeSlider;
    private JSlider phaseShiftSlider;
    private JButton exportPhaseButton;
    private static final int SAMPLE_RATE = 44100;

    public PhaseAnalyzer(SweepGenerator generator) {
        this.generator = generator;
        setTitle("Phase Analysis");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Set Bahnschrift font
        Font bahnschrift = new Font("Bahnschrift", Font.PLAIN, 12);
        UIManager.put("Label.font", bahnschrift);
        UIManager.put("Button.font", bahnschrift);
        UIManager.put("Slider.font", bahnschrift);

        phasePanel = new PhasePanel(generator, () -> windowSizeSlider.getValue() / 1000.0, () -> phaseShiftSlider.getValue());
        add(phasePanel, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Phase Difference Label
        phaseLabel = new JLabel("Phase Difference: N/A");
        phaseLabel.setFont(bahnschrift);
        gbc.gridwidth = 2;
        controlPanel.add(phaseLabel, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;

        // Fundamental Frequency Label
        fundamentalLabel = new JLabel("Fundamental: N/A");
        fundamentalLabel.setFont(bahnschrift);
        gbc.gridwidth = 2;
        controlPanel.add(fundamentalLabel, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;

        // Window Size Slider
        JLabel windowSizeLabel = new JLabel("Window Size (ms):");
        controlPanel.add(windowSizeLabel, gbc);
        gbc.gridx = 1;
        windowSizeSlider = new JSlider(JSlider.HORIZONTAL, 5, 50, 20);
        windowSizeSlider.setMajorTickSpacing(15);
        windowSizeSlider.setMinorTickSpacing(5);
        windowSizeSlider.setPaintTicks(true);
        windowSizeSlider.setPaintLabels(true);
        windowSizeSlider.addChangeListener(e -> phasePanel.repaint());
        controlPanel.add(windowSizeSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Phase Shift Slider
        JLabel phaseShiftLabel = new JLabel("Right Channel Phase Shift (deg):");
        controlPanel.add(phaseShiftLabel, gbc);
        gbc.gridx = 1;
        phaseShiftSlider = new JSlider(JSlider.HORIZONTAL, 0, 360, 45);
        phaseShiftSlider.setMajorTickSpacing(90);
        phaseShiftSlider.setMinorTickSpacing(10);
        phaseShiftSlider.setPaintTicks(true);
        phaseShiftSlider.setPaintLabels(true);
        phaseShiftSlider.addChangeListener(e -> phasePanel.repaint());
        controlPanel.add(phaseShiftSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Export Button
        exportPhaseButton = new JButton("Export Phase CSV");
        gbc.gridwidth = 2;
        controlPanel.add(exportPhaseButton, gbc);

        add(controlPanel, BorderLayout.SOUTH);

        exportPhaseButton.addActionListener(e -> {
            if (!generator.isRunning()) {
                JOptionPane.showMessageDialog(this, "Start the sweep to export data.");
                return;
            }
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Phase CSV");
            fileChooser.setSelectedFile(new File("phase_data.csv"));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".csv")) {
                    file = new File(file.getPath() + ".csv");
                }
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(phasePanel.getPhaseCSV());
                    JOptionPane.showMessageDialog(this, "Phase CSV saved to " + file.getPath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving CSV: " + ex.getMessage());
                }
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (phasePanel != null) {
            phasePanel.repaint();
        }
    }

    private class PhasePanel extends JPanel {
        private SweepGenerator generator;
        private java.util.function.Supplier<Double> windowSizeSupplier;
        private java.util.function.Supplier<Integer> phaseShiftSupplier;
        private List<Double> phaseHistory;
        private List<Double> timeHistory;
        private double fundamentalFreq;
        private double phaseDifference;
        private static final int MAX_POINTS = 1000;

        public PhasePanel(SweepGenerator generator, java.util.function.Supplier<Double> windowSizeSupplier,
                          java.util.function.Supplier<Integer> phaseShiftSupplier) {
            this.generator = generator;
            this.windowSizeSupplier = windowSizeSupplier;
            this.phaseShiftSupplier = phaseShiftSupplier;
            this.phaseHistory = new ArrayList<>();
            this.timeHistory = new ArrayList<>();
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(600, 400));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int midY = height / 2;

            // Draw grid
            g2d.setColor(Color.DARK_GRAY);
            for (int i = 0; i < height; i += 50) {
                g2d.drawLine(0, i, width, i);
            }
            for (int i = 0; i < width; i += 50) {
                g2d.drawLine(i, 0, i, height);
            }

            if (!generator.isRunning()) {
                phaseLabel.setText("Phase Difference: N/A");
                fundamentalLabel.setText("Fundamental: N/A");
                phaseHistory.clear();
                timeHistory.clear();
                return;
            }

            // Get waveform samples
            double windowSize = windowSizeSupplier.get();
            int sampleCount = (int) (windowSize * SAMPLE_RATE);
            double[] leftChannel = generator.getWaveformSamples(sampleCount, SAMPLE_RATE, windowSize);
            double phaseShiftDeg = phaseShiftSupplier.get();
            double phaseShiftRad = Math.toRadians(phaseShiftDeg);
            double[] rightChannel = new double[sampleCount];
            for (int i = 0; i < sampleCount; i++) {
                double t = i / (double) SAMPLE_RATE;
                rightChannel[i] = generator.calculateWaveform(t + phaseShiftRad / (2 * Math.PI * generator.getCurrentValue()));
            }

            // Compute FFT
            FFTCalculator fft = new FFTCalculator();
            FFTCalculator.Complex[] leftFFT = fft.computeFFT(leftChannel);
            FFTCalculator.Complex[] rightFFT = fft.computeFFT(rightChannel);

            // Find fundamental frequency
            int fundamentalIndex = 0;
            double maxMagnitude = 0;
            for (int i = 1; i < leftFFT.length / 2; i++) {
                double mag = Math.sqrt(leftFFT[i].real * leftFFT[i].real + leftFFT[i].imag * leftFFT[i].imag);
                if (mag > maxMagnitude) {
                    maxMagnitude = mag;
                    fundamentalIndex = i;
                }
            }
            double freqStep = SAMPLE_RATE / (double) leftFFT.length;
            fundamentalFreq = fundamentalIndex * freqStep;

            // Compute phase difference
            double leftPhase = Math.atan2(leftFFT[fundamentalIndex].imag, leftFFT[fundamentalIndex].real);
            double rightPhase = Math.atan2(rightFFT[fundamentalIndex].imag, rightFFT[fundamentalIndex].real);
            phaseDifference = Math.toDegrees(rightPhase - leftPhase);
            // Normalize to [-180, 180]
            while (phaseDifference > 180) phaseDifference -= 360;
            while (phaseDifference < -180) phaseDifference += 360;

            // Update labels
            phaseLabel.setText(String.format("Phase Difference: %.1f°", phaseDifference));
            fundamentalLabel.setText(String.format("Fundamental: %.1f Hz", fundamentalFreq));

            // Store history
            double currentTime = System.currentTimeMillis() / 1000.0;
            phaseHistory.add(phaseDifference);
            timeHistory.add(currentTime);
            if (phaseHistory.size() > MAX_POINTS) {
                phaseHistory.remove(0);
                timeHistory.remove(0);
            }

            // Draw phase trace
            g2d.setColor(Color.CYAN);
            double minTime = timeHistory.isEmpty() ? 0 : timeHistory.get(0);
            double maxTime = timeHistory.isEmpty() ? 1 : timeHistory.get(timeHistory.size() - 1);
            double timeRange = maxTime - minTime > 0 ? maxTime - minTime : 1;
            for (int i = 1; i < phaseHistory.size(); i++) {
                int x1 = (int) ((timeHistory.get(i - 1) - minTime) / timeRange * width);
                int x2 = (int) ((timeHistory.get(i) - minTime) / timeRange * width);
                int y1 = (int) (midY - phaseHistory.get(i - 1) / 180.0 * (height / 2 - 50));
                int y2 = (int) (midY - phaseHistory.get(i) / 180.0 * (height / 2 - 50));
                g2d.drawLine(x1, y1, x2, y2);
            }

            // Draw axes labels
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Bahnschrift", Font.PLAIN, 12));
            g2d.drawString("Time (s)", width / 2 - 30, height - 10);
            g2d.rotate(-Math.PI / 2);
            g2d.drawString("Phase Difference (°)", -height / 2 - 30, 20);
            g2d.rotate(Math.PI / 2);
        }

        public String getPhaseCSV() {
            StringBuilder csv = new StringBuilder("Time (s),Fundamental (Hz),Phase Difference (deg)\n");
            if (phaseHistory.isEmpty()) return csv.toString();
            for (int i = 0; i < phaseHistory.size(); i++) {
                csv.append(String.format("%.3f,%.1f,%.1f\n", timeHistory.get(i), fundamentalFreq, phaseHistory.get(i)));
            }
            return csv.toString();
        }
    }
}