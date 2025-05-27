package simulation.software.codebase;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class DigitalToAnalog extends JFrame {
    private SweepGenerator generator;
    private DacPanel dacPanel;
    private JSlider bitDepthSlider;
    private JSlider samplingRateSlider;
    private JSlider nonlinearitySlider;
    private JSlider thermalNoiseSlider;
    private JLabel snrLabel;
    private JButton exportDacButton;

    public DigitalToAnalog(SweepGenerator generator) {
        this.generator = generator;
        setTitle("Digital to Analog Converter Simulation");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Set Bahnschrift font
        Font bahnschrift = new Font("Bahnschrift", Font.PLAIN, 12);
        UIManager.put("Label.font", bahnschrift);
        UIManager.put("Button.font", bahnschrift);
        UIManager.put("Slider.font", bahnschrift);

        dacPanel = new DacPanel(generator, () -> bitDepthSlider.getValue(),
                () -> samplingRateSlider.getValue(),
                () -> nonlinearitySlider.getValue(),
                () -> thermalNoiseSlider.getValue());
        add(dacPanel, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // SNR Label
        snrLabel = new JLabel("SNR: N/A");
        snrLabel.setFont(bahnschrift);
        gbc.gridwidth = 2;
        controlPanel.add(snrLabel, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;

        // Bit Depth Slider
        JLabel bitDepthLabel = new JLabel("Bit Depth:");
        controlPanel.add(bitDepthLabel, gbc);
        gbc.gridx = 1;
        bitDepthSlider = new JSlider(JSlider.HORIZONTAL, 4, 16, 12);
        bitDepthSlider.setMajorTickSpacing(4);
        bitDepthSlider.setMinorTickSpacing(1);
        bitDepthSlider.setPaintTicks(true);
        bitDepthSlider.setPaintLabels(true);
        bitDepthSlider.setSnapToTicks(true);
        bitDepthSlider.addChangeListener(e -> dacPanel.repaint());
        controlPanel.add(bitDepthSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Sampling Rate Slider
        JLabel samplingRateLabel = new JLabel("Sampling Rate (Hz):");
        controlPanel.add(samplingRateLabel, gbc);
        gbc.gridx = 1;
        samplingRateSlider = new JSlider(JSlider.HORIZONTAL, 1000, 20000, 10000);
        samplingRateSlider.setMajorTickSpacing(5000);
        samplingRateSlider.setMinorTickSpacing(1000);
        samplingRateSlider.setPaintTicks(true);
        samplingRateSlider.setPaintLabels(true);
        samplingRateSlider.addChangeListener(e -> dacPanel.repaint());
        controlPanel.add(samplingRateSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Non-linearity Slider
        JLabel nonlinearityLabel = new JLabel("Non-linearity:");
        controlPanel.add(nonlinearityLabel, gbc);
        gbc.gridx = 1;
        nonlinearitySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0); // 0 to 0.1 scaled by 1000
        nonlinearitySlider.setMajorTickSpacing(50);
        nonlinearitySlider.setMinorTickSpacing(10);
        nonlinearitySlider.setPaintTicks(true);
        nonlinearitySlider.setPaintLabels(true);
        nonlinearitySlider.addChangeListener(e -> dacPanel.repaint());
        controlPanel.add(nonlinearitySlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Thermal Noise Slider
        JLabel thermalNoiseLabel = new JLabel("Thermal Noise (V):");
        controlPanel.add(thermalNoiseLabel, gbc);
        gbc.gridx = 1;
        thermalNoiseSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0); // 0 to 0.1 scaled by 1000
        thermalNoiseSlider.setMajorTickSpacing(50);
        thermalNoiseSlider.setMinorTickSpacing(10);
        thermalNoiseSlider.setPaintTicks(true);
        thermalNoiseSlider.setPaintLabels(true);
        thermalNoiseSlider.addChangeListener(e -> dacPanel.repaint());
        controlPanel.add(thermalNoiseSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Export Button
        exportDacButton = new JButton("Export DAC CSV");
        gbc.gridwidth = 2;
        controlPanel.add(exportDacButton, gbc);

        add(controlPanel, BorderLayout.SOUTH);

        exportDacButton.addActionListener(e -> {
            if (!generator.isRunning()) {
                JOptionPane.showMessageDialog(this, "Start the sweep to export data.");
                return;
            }
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save DAC CSV");
            fileChooser.setSelectedFile(new File("dac_output.csv"));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".csv")) {
                    file = new File(file.getPath() + ".csv");
                }
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(dacPanel.getDacCSV());
                    JOptionPane.showMessageDialog(this, "DAC CSV saved to " + file.getPath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving CSV: " + ex.getMessage());
                }
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (dacPanel != null) {
            dacPanel.repaint();
        }
    }

    private class DacPanel extends JPanel {
        private SweepGenerator generator;
        private java.util.function.Supplier<Integer> bitDepthSupplier;
        private java.util.function.Supplier<Integer> samplingRateSupplier;
        private java.util.function.Supplier<Integer> nonlinearitySupplier;
        private java.util.function.Supplier<Integer> thermalNoiseSupplier;
        private static final int POINTS = 1000;
        private static final double DISPLAY_WINDOW = 0.1; // 100ms window
        private double[] dacOutput;

        public DacPanel(SweepGenerator generator,
                        java.util.function.Supplier<Integer> bitDepthSupplier,
                        java.util.function.Supplier<Integer> samplingRateSupplier,
                        java.util.function.Supplier<Integer> nonlinearitySupplier,
                        java.util.function.Supplier<Integer> thermalNoiseSupplier) {
            this.generator = generator;
            this.bitDepthSupplier = bitDepthSupplier;
            this.samplingRateSupplier = samplingRateSupplier;
            this.nonlinearitySupplier = nonlinearitySupplier;
            this.thermalNoiseSupplier = thermalNoiseSupplier;
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
                snrLabel.setText("SNR: N/A");
                return;
            }

            // DAC parameters
            int bitDepth = bitDepthSupplier.get();
            double samplingRate = samplingRateSupplier.get();
            double nonlinearity = nonlinearitySupplier.get() / 1000.0; // Scale to 0–0.1
            double thermalNoiseAmp = thermalNoiseSupplier.get() / 1000.0; // Scale to 0–0.1
            int levels = 1 << bitDepth;
            double maxAmplitude = 1.0; // Normalized waveform [-1, 1]
            double stepSize = 2.0 * maxAmplitude / levels;
            Random rand = new Random();

            // Generate samples
            int newSamples = (int) (DISPLAY_WINDOW * samplingRate);
            double[] sampledTime = new double[newSamples];
            double[] analogOutput = new double[newSamples];
            dacOutput = new double[newSamples];

            // Resample waveform
            for (int i = 0; i < newSamples; i++) {
                double t = i / samplingRate;
                sampledTime[i] = t;
                analogOutput[i] = generator.calculateWaveform(t);
            }

            // Apply DAC effects
            double signalPower = 0;
            double noisePower = 0;
            for (int i = 0; i < newSamples; i++) {
                double value = analogOutput[i];
                // Quantization
                int quantLevel = (int) Math.round(value / stepSize);
                value = quantLevel * stepSize;
                value = Math.max(-maxAmplitude, Math.min(maxAmplitude, value));

                // Non-linearity: y = x + k*x^3
                double nonlinear = value + nonlinearity * Math.pow(value, 3);
                value = Math.max(-maxAmplitude, Math.min(maxAmplitude, nonlinear));

                // Thermal noise
                double noise = thermalNoiseAmp * (2 * rand.nextDouble() - 1);
                value += noise;

                dacOutput[i] = value;
                signalPower += Math.pow(analogOutput[i], 2);
                noisePower += Math.pow(value - analogOutput[i], 2);
            }

            // Compute SNR
            if (signalPower > 0 && noisePower > 0) {
                double snr = 10 * Math.log10(signalPower / noisePower);
                snrLabel.setText(String.format("SNR: %.1f dB", snr));
            } else {
                snrLabel.setText("SNR: N/A");
            }

            // Draw original waveform (green)
            g2d.setColor(Color.GREEN);
            for (int i = 0; i < newSamples - 1; i++) {
                int x1 = (int) (i * width / (double) newSamples);
                int x2 = (int) ((i + 1) * width / (double) newSamples);
                int y1 = (int) (midY - analogOutput[i] * midY * 0.5);
                int y2 = (int) (midY - analogOutput[i + 1] * midY * 0.5);
                g2d.drawLine(x1, y1, x2, y2);
            }

            // Draw quantized waveform (red)
            g2d.setColor(Color.RED);
            for (int i = 0; i < newSamples - 1; i++) {
                int x1 = (int) (i * width / (double) newSamples);
                int x2 = (int) ((i + 1) * width / (double) newSamples);
                int y1 = (int) (midY - dacOutput[i] * midY * 0.5);
                int y2 = (int) (midY - dacOutput[i + 1] * midY * 0.5);
                g2d.drawLine(x1, y1, x2, y2);
            }

            // Draw info
            g2d.setFont(new Font("Bahnschrift", Font.BOLD, 14));
            g2d.setColor(Color.WHITE);
            g2d.drawString(String.format("Bit Depth: %d bits", bitDepth), 10, 30);
            g2d.drawString(String.format("Sampling Rate: %.0f Hz", samplingRate), 10, 50);
        }

        public String getDacCSV() {
            if (dacOutput == null) return "Time (s),Analog Amplitude,Quantized Amplitude\n";
            StringBuilder csv = new StringBuilder("Time (s),Analog Amplitude,Quantized Amplitude\n");
            double samplingRate = samplingRateSupplier.get();
            for (int i = 0; i < dacOutput.length; i++) {
                double t = i / samplingRate;
                csv.append(String.format("%.6f,%.6f,%.6f\n", t, generator.calculateWaveform(t), dacOutput[i]));
            }
            return csv.toString();
        }
    }
}