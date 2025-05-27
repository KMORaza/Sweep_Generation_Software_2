package simulation.software.codebase;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CrossCoupling extends JFrame {
    private SweepGenerator generator;
    private CrossCouplingPanel couplingPanel;
    private JLabel crosstalkLabel;
    private JLabel fundamentalLabel;
    private JSlider windowSizeSlider;
    private JSlider couplingFactorSlider;
    private JButton exportCrosstalkButton;
    private static final int SAMPLE_RATE = 44100;

    public CrossCoupling(SweepGenerator generator) {
        this.generator = generator;
        setTitle("Cross-Coupling Analysis");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Set Bahnschrift font
        Font bahnschrift = new Font("Bahnschrift", Font.PLAIN, 12);
        UIManager.put("Label.font", bahnschrift);
        UIManager.put("Button.font", bahnschrift);
        UIManager.put("Slider.font", bahnschrift);

        couplingPanel = new CrossCouplingPanel(generator, () -> windowSizeSlider.getValue() / 1000.0, 
                                              () -> couplingFactorSlider.getValue() / 1000.0);
        add(couplingPanel, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Crosstalk Label
        crosstalkLabel = new JLabel("Crosstalk: N/A");
        crosstalkLabel.setFont(bahnschrift);
        gbc.gridwidth = 2;
        controlPanel.add(crosstalkLabel, gbc);
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
        windowSizeSlider.addChangeListener(e -> couplingPanel.repaint());
        controlPanel.add(windowSizeSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Coupling Factor Slider
        JLabel couplingFactorLabel = new JLabel("Coupling Factor (%):");
        controlPanel.add(couplingFactorLabel, gbc);
        gbc.gridx = 1;
        couplingFactorSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50); // 0 to 10%, step 0.01
        couplingFactorSlider.setMajorTickSpacing(25);
        couplingFactorSlider.setMinorTickSpacing(5);
        couplingFactorSlider.setPaintTicks(true);
        couplingFactorSlider.setPaintLabels(true);
        couplingFactorSlider.addChangeListener(e -> couplingPanel.repaint());
        controlPanel.add(couplingFactorSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Export Button
        exportCrosstalkButton = new JButton("Export Crosstalk CSV");
        gbc.gridwidth = 2;
        controlPanel.add(exportCrosstalkButton, gbc);

        add(controlPanel, BorderLayout.SOUTH);

        exportCrosstalkButton.addActionListener(e -> {
            if (!generator.isRunning()) {
                JOptionPane.showMessageDialog(this, "Start the sweep to export data.");
                return;
            }
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Crosstalk CSV");
            fileChooser.setSelectedFile(new File("crosstalk_data.csv"));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".csv")) {
                    file = new File(file.getPath() + ".csv");
                }
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(couplingPanel.getCrosstalkCSV());
                    JOptionPane.showMessageDialog(this, "Crosstalk CSV saved to " + file.getPath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving CSV: " + ex.getMessage());
                }
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (couplingPanel != null) {
            couplingPanel.repaint();
        }
    }

    private class CrossCouplingPanel extends JPanel {
        private SweepGenerator generator;
        private java.util.function.Supplier<Double> windowSizeSupplier;
        private java.util.function.Supplier<Double> couplingFactorSupplier;
        private List<Double> crosstalkHistory;
        private List<Double> timeHistory;
        private double fundamentalFreq;
        private double crosstalkDB;
        private static final int MAX_POINTS = 1000;
        private static final double PHASE_SHIFT_DEG = 45.0; // Fixed phase shift for right channel

        public CrossCouplingPanel(SweepGenerator generator, 
                                 java.util.function.Supplier<Double> windowSizeSupplier,
                                 java.util.function.Supplier<Double> couplingFactorSupplier) {
            this.generator = generator;
            this.windowSizeSupplier = windowSizeSupplier;
            this.couplingFactorSupplier = couplingFactorSupplier;
            this.crosstalkHistory = new ArrayList<>();
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
                crosstalkLabel.setText("Crosstalk: N/A");
                fundamentalLabel.setText("Fundamental: N/A");
                crosstalkHistory.clear();
                timeHistory.clear();
                return;
            }

            // Get waveform samples
            double windowSize = windowSizeSupplier.get();
            int sampleCount = (int) (windowSize * SAMPLE_RATE);
            double[] leftOriginal = generator.getWaveformSamples(sampleCount, SAMPLE_RATE, windowSize);
            double phaseShiftRad = Math.toRadians(PHASE_SHIFT_DEG);
            double[] rightOriginal = new double[sampleCount];
            for (int i = 0; i < sampleCount; i++) {
                double t = i / (double) SAMPLE_RATE;
                rightOriginal[i] = generator.calculateWaveform(
                    t + phaseShiftRad / (2 * Math.PI * generator.getCurrentValue()));
            }

            // Apply cross-coupling
            double couplingFactor = couplingFactorSupplier.get();
            double[] leftChannel = new double[sampleCount];
            double[] rightChannel = new double[sampleCount];
            for (int i = 0; i < sampleCount; i++) {
                leftChannel[i] = leftOriginal[i] + couplingFactor * rightOriginal[i];
                rightChannel[i] = rightOriginal[i] + couplingFactor * leftOriginal[i];
            }

            // Compute FFT
            FFTCalculator fft = new FFTCalculator();
            FFTCalculator.Complex[] leftFFT = fft.computeFFT(leftChannel);
            FFTCalculator.Complex[] rightFFT = fft.computeFFT(rightChannel);

            // Find fundamental frequency
            int fundamentalIndex = 0;
            double maxMagnitude = 0;
            for (int i = 1; i < leftFFT.length / 2; i++) {
                double mag = Math.sqrt(leftFFT[i].real * leftFFT[i].real + 
                                      leftFFT[i].imag * leftFFT[i].imag);
                if (mag > maxMagnitude) {
                    maxMagnitude = mag;
                    fundamentalIndex = i;
                }
            }
            double freqStep = SAMPLE_RATE / (double) leftFFT.length;
            fundamentalFreq = fundamentalIndex * freqStep;

            // Compute crosstalk (left channel leaking into right channel)
            double leftMag = Math.sqrt(leftFFT[fundamentalIndex].real * leftFFT[fundamentalIndex].real +
                                      leftFFT[fundamentalIndex].imag * leftFFT[fundamentalIndex].imag);
            double rightLeakMag = Math.sqrt(rightFFT[fundamentalIndex].real * rightFFT[fundamentalIndex].real +
                                           rightFFT[fundamentalIndex].imag * rightFFT[fundamentalIndex].imag);
            crosstalkDB = leftMag > 0 ? 20 * Math.log10(rightLeakMag / leftMag) : -Double.MAX_VALUE;

            // Update labels
            crosstalkLabel.setText(String.format("Crosstalk: %.1f dB", crosstalkDB));
            fundamentalLabel.setText(String.format("Fundamental: %.1f Hz", fundamentalFreq));

            // Store history
            double currentTime = System.currentTimeMillis() / 1000.0;
            crosstalkHistory.add(crosstalkDB);
            timeHistory.add(currentTime);
            if (crosstalkHistory.size() > MAX_POINTS) {
                crosstalkHistory.remove(0);
                timeHistory.remove(0);
            }

            // Draw crosstalk trace
            g2d.setColor(Color.CYAN);
            double minTime = timeHistory.isEmpty() ? 0 : timeHistory.get(0);
            double maxTime = timeHistory.isEmpty() ? 1 : timeHistory.get(timeHistory.size() - 1);
            double timeRange = maxTime - minTime > 0 ? maxTime - minTime : 1;
            for (int i = 1; i < crosstalkHistory.size(); i++) {
                int x1 = (int) ((timeHistory.get(i - 1) - minTime) / timeRange * width);
                int x2 = (int) ((timeHistory.get(i) - minTime) / timeRange * width);
                int y1 = (int) (midY - (crosstalkHistory.get(i - 1) / -100.0) * (height / 2 - 50));
                int y2 = (int) (midY - (crosstalkHistory.get(i) / -100.0) * (height / 2 - 50));
                g2d.drawLine(x1, y1, x2, y2);
            }

            // Draw axes labels
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Bahnschrift", Font.PLAIN, 12));
            g2d.drawString("Time (s)", width / 2 - 30, height - 10);
            g2d.rotate(-Math.PI / 2);
            g2d.drawString("Crosstalk (dB)", -height / 2 - 30, 20);
            g2d.rotate(Math.PI / 2);
        }

        public String getCrosstalkCSV() {
            StringBuilder csv = new StringBuilder("Time (s),Fundamental (Hz),Crosstalk (dB)\n");
            if (crosstalkHistory.isEmpty()) return csv.toString();
            for (int i = 0; i < crosstalkHistory.size(); i++) {
                csv.append(String.format("%.3f,%.1f,%.1f\n", 
                                        timeHistory.get(i), fundamentalFreq, crosstalkHistory.get(i)));
            }
            return csv.toString();
        }
    }
}