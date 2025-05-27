package simulation.software.codebase;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AutomatischeOptimierung extends JFrame {
    private SweepGenerator generator;
    private OptimizationPanel optimizationPanel;
    private JLabel statusLabel;
    private JTextField targetResponseField;
    private JSlider startFreqMinSlider, startFreqMaxSlider, endFreqMinSlider, endFreqMaxSlider, sweepTimeMinSlider, sweepTimeMaxSlider;
    private JButton startOptimizationButton, exportButton;
    private static final int SAMPLE_RATE = 44100;
    private static final int MAX_ITERATIONS = 100;
    private static final double MSE_THRESHOLD = 0.1; // dB²

    public AutomatischeOptimierung(SweepGenerator generator) {
        this.generator = generator;
        setTitle("Auto-Optimization");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Set Bahnschrift font
        Font bahnschrift = new Font("Bahnschrift", Font.PLAIN, 12);
        UIManager.put("Label.font", bahnschrift);
        UIManager.put("Button.font", bahnschrift);
        UIManager.put("TextField.font", bahnschrift);
        UIManager.put("Slider.font", bahnschrift);

        optimizationPanel = new OptimizationPanel(generator);
        add(optimizationPanel, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Status Label
        statusLabel = new JLabel("Status: Idle");
        statusLabel.setFont(bahnschrift);
        gbc.gridwidth = 2;
        controlPanel.add(statusLabel, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;

        // Target Response
        JLabel targetLabel = new JLabel("Target Response (freq,dB;...):");
        controlPanel.add(targetLabel, gbc);
        gbc.gridx = 1;
        targetResponseField = new JTextField("100,0;500,0;1000,0", 20);
        controlPanel.add(targetResponseField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Start Frequency Min Slider
        JLabel startFreqMinLabel = new JLabel("Start Freq Min (Hz):");
        controlPanel.add(startFreqMinLabel, gbc);
        gbc.gridx = 1;
        startFreqMinSlider = new JSlider(JSlider.HORIZONTAL, 20, 1000, 50);
        startFreqMinSlider.setMajorTickSpacing(250);
        startFreqMinSlider.setPaintTicks(true);
        startFreqMinSlider.setPaintLabels(true);
        controlPanel.add(startFreqMinSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Start Frequency Max Slider
        JLabel startFreqMaxLabel = new JLabel("Start Freq Max (Hz):");
        controlPanel.add(startFreqMaxLabel, gbc);
        gbc.gridx = 1;
        startFreqMaxSlider = new JSlider(JSlider.HORIZONTAL, 50, 2000, 500);
        startFreqMaxSlider.setMajorTickSpacing(500);
        startFreqMaxSlider.setPaintTicks(true);
        startFreqMaxSlider.setPaintLabels(true);
        controlPanel.add(startFreqMaxSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // End Frequency Min Slider
        JLabel endFreqMinLabel = new JLabel("End Freq Min (Hz):");
        controlPanel.add(endFreqMinLabel, gbc);
        gbc.gridx = 1;
        endFreqMinSlider = new JSlider(JSlider.HORIZONTAL, 500, 5000, 1000);
        endFreqMinSlider.setMajorTickSpacing(1000);
        endFreqMinSlider.setPaintTicks(true);
        endFreqMinSlider.setPaintLabels(true);
        controlPanel.add(endFreqMinSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // End Frequency Max Slider
        JLabel endFreqMaxLabel = new JLabel("End Freq Max (Hz):");
        controlPanel.add(endFreqMaxLabel, gbc);
        gbc.gridx = 1;
        endFreqMaxSlider = new JSlider(JSlider.HORIZONTAL, 1000, 20000, 5000);
        endFreqMaxSlider.setMajorTickSpacing(5000);
        endFreqMaxSlider.setPaintTicks(true);
        endFreqMaxSlider.setPaintLabels(true);
        controlPanel.add(endFreqMaxSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Sweep Time Min Slider
        JLabel sweepTimeMinLabel = new JLabel("Sweep Time Min (s):");
        controlPanel.add(sweepTimeMinLabel, gbc);
        gbc.gridx = 1;
        sweepTimeMinSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, 5);
        sweepTimeMinSlider.setMajorTickSpacing(5);
        sweepTimeMinSlider.setPaintTicks(true);
        sweepTimeMinSlider.setPaintLabels(true);
        controlPanel.add(sweepTimeMinSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Sweep Time Max Slider
        JLabel sweepTimeMaxLabel = new JLabel("Sweep Time Max (s):");
        controlPanel.add(sweepTimeMaxLabel, gbc);
        gbc.gridx = 1;
        sweepTimeMaxSlider = new JSlider(JSlider.HORIZONTAL, 5, 60, 30);
        sweepTimeMaxSlider.setMajorTickSpacing(15);
        sweepTimeMaxSlider.setPaintTicks(true);
        sweepTimeMaxSlider.setPaintLabels(true);
        controlPanel.add(sweepTimeMaxSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Start/Stop Optimization Button
        startOptimizationButton = new JButton("Start Optimization");
        gbc.gridwidth = 2;
        controlPanel.add(startOptimizationButton, gbc);
        gbc.gridy++;

        // Export Button
        exportButton = new JButton("Export Optimization CSV");
        gbc.gridwidth = 2;
        controlPanel.add(exportButton, gbc);

        add(controlPanel, BorderLayout.SOUTH);

        startOptimizationButton.addActionListener(e -> {
            if (optimizationPanel.isOptimizing()) {
                optimizationPanel.stopOptimization();
                startOptimizationButton.setText("Start Optimization");
                statusLabel.setText("Status: Idle");
            } else {
                try {
                    optimizationPanel.startOptimization(
                        parseTargetResponse(targetResponseField.getText()),
                        startFreqMinSlider.getValue(), startFreqMaxSlider.getValue(),
                        endFreqMinSlider.getValue(), endFreqMaxSlider.getValue(),
                        sweepTimeMinSlider.getValue(), sweepTimeMaxSlider.getValue()
                    );
                    startOptimizationButton.setText("Stop Optimization");
                    statusLabel.setText("Status: Optimizing...");
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid target response: " + ex.getMessage());
                }
            }
        });

        exportButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Optimization CSV");
            fileChooser.setSelectedFile(new File("optimization_data.csv"));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".csv")) {
                    file = new File(file.getPath() + ".csv");
                }
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(optimizationPanel.getOptimizationCSV());
                    JOptionPane.showMessageDialog(this, "Optimization CSV saved to " + file.getPath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving CSV: " + ex.getMessage());
                }
            }
        });
    }

    private double[][] parseTargetResponse(String input) {
        try {
            String[] pairs = input.split(";");
            double[][] response = new double[pairs.length][2];
            for (int i = 0; i < pairs.length; i++) {
                String[] parts = pairs[i].split(",");
                response[i][0] = Double.parseDouble(parts[0].trim()); // Frequency
                response[i][1] = Double.parseDouble(parts[1].trim()); // dB
            }
            return response;
        } catch (Exception e) {
            throw new IllegalArgumentException("Format: freq,dB;freq,dB (e.g., 100,0;500,0)");
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (optimizationPanel != null) {
            optimizationPanel.repaint();
        }
    }

    private class OptimizationPanel extends JPanel {
        private SweepGenerator generator;
        private List<Double> errorHistory;
        private List<Double> timeHistory;
        private double[][] targetResponse;
        private double startFreq, endFreq, sweepTime;
        private double mse;
        private boolean optimizing;
        private Thread optimizationThread;
        private static final int MAX_POINTS = 1000;
        private static final double WINDOW_SIZE = 0.02; // 20ms

        public OptimizationPanel(SweepGenerator generator) {
            this.generator = generator;
            this.errorHistory = new ArrayList<>();
            this.timeHistory = new ArrayList<>();
            this.startFreq = 100;
            this.endFreq = 1000;
            this.sweepTime = 10;
            this.mse = Double.MAX_VALUE;
            this.optimizing = false;
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(600, 400));
        }

        public void startOptimization(double[][] targetResponse, int startFreqMin, int startFreqMax,
                                     int endFreqMin, int endFreqMax, int sweepTimeMin, int sweepTimeMax) {
            if (optimizing) return;
            this.targetResponse = targetResponse;
            this.optimizing = true;
            errorHistory.clear();
            timeHistory.clear();

            optimizationThread = new Thread(() -> {
                Random rand = new Random();
                double bestMSE = Double.MAX_VALUE;
                double bestStartFreq = startFreq;
                double bestEndFreq = endFreq;
                double bestSweepTime = sweepTime;

                for (int i = 0; i < MAX_ITERATIONS && optimizing; i++) {
                    // Randomly sample parameters within bounds
                    double trialStartFreq = startFreqMin + rand.nextDouble() * (startFreqMax - startFreqMin);
                    double trialEndFreq = Math.max(trialStartFreq + 50, endFreqMin + rand.nextDouble() * (endFreqMax - endFreqMin));
                    double trialSweepTime = sweepTimeMin + rand.nextDouble() * (sweepTimeMax - sweepTimeMin);

                    // Simulate sweep
                    generator.startSweep(trialStartFreq, trialEndFreq, trialSweepTime, 500, 15, 
                                        0.8, 0, 10, 0.5, "Linear", "Sine", "None", "None");
                    try {
                        Thread.sleep(100); // Allow sweep to stabilize
                    } catch (InterruptedException e) {
                        break;
                    }

                    // Compute MSE
                    mse = computeMSE();
                    if (mse < bestMSE) {
                        bestMSE = mse;
                        bestStartFreq = trialStartFreq;
                        bestEndFreq = trialEndFreq;
                        bestSweepTime = trialSweepTime;
                        startFreq = bestStartFreq;
                        endFreq = bestEndFreq;
                        sweepTime = bestSweepTime;
                    }

                    // Update history
                    double currentTime = System.currentTimeMillis() / 1000.0;
                    errorHistory.add(mse);
                    timeHistory.add(currentTime);
                    if (errorHistory.size() > MAX_POINTS) {
                        errorHistory.remove(0);
                        timeHistory.remove(0);
                    }

                    repaint();
                    if (mse < MSE_THRESHOLD) break;
                }

                // Apply best parameters
                generator.startSweep(bestStartFreq, bestEndFreq, bestSweepTime, 500, 15, 
                                    0.8, 0, 10, 0.5, "Linear", "Sine", "None", "None");
                optimizing = false;
                final double finalBestMSE = bestMSE; // Copy to final variable
                SwingUtilities.invokeLater(() -> {
                    startOptimizationButton.setText("Start Optimization");
                    statusLabel.setText(String.format("Status: Done (MSE: %.2f dB²)", finalBestMSE));
                });
            });
            optimizationThread.start();
        }

        public void stopOptimization() {
            optimizing = false;
            if (optimizationThread != null) {
                optimizationThread.interrupt();
            }
        }

        public boolean isOptimizing() {
            return optimizing;
        }

        private double computeMSE() {
            int sampleCount = (int) (WINDOW_SIZE * SAMPLE_RATE);
            double[] samples = generator.getWaveformSamples(sampleCount, SAMPLE_RATE, WINDOW_SIZE);
            FFTCalculator fft = new FFTCalculator();
            double[] magnitude = fft.computeFFTMagnitude(samples);
            double[] freqs = new double[magnitude.length];
            double freqStep = SAMPLE_RATE / (double) (magnitude.length * 2);
            for (int i = 0; i < freqs.length; i++) {
                freqs[i] = i * freqStep;
            }

            // Interpolate target response
            double[] targetDB = new double[magnitude.length];
            for (int i = 0; i < magnitude.length; i++) {
                double freq = freqs[i];
                targetDB[i] = interpolateTarget(freq);
            }

            // Compute MSE
            double sumSquaredError = 0;
            for (int i = 0; i < magnitude.length; i++) {
                double generatedDB = 20 * Math.log10(magnitude[i] + 1e-10);
                double error = generatedDB - targetDB[i];
                sumSquaredError += error * error;
            }
            return sumSquaredError / magnitude.length;
        }

        private double interpolateTarget(double freq) {
            if (targetResponse.length < 2) return targetResponse[0][1];
            for (int i = 0; i < targetResponse.length - 1; i++) {
                double f1 = targetResponse[i][0], dB1 = targetResponse[i][1];
                double f2 = targetResponse[i + 1][0], dB2 = targetResponse[i + 1][1];
                if (freq >= f1 && freq <= f2) {
                    return dB1 + (dB2 - dB1) * (freq - f1) / (f2 - f1);
                }
            }
            return targetResponse[targetResponse.length - 1][1];
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

            // Plot target and current response
            if (targetResponse != null) {
                int sampleCount = (int) (WINDOW_SIZE * SAMPLE_RATE);
                double[] samples = generator.getWaveformSamples(sampleCount, SAMPLE_RATE, WINDOW_SIZE);
                FFTCalculator fft = new FFTCalculator();
                double[] magnitude = fft.computeFFTMagnitude(samples);
                double[] freqs = new double[magnitude.length];
                double freqStep = SAMPLE_RATE / (double) (magnitude.length * 2);
                for (int i = 0; i < freqs.length; i++) {
                    freqs[i] = i * freqStep;
                }

                // Target response (white)
                g2d.setColor(Color.WHITE);
                for (int i = 1; i < freqs.length; i++) {
                    int x1 = (int) (freqs[i - 1] / 20000.0 * width);
                    int x2 = (int) (freqs[i] / 20000.0 * width);
                    int y1 = (int) (midY - interpolateTarget(freqs[i - 1]) / 20.0 * (height / 2 - 50));
                    int y2 = (int) (midY - interpolateTarget(freqs[i]) / 20.0 * (height / 2 - 50));
                    g2d.drawLine(x1, y1, x2, y2);
                }

                // Current response (cyan)
                g2d.setColor(Color.CYAN);
                for (int i = 1; i < magnitude.length; i++) {
                    int x1 = (int) (freqs[i - 1] / 20000.0 * width);
                    int x2 = (int) (freqs[i] / 20000.0 * width);
                    double dB1 = 20 * Math.log10(magnitude[i - 1] + 1e-10);
                    double dB2 = 20 * Math.log10(magnitude[i] + 1e-10);
                    int y1 = (int) (midY - dB1 / 20.0 * (height / 2 - 50));
                    int y2 = (int) (midY - dB2 / 20.0 * (height / 2 - 50));
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }

            // Plot error history (yellow)
            g2d.setColor(Color.YELLOW);
            double minTime = timeHistory.isEmpty() ? 0 : timeHistory.get(0);
            double maxTime = timeHistory.isEmpty() ? 1 : timeHistory.get(timeHistory.size() - 1);
            double timeRange = maxTime - minTime > 0 ? maxTime - minTime : 1;
            for (int i = 1; i < errorHistory.size(); i++) {
                int x1 = (int) ((timeHistory.get(i - 1) - minTime) / timeRange * width);
                int x2 = (int) ((timeHistory.get(i) - minTime) / timeRange * width);
                int y1 = (int) (midY - errorHistory.get(i - 1) / 10.0 * (height / 2 - 50));
                int y2 = (int) (midY - errorHistory.get(i) / 10.0 * (height / 2 - 50));
                g2d.drawLine(x1, y1, x2, y2);
            }

            // Draw axes labels
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Bahnschrift", Font.PLAIN, 12));
            g2d.drawString("Frequency (Hz)", width / 2 - 30, height - 10);
            g2d.rotate(-Math.PI / 2);
            g2d.drawString("Amplitude (dB) / MSE (dB²)", -height / 2 - 30, 20);
            g2d.rotate(Math.PI / 2);

            // Draw current parameters
            g2d.drawString(String.format("Start: %.0f Hz, End: %.0f Hz, Time: %.1f s, MSE: %.2f dB²",
                                        startFreq, endFreq, sweepTime, mse), 10, 20);
        }

        public String getOptimizationCSV() {
            StringBuilder csv = new StringBuilder("Time (s),Start Freq (Hz),End Freq (Hz),Sweep Time (s),MSE (dB²)\n");
            if (errorHistory.isEmpty()) return csv.toString();
            for (int i = 0; i < errorHistory.size(); i++) {
                csv.append(String.format("%.3f,%.0f,%.0f,%.1f,%.2f\n",
                                        timeHistory.get(i), startFreq, endFreq, sweepTime, errorHistory.get(i)));
            }
            return csv.toString();
        }
    }
}