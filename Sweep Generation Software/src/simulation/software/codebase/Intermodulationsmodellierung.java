package simulation.software.codebase;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class Intermodulationsmodellierung extends JFrame {
    private SweepGenerator generator;
    private WaveformPanel waveformPanel;
    private SpectrumPanel spectrumPanel;
    private JTextField tone1FreqField, tone2FreqField, toneAmplitudeField, harmonicOrderField, harmonicScaleField, spurFreqField, spurAmpField, phaseNoiseField;
    private JButton startButton, stopButton, exportWaveformButton, exportSpectrumButton;
    private JLabel statusLabel;
    private Timer timer;
    private boolean running = false;
    private double iip3 = Double.POSITIVE_INFINITY;
    private double thd = 0;
    private double sinad = Double.POSITIVE_INFINITY;
    private static final int SAMPLE_RATE = 44100;
    private static final int SAMPLE_COUNT = 1024;
    private static final double WINDOW_SIZE = 0.02; // 20ms
    private Random random = new Random();

    public Intermodulationsmodellierung() {
        setTitle("Intermodulation Modeling");
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
        waveformPanel = new WaveformPanel();
        spectrumPanel = new SpectrumPanel();

        // Layout: Waveform at top, spectrum at center
        JPanel displayPanel = new JPanel(new GridLayout(2, 1));
        displayPanel.add(waveformPanel);
        displayPanel.add(spectrumPanel);
        add(displayPanel, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBackground(Color.BLACK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Status Label
        statusLabel = new JLabel("Status: Idle, IIP3: ∞ dBm, THD: 0%, SINAD: ∞ dB");
        statusLabel.setForeground(Color.WHITE);
        gbc.gridwidth = 2;
        controlPanel.add(statusLabel, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;

        // Tone 1 Frequency
        JLabel tone1FreqLabel = new JLabel("Tone 1 Freq (Hz):");
        tone1FreqLabel.setForeground(Color.WHITE);
        controlPanel.add(tone1FreqLabel, gbc);
        gbc.gridx = 1;
        tone1FreqField = new JTextField("1000", 10);
        controlPanel.add(tone1FreqField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Tone 2 Frequency
        JLabel tone2FreqLabel = new JLabel("Tone 2 Freq (Hz):");
        tone2FreqLabel.setForeground(Color.WHITE);
        controlPanel.add(tone2FreqLabel, gbc);
        gbc.gridx = 1;
        tone2FreqField = new JTextField("1100", 10);
        controlPanel.add(tone2FreqField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Tone Amplitude
        JLabel toneAmpLabel = new JLabel("Tone Amplitude:");
        toneAmpLabel.setForeground(Color.WHITE);
        controlPanel.add(toneAmpLabel, gbc);
        gbc.gridx = 1;
        toneAmplitudeField = new JTextField("0.5", 10);
        controlPanel.add(toneAmplitudeField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Harmonic Order
        JLabel harmonicOrderLabel = new JLabel("Max Harmonic Order:");
        harmonicOrderLabel.setForeground(Color.WHITE);
        controlPanel.add(harmonicOrderLabel, gbc);
        gbc.gridx = 1;
        harmonicOrderField = new JTextField("3", 10);
        controlPanel.add(harmonicOrderField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Harmonic Scale
        JLabel harmonicScaleLabel = new JLabel("Harmonic Scale:");
        harmonicScaleLabel.setForeground(Color.WHITE);
        controlPanel.add(harmonicScaleLabel, gbc);
        gbc.gridx = 1;
        harmonicScaleField = new JTextField("0.1", 10);
        controlPanel.add(harmonicScaleField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Spur Frequency
        JLabel spurFreqLabel = new JLabel("Spur Freq (Hz):");
        spurFreqLabel.setForeground(Color.WHITE);
        controlPanel.add(spurFreqLabel, gbc);
        gbc.gridx = 1;
        spurFreqField = new JTextField("60", 10);
        controlPanel.add(spurFreqField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Spur Amplitude
        JLabel spurAmpLabel = new JLabel("Spur Amplitude:");
        spurAmpLabel.setForeground(Color.WHITE);
        controlPanel.add(spurAmpLabel, gbc);
        gbc.gridx = 1;
        spurAmpField = new JTextField("0.05", 10);
        controlPanel.add(spurAmpField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Phase Noise Variance
        JLabel phaseNoiseLabel = new JLabel("Phase Noise Variance:");
        phaseNoiseLabel.setForeground(Color.WHITE);
        controlPanel.add(phaseNoiseLabel, gbc);
        gbc.gridx = 1;
        phaseNoiseField = new JTextField("0.01", 10);
        controlPanel.add(phaseNoiseField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Start/Stop Buttons
        startButton = new JButton("Start Simulation");
        gbc.gridwidth = 1;
        controlPanel.add(startButton, gbc);
        gbc.gridx = 1;
        stopButton = new JButton("Stop Simulation");
        controlPanel.add(stopButton, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Export Buttons
        exportWaveformButton = new JButton("Export Waveform CSV");
        gbc.gridwidth = 1;
        controlPanel.add(exportWaveformButton, gbc);
        gbc.gridx = 1;
        exportSpectrumButton = new JButton("Export Spectrum CSV");
        controlPanel.add(exportSpectrumButton, gbc);

        add(controlPanel, BorderLayout.SOUTH);

        // Timer for 50ms updates
        timer = new Timer(50, e -> {
            waveformPanel.repaint();
            spectrumPanel.repaint();
        });

        // Button actions
        startButton.addActionListener(e -> {
            try {
                double tone1Freq = Double.parseDouble(tone1FreqField.getText());
                double tone2Freq = Double.parseDouble(tone2FreqField.getText());
                double toneAmplitude = Double.parseDouble(toneAmplitudeField.getText());
                int harmonicOrder = Integer.parseInt(harmonicOrderField.getText());
                double harmonicScale = Double.parseDouble(harmonicScaleField.getText());
                double spurFreq = Double.parseDouble(spurFreqField.getText());
                double spurAmp = Double.parseDouble(spurAmpField.getText());
                double phaseNoiseVariance = Double.parseDouble(phaseNoiseField.getText());
                if (tone1Freq <= 0 || tone2Freq <= 0 || tone1Freq == tone2Freq) {
                    throw new NumberFormatException("Tone frequencies must be positive and distinct.");
                }
                if (toneAmplitude <= 0 || toneAmplitude > 1) {
                    throw new NumberFormatException("Tone amplitude must be between 0 and 1.");
                }
                if (harmonicOrder < 1 || harmonicOrder > 10) {
                    throw new NumberFormatException("Harmonic order must be 1 to 10.");
                }
                if (harmonicScale < 0 || harmonicScale > 1) {
                    throw new NumberFormatException("Harmonic scale must be 0 to 1.");
                }
                if (spurFreq < 0) {
                    throw new NumberFormatException("Spur frequency must be non-negative.");
                }
                if (spurAmp < 0 || spurAmp > 1) {
                    throw new NumberFormatException("Spur amplitude must be 0 to 1.");
                }
                if (phaseNoiseVariance < 0 || phaseNoiseVariance > 0.1) {
                    throw new NumberFormatException("Phase noise variance must be 0 to 0.1.");
                }
                generator.startSweep(tone1Freq, tone2Freq, 10, 500, 15, 0.8, 0, 10, 0.5, "Linear", 
                                    "Sine", "None", "None");
                running = true;
                statusLabel.setText(String.format("Status: Running, IIP3: %.2f dBm, THD: %.2f%%, SINAD: %.2f dB", 
                                                 iip3, thd * 100, sinad));
                timer.start();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error starting simulation: " + ex.getMessage());
            }
        });

        stopButton.addActionListener(e -> {
            generator.stopSweep();
            running = false;
            timer.stop();
            statusLabel.setText("Status: Idle, IIP3: ∞ dBm, THD: 0%, SINAD: ∞ dB");
            waveformPanel.repaint();
            spectrumPanel.repaint();
        });

        exportWaveformButton.addActionListener(e -> {
            if (!running) {
                JOptionPane.showMessageDialog(this, "Start the simulation to export waveform data.");
                return;
            }
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Waveform CSV");
            fileChooser.setSelectedFile(new File("intermodulation_waveform.csv"));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".csv")) {
                    file = new File(file.getPath() + ".csv");
                }
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(getWaveformCSV());
                    JOptionPane.showMessageDialog(this, "Waveform CSV saved to " + file.getPath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving CSV: " + ex.getMessage());
                }
            }
        });

        exportSpectrumButton.addActionListener(e -> {
            if (!running) {
                JOptionPane.showMessageDialog(this, "Start the simulation to export spectrum data.");
                return;
            }
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Spectrum CSV");
            fileChooser.setSelectedFile(new File("intermodulation_spectrum.csv"));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".csv")) {
                    file = new File(file.getPath() + ".csv");
                }
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(getSpectrumCSV());
                    JOptionPane.showMessageDialog(this, "Spectrum CSV saved to " + file.getPath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving CSV: " + ex.getMessage());
                }
            }
        });
    }

    private double[] getDistortedWaveformSamples() {
        double[] originalSamples = generator.getWaveformSamples(SAMPLE_COUNT, SAMPLE_RATE, WINDOW_SIZE);
        double tone1Freq = Double.parseDouble(tone1FreqField.getText());
        double tone2Freq = Double.parseDouble(tone2FreqField.getText());
        double toneAmplitude = Double.parseDouble(toneAmplitudeField.getText());
        int harmonicOrder = Integer.parseInt(harmonicOrderField.getText());
        double harmonicScale = Double.parseDouble(harmonicScaleField.getText());
        double spurFreq = Double.parseDouble(spurFreqField.getText());
        double spurAmp = Double.parseDouble(spurAmpField.getText());
        double phaseNoiseVariance = Double.parseDouble(phaseNoiseField.getText());
        double[] distortedSamples = new double[SAMPLE_COUNT];
        double timeStep = WINDOW_SIZE / SAMPLE_COUNT;

        // Generate two-tone signal with phase noise
        double[] twoToneSamples = new double[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double t = i * timeStep;
            double phaseNoise = random.nextGaussian() * Math.sqrt(phaseNoiseVariance);
            twoToneSamples[i] = toneAmplitude * (Math.sin(2 * Math.PI * tone1Freq * t + phaseNoise) + 
                                                Math.sin(2 * Math.PI * tone2Freq * t + phaseNoise));
        }

        // Apply non-linear distortion (cubic for IMD)
        double nonLinearGain = 0.1; // Controls IMD strength
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double x = twoToneSamples[i];
            distortedSamples[i] = x + nonLinearGain * x * x * x; // y = x + kx^3
        }

        // Add harmonics
        for (int n = 2; n <= harmonicOrder; n++) {
            for (int i = 0; i < SAMPLE_COUNT; i++) {
                double t = i * timeStep;
                double phaseNoise = random.nextGaussian() * Math.sqrt(phaseNoiseVariance);
                distortedSamples[i] += harmonicScale / n * toneAmplitude * (
                    Math.sin(2 * Math.PI * n * tone1Freq * t + phaseNoise) + 
                    Math.sin(2 * Math.PI * n * tone2Freq * t + phaseNoise)
                );
            }
        }

        // Add spur
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double t = i * timeStep;
            distortedSamples[i] += spurAmp * Math.sin(2 * Math.PI * spurFreq * t);
        }

        // Compute metrics
        computeMetrics(twoToneSamples, distortedSamples);

        return distortedSamples;
    }

    private void computeMetrics(double[] original, double[] distorted) {
        // SINAD
        double signalPower = 0;
        double errorPower = 0;
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            signalPower += original[i] * original[i];
            double error = distorted[i] - original[i];
            errorPower += error * error;
        }
        signalPower /= SAMPLE_COUNT;
        errorPower /= SAMPLE_COUNT;
        sinad = errorPower == 0 ? Double.POSITIVE_INFINITY : 10 * Math.log10(signalPower / errorPower);

        // THD (harmonic power vs fundamental)
        double[] spectrum = computeSpectrum(distorted);
        double tone1Freq = Double.parseDouble(tone1FreqField.getText());
        int harmonicOrder = Integer.parseInt(harmonicOrderField.getText());
        double fundamentalPower = 0;
        double harmonicPower = 0;
        int bin1 = (int) (tone1Freq * SAMPLE_COUNT / SAMPLE_RATE);
        fundamentalPower += spectrum[bin1] * spectrum[bin1];
        for (int n = 2; n <= harmonicOrder; n++) {
            int bin = n * bin1;
            if (bin < spectrum.length) {
                harmonicPower += spectrum[bin] * spectrum[bin];
            }
        }
        thd = harmonicPower == 0 ? 0 : Math.sqrt(harmonicPower / fundamentalPower);

        // IIP3 (simplified estimation)
        double imdPower = 0;
        int imdBin = (int) ((2 * tone1Freq - Double.parseDouble(tone2FreqField.getText())) * SAMPLE_COUNT / SAMPLE_RATE);
        if (imdBin >= 0 && imdBin < spectrum.length) {
            imdPower = spectrum[imdBin] * spectrum[imdBin];
        }
        if (imdPower > 0) {
            double inputPower = signalPower / SAMPLE_COUNT;
            iip3 = 10 * Math.log10(inputPower) + (10 * Math.log10(inputPower / imdPower) / 2);
        } else {
            iip3 = Double.POSITIVE_INFINITY;
        }
    }

    private double[] computeSpectrum(double[] samples) {
        double[] spectrum = new double[SAMPLE_COUNT / 2];
        for (int k = 0; k < spectrum.length; k++) {
            double re = 0, im = 0;
            for (int n = 0; n < SAMPLE_COUNT; n++) {
                double angle = 2 * Math.PI * k * n / SAMPLE_COUNT;
                re += samples[n] * Math.cos(angle);
                im -= samples[n] * Math.sin(angle);
            }
            spectrum[k] = Math.sqrt(re * re + im * im) / SAMPLE_COUNT;
        }
        return spectrum;
    }

    private String getWaveformCSV() {
        double[] samples = getDistortedWaveformSamples();
        StringBuilder csv = new StringBuilder("Time (s),Amplitude\n");
        double timeStep = WINDOW_SIZE / SAMPLE_COUNT;
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double time = i * timeStep;
            csv.append(String.format("%.6f,%.6f\n", time, samples[i]));
        }
        return csv.toString();
    }

    private String getSpectrumCSV() {
        double[] samples = getDistortedWaveformSamples();
        double[] spectrum = computeSpectrum(samples);
        StringBuilder csv = new StringBuilder("Frequency (Hz),Magnitude\n");
        double freqStep = SAMPLE_RATE / (double) SAMPLE_COUNT;
        for (int i = 0; i < spectrum.length; i++) {
            double freq = i * freqStep;
            csv.append(String.format("%.2f,%.6f\n", freq, spectrum[i]));
        }
        return csv.toString();
    }

    private class WaveformPanel extends JPanel {
        public WaveformPanel() {
            setBackground(Color.BLACK);
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
                double[] distortedSamples = getDistortedWaveformSamples();
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

    private class SpectrumPanel extends JPanel {
        public SpectrumPanel() {
            setBackground(Color.BLACK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Draw grid
            g2d.setColor(Color.DARK_GRAY);
            for (int y = 0; y <= height; y += 50) {
                g2d.drawLine(0, y, width, y);
            }
            for (int x = 0; x <= width; x += 100) {
                g2d.drawLine(x, 0, x, height);
            }

            // Draw spectrum
            if (running) {
                double[] distortedSamples = getDistortedWaveformSamples();
                double[] spectrum = computeSpectrum(distortedSamples);
                g2d.setColor(Color.YELLOW);
                double maxMag = 0;
                for (double s : spectrum) {
                    maxMag = Math.max(maxMag, s);
                }
                maxMag = maxMag == 0 ? 1 : maxMag;
                for (int i = 1; i < spectrum.length; i++) {
                    int x1 = (i - 1) * width / spectrum.length;
                    int x2 = i * width / spectrum.length;
                    int y1 = (int) (height - (spectrum[i - 1] / maxMag) * (height - 50));
                    int y2 = (int) (height - (spectrum[i] / maxMag) * (height - 50));
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }

            // Draw axes labels
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Bahnschrift", Font.PLAIN, 12));
            g2d.drawString("Frequency (Hz)", width / 2 - 20, height - 10);
            g2d.rotate(-Math.PI / 2);
            g2d.drawString("Magnitude", -height / 2 - 20, 20);
            g2d.rotate(Math.PI / 2);
        }
    }
}