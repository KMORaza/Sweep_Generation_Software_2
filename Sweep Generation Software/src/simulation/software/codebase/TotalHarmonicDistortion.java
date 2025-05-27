package simulation.software.codebase;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TotalHarmonicDistortion extends JFrame {
    private SweepGenerator generator;
    private ThdPanel thdPanel;
    private JLabel thdLabel;
    private JLabel fundamentalLabel;
    private JSlider windowSizeSlider;
    private JButton exportThdButton;
    private static final int SAMPLE_RATE = 44100;

    public TotalHarmonicDistortion(SweepGenerator generator) {
        this.generator = generator;
        setTitle("Total Harmonic Distortion Analysis");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Set Bahnschrift font
        Font bahnschrift = new Font("Bahnschrift", Font.PLAIN, 12);
        UIManager.put("Label.font", bahnschrift);
        UIManager.put("Button.font", bahnschrift);
        UIManager.put("Slider.font", bahnschrift);

        thdPanel = new ThdPanel(generator, () -> windowSizeSlider.getValue() / 1000.0);
        add(thdPanel, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // THD Label
        thdLabel = new JLabel("THD: N/A");
        thdLabel.setFont(bahnschrift);
        gbc.gridwidth = 2;
        controlPanel.add(thdLabel, gbc);
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
        windowSizeSlider.addChangeListener(e -> thdPanel.repaint());
        controlPanel.add(windowSizeSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Export Button
        exportThdButton = new JButton("Export THD CSV");
        gbc.gridwidth = 2;
        controlPanel.add(exportThdButton, gbc);

        add(controlPanel, BorderLayout.SOUTH);

        exportThdButton.addActionListener(e -> {
            if (!generator.isRunning()) {
                JOptionPane.showMessageDialog(this, "Start the sweep to export data.");
                return;
            }
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save THD CSV");
            fileChooser.setSelectedFile(new File("thd_data.csv"));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".csv")) {
                    file = new File(file.getPath() + ".csv");
                }
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(thdPanel.getThdCSV());
                    JOptionPane.showMessageDialog(this, "THD CSV saved to " + file.getPath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving CSV: " + ex.getMessage());
                }
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (thdPanel != null) {
            thdPanel.repaint();
        }
    }

    private class ThdPanel extends JPanel {
        private SweepGenerator generator;
        private java.util.function.Supplier<Double> windowSizeSupplier;
        private static final int MAX_HARMONICS = 10;
        private double[] harmonicMagnitudes;
        private double fundamentalFreq;
        private double thdPercent;

        public ThdPanel(SweepGenerator generator, java.util.function.Supplier<Double> windowSizeSupplier) {
            this.generator = generator;
            this.windowSizeSupplier = windowSizeSupplier;
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

            // Draw grid
            g2d.setColor(Color.DARK_GRAY);
            for (int i = 0; i < height; i += 50) {
                g2d.drawLine(0, i, width, i);
            }
            for (int i = 0; i < width; i += 50) {
                g2d.drawLine(i, 0, i, height);
            }

            if (!generator.isRunning()) {
                thdLabel.setText("THD: N/A");
                fundamentalLabel.setText("Fundamental: N/A");
                harmonicMagnitudes = null;
                return;
            }

            // Get waveform samples
            double windowSize = windowSizeSupplier.get();
            int sampleCount = (int) (windowSize * SAMPLE_RATE);
            double[] samples = generator.getWaveformSamples(sampleCount, SAMPLE_RATE, windowSize);

            // Compute FFT
            FFTCalculator fft = new FFTCalculator();
            double[] magnitude = fft.computeFFTMagnitude(samples);
            if (magnitude.length == 0) {
                thdLabel.setText("THD: N/A");
                fundamentalLabel.setText("Fundamental: N/A");
                return;
            }

            // Find fundamental frequency
            int fundamentalIndex = 0;
            double maxMagnitude = 0;
            for (int i = 1; i < magnitude.length / 2; i++) {
                if (magnitude[i] > maxMagnitude) {
                    maxMagnitude = magnitude[i];
                    fundamentalIndex = i;
                }
            }
            double freqStep = SAMPLE_RATE / (double) (magnitude.length * 2);
            fundamentalFreq = fundamentalIndex * freqStep;

            // Calculate THD
            harmonicMagnitudes = new double[MAX_HARMONICS + 1]; // 0 for fundamental, 1-10 for harmonics
            harmonicMagnitudes[0] = magnitude[fundamentalIndex];
            double harmonicPower = 0;
            for (int n = 2; n <= MAX_HARMONICS; n++) {
                int harmonicIndex = fundamentalIndex * n;
                if (harmonicIndex < magnitude.length) {
                    harmonicMagnitudes[n - 1] = magnitude[harmonicIndex];
                    harmonicPower += magnitude[harmonicIndex] * magnitude[harmonicIndex];
                }
            }
            double fundamentalPower = harmonicMagnitudes[0] * harmonicMagnitudes[0];
            thdPercent = fundamentalPower > 0 ? 100 * Math.sqrt(harmonicPower / fundamentalPower) : 0;
            double thdDb = thdPercent > 0 ? 20 * Math.log10(thdPercent / 100) : -100;

            // Update labels
            thdLabel.setText(String.format("THD: %.2f%% (%.1f dB)", thdPercent, thdDb));
            fundamentalLabel.setText(String.format("Fundamental: %.1f Hz", fundamentalFreq));

            // Draw harmonic spectrum
            g2d.setColor(Color.YELLOW);
            int barWidth = width / (MAX_HARMONICS + 1) / 2;
            double maxDisplayMag = maxMagnitude * 1.1; // Add 10% headroom
            for (int n = 0; n <= MAX_HARMONICS; n++) {
                int x = (int) (n * width / (double) (MAX_HARMONICS + 1));
                double mag = harmonicMagnitudes[n];
                int barHeight = (int) (mag / maxDisplayMag * (height - 50));
                g2d.fillRect(x, height - barHeight, barWidth, barHeight);
                g2d.setColor(Color.WHITE);
                g2d.drawString(String.format("%d", n + 1), x + barWidth / 2, height - 10);
                g2d.setColor(Color.YELLOW);
            }

            // Draw axes labels
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Bahnschrift", Font.PLAIN, 12));
            g2d.drawString("Harmonic Number", width / 2 - 50, height - 30);
            g2d.rotate(-Math.PI / 2);
            g2d.drawString("Magnitude", -height / 2 - 30, 20);
            g2d.rotate(Math.PI / 2);
        }

        public String getThdCSV() {
            if (harmonicMagnitudes == null) return "Fundamental (Hz),THD (%),THD (dB),H1,H2,H3,H4,H5,H6,H7,H8,H9,H10\n";
            StringBuilder csv = new StringBuilder("Fundamental (Hz),THD (%),THD (dB),H1,H2,H3,H4,H5,H6,H7,H8,H9,H10\n");
            double thdDb = thdPercent > 0 ? 20 * Math.log10(thdPercent / 100) : -100;
            csv.append(String.format("%.2f,%.2f,%.1f", fundamentalFreq, thdPercent, thdDb));
            for (int n = 0; n < MAX_HARMONICS; n++) {
                csv.append(String.format(",%.6f", harmonicMagnitudes[n]));
            }
            csv.append("\n");
            return csv.toString();
        }
    }
}