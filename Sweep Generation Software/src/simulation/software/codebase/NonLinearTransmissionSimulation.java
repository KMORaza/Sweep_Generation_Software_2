package simulation.software.codebase;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class NonLinearTransmissionSimulation extends JFrame {
    private SweepGenerator generator;
    private TransmissionWaveformPanel waveformPanel;
    private JComboBox<String> nonLinearTypeCombo;
    private JTextField startFreqField, endFreqField, sweepTimeField, nonLinearGainField, reflectionDelayField, reflectionCoeffField, interferenceFreqField, interferenceAmpField;
    private JButton startButton, stopButton, exportButton;
    private JLabel statusLabel;
    private Timer timer;
    private boolean running = false;
    private double sinad = Double.POSITIVE_INFINITY;
    private static final int SAMPLE_RATE = 44100;
    private static final int SAMPLE_COUNT = 1024;
    private static final double WINDOW_SIZE = 0.02; // 20ms

    public NonLinearTransmissionSimulation() {
        setTitle("Non-Linear Transmission Simulation");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Set Bahnschrift font
        Font bahnschrift = new Font("Bahnschrift", Font.PLAIN, 12);
        UIManager.put("Label.font", bahnschrift);
        UIManager.put("Button.font", bahnschrift);
        UIManager.put("TextField.font", bahnschrift);
        UIManager.put("ComboBox.font", bahnschrift);

        generator = new SweepGenerator();
        waveformPanel = new TransmissionWaveformPanel();
        add(waveformPanel, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBackground(Color.BLACK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Status Label
        statusLabel = new JLabel("Status: Idle, SINAD: ∞ dB");
        statusLabel.setForeground(Color.WHITE);
        gbc.gridwidth = 2;
        controlPanel.add(statusLabel, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;

        // Non-linear Type
        JLabel nonLinearTypeLabel = new JLabel("Nonlinear Type:");
        nonLinearTypeLabel.setForeground(Color.WHITE);
        controlPanel.add(nonLinearTypeLabel, gbc);
        gbc.gridx = 1;
        String[] nonLinearTypes = {"Tanh", "Cubic"}; // Moved array declaration
        nonLinearTypeCombo = new JComboBox<>(nonLinearTypes);
        controlPanel.add(nonLinearTypeCombo, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Non-linear Gain
        JLabel nonLinearGainLabel = new JLabel("Nonlinear Gain:");
        nonLinearGainLabel.setForeground(Color.WHITE);
        controlPanel.add(nonLinearGainLabel, gbc);
        gbc.gridx = 1;
        nonLinearGainField = new JTextField("1.0", 10);
        controlPanel.add(nonLinearGainField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Reflection Delay
        JLabel reflectionDelayLabel = new JLabel("Reflection Delay (ms):");
        reflectionDelayLabel.setForeground(Color.WHITE);
        controlPanel.add(reflectionDelayLabel, gbc);
        gbc.gridx = 1;
        reflectionDelayField = new JTextField("0.5", 10);
        controlPanel.add(reflectionDelayField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Reflection Coefficient
        JLabel reflectionCoeffLabel = new JLabel("Reflection Coefficient (%):");
        reflectionCoeffLabel.setForeground(Color.WHITE);
        controlPanel.add(reflectionCoeffLabel, gbc);
        gbc.gridx = 1;
        reflectionCoeffField = new JTextField("20", 10);
        controlPanel.add(reflectionCoeffField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Interference Frequency
        JLabel interferenceFreqLabel = new JLabel("Interference Freq (Hz):");
        interferenceFreqLabel.setForeground(Color.WHITE);
        controlPanel.add(interferenceFreqLabel, gbc);
        gbc.gridx = 1;
        interferenceFreqField = new JTextField("50", 10);
        controlPanel.add(interferenceFreqField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Interference Amplitude
        JLabel interferenceAmpLabel = new JLabel("Interference Amplitude:");
        interferenceAmpLabel.setForeground(Color.WHITE);
        controlPanel.add(interferenceAmpLabel, gbc);
        gbc.gridx = 1;
        interferenceAmpField = new JTextField("0.1", 10);
        controlPanel.add(interferenceAmpField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Start Frequency
        JLabel startFreqLabel = new JLabel("Start Frequency (Hz):");
        startFreqLabel.setForeground(Color.WHITE);
        controlPanel.add(startFreqLabel, gbc);
        gbc.gridx = 1;
        startFreqField = new JTextField("100", 10);
        controlPanel.add(startFreqField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // End Frequency
        JLabel endFreqLabel = new JLabel("End Frequency (Hz):");
        endFreqLabel.setForeground(Color.WHITE);
        controlPanel.add(endFreqLabel, gbc);
        gbc.gridx = 1;
        endFreqField = new JTextField("1000", 10);
        controlPanel.add(endFreqField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Sweep Time
        JLabel sweepTimeLabel = new JLabel("Sweep Time (s):");
        sweepTimeLabel.setForeground(Color.WHITE);
        controlPanel.add(sweepTimeLabel, gbc);
        gbc.gridx = 1;
        sweepTimeField = new JTextField("10", 10);
        controlPanel.add(sweepTimeField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Start/Stop Buttons
        startButton = new JButton("Start Transmission");
        gbc.gridwidth = 1;
        controlPanel.add(startButton, gbc);
        gbc.gridx = 1;
        stopButton = new JButton("Stop Transmission");
        controlPanel.add(stopButton, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Export Button
        exportButton = new JButton("Export CSV");
        gbc.gridwidth = 2;
        controlPanel.add(exportButton, gbc);

        add(controlPanel, BorderLayout.SOUTH);

        // Timer for 50ms updates
        timer = new Timer(50, e -> waveformPanel.repaint());

        // Button actions
        startButton.addActionListener(e -> {
            try {
                double startFreq = Double.parseDouble(startFreqField.getText());
                double endFreq = Double.parseDouble(endFreqField.getText());
                double sweepTime = Double.parseDouble(sweepTimeField.getText());
                double nonLinearGain = Double.parseDouble(nonLinearGainField.getText());
                double reflectionDelay = Double.parseDouble(reflectionDelayField.getText());
                double reflectionCoeff = Double.parseDouble(reflectionCoeffField.getText()) / 100.0;
                double interferenceFreq = Double.parseDouble(interferenceFreqField.getText());
                double interferenceAmplitude = Double.parseDouble(interferenceAmpField.getText());
                if (startFreq < 0 || endFreq <= startFreq || sweepTime <= 0) {
                    throw new NumberFormatException("Invalid frequency or time parameters.");
                }
                if (nonLinearGain < 0) {
                    throw new NumberFormatException("Non-linear gain must be positive.");
                }
                if (reflectionDelay <= 0) {
                    throw new NumberFormatException("Reflection delay must be positive.");
                }
                if (reflectionCoeff < -1 || reflectionCoeff > 1) {
                    throw new NumberFormatException("Reflection coefficient must be between -100% and 100%.");
                }
                if (interferenceFreq < 0) {
                    throw new NumberFormatException("Interference frequency must be non-negative.");
                }
                if (interferenceAmplitude < 0) {
                    throw new NumberFormatException("Interference amplitude must be non-negative.");
                }
                generator.startSweep(startFreq, endFreq, sweepTime, 500, 15, 0.8, 0, 10, 0.5, "Linear", 
                                    "Sine", "None", "None");
                running = true;
                statusLabel.setText(String.format("Status: Running %s, SINAD: %.2f dB", 
                                                 nonLinearTypeCombo.getSelectedItem(), sinad));
                timer.start();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error starting transmission simulation: " + ex.getMessage());
            }
        });

        stopButton.addActionListener(e -> {
            generator.stopSweep();
            running = false;
            timer.stop();
            statusLabel.setText("Status: Idle, SINAD: ∞ dB");
            waveformPanel.repaint();
        });

        exportButton.addActionListener(e -> {
            if (!running) {
                JOptionPane.showMessageDialog(this, "Start the transmission simulation to export data.");
                return;
            }
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Transmission Waveform CSV");
            fileChooser.setSelectedFile(new File("transmission_waveform.csv"));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".csv")) {
                    file = new File(file.getPath() + ".csv");
                }
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(getTransmissionWaveformCSV());
                    JOptionPane.showMessageDialog(this, "CSV saved to " + file.getPath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving CSV: " + ex.getMessage());
                }
            }
        });
    }

    private double[] getTransmissionWaveformSamples() {
        double[] originalSamples = generator.getWaveformSamples(SAMPLE_COUNT, SAMPLE_RATE, WINDOW_SIZE);
        double nonLinearGain = Double.parseDouble(nonLinearGainField.getText());
        double reflectionDelay = Double.parseDouble(reflectionDelayField.getText()) * 1e-3; // ms to s
        double reflectionCoefficient = Double.parseDouble(reflectionCoeffField.getText()) / 100.0;
        double interferenceFreq = Double.parseDouble(interferenceFreqField.getText());
        double interferenceAmp = Double.parseDouble(interferenceAmpField.getText());
        String nonLinearType = (String) nonLinearTypeCombo.getSelectedItem();
        double[] distortedSamples = new double[SAMPLE_COUNT];
        double timeStep = WINDOW_SIZE / SAMPLE_COUNT;

        // Apply non-linear distortion
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double input = originalSamples[i];
            if (nonLinearType.equals("Tanh")) {
                distortedSamples[i] = Math.tanh(nonLinearGain * input);
            } else { // Cubic
                distortedSamples[i] = input * (1 + nonLinearGain * input * input);
            }
        }

        // Apply reflection (single echo)
        int delaySamples = (int) (reflectionDelay * SAMPLE_RATE);
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            if (i >= delaySamples) {
                distortedSamples[i] += reflectionCoefficient * originalSamples[i - delaySamples];
            }
        }

        // Apply interference
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double t = i * timeStep;
            distortedSamples[i] += interferenceAmp * Math.sin(2 * Math.PI * interferenceFreq * t);
        }

        // Compute SINAD
        sinad = computeSINAD(originalSamples, distortedSamples);

        return distortedSamples;
    }

    private double computeSINAD(double[] original, double[] distorted) {
        double signalPower = 0;
        double errorPower = 0;
        for (int i = 0; i < original.length; i++) {
            signalPower += original[i] * original[i];
            double error = distorted[i] - original[i];
            errorPower += error * error;
        }
        signalPower /= original.length;
        errorPower /= original.length;
        if (errorPower == 0) return Double.POSITIVE_INFINITY;
        return 10 * Math.log10(signalPower / errorPower);
    }

    private String getTransmissionWaveformCSV() {
        double[] samples = getTransmissionWaveformSamples();
        StringBuilder csv = new StringBuilder("Time (s),Amplitude\n");
        double timeStep = WINDOW_SIZE / SAMPLE_COUNT;
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double time = i * timeStep;
            csv.append(String.format("%.6f,%.6f\n", time, samples[i]));
        }
        return csv.toString();
    }

    private class TransmissionWaveformPanel extends JPanel {
        public TransmissionWaveformPanel() {
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
            for (int y = 0; y <= height; y += 50) {
                g2d.drawLine(0, y, width, y);
            }
            for (int x = 0; x <= width; x += 50) {
                g2d.drawLine(x, 0, x, height);
            }

            // Draw original waveform (gray)
            if (running) {
                double[] originalSamples = generator.getWaveformSamples(SAMPLE_COUNT, SAMPLE_RATE, WINDOW_SIZE);
                g2d.setColor(Color.GRAY);
                for (int i = 1; i < SAMPLE_COUNT; i++) {
                    int x1 = (i - 1) * width / SAMPLE_COUNT;
                    int x2 = i * width / SAMPLE_COUNT;
                    int y1 = (int) (midY - originalSamples[i - 1] * (height / 2 - 50));
                    int y2 = (int) (midY - originalSamples[i] * (height / 2 - 50));
                    g2d.drawLine(x1, y1, x2, y2);
                }

                // Draw distorted waveform (cyan)
                double[] distortedSamples = getTransmissionWaveformSamples();
                g2d.setColor(Color.CYAN);
                for (int i = 1; i < SAMPLE_COUNT; i++) {
                    int x1 = (i - 1) * width / SAMPLE_COUNT;
                    int x2 = i * width / SAMPLE_COUNT;
                    int y1 = (int) (midY - distortedSamples[i - 1] * (height / 2 - 50));
                    int y2 = (int) (midY - distortedSamples[i] * (height / 2 - 50));
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }

            // Draw axes labels
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Bahnschrift", Font.PLAIN, 12));
            g2d.drawString("Time (s)", width / 2 - 20, height - 10);
            g2d.rotate(-Math.PI / 2);
            g2d.drawString("Amplitude", -height / 2 - 20, 20);
            g2d.rotate(Math.PI / 2);
        }
    }
}