package simulation.software.codebase;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TotalHarmonicDistortion extends JFrame {
    private SweepGenerator generator;
    private ThdPanel thdPanel;
    private JSlider windowSizeSlider;
    private JSlider harmonicCountSlider;
    private JLabel thdLabel;
    private JLabel fundamentalLabel;
    private JButton exportButton;

    public TotalHarmonicDistortion(SweepGenerator generator) {
        this.generator = generator;
        setTitle("Total Harmonic Distortion Analyzer");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Set Bahnschrift font
        Font bahnschrift = new Font("Bahnschrift", Font.PLAIN, 12);
        UIManager.put("Label.font", bahnschrift);
        UIManager.put("Button.font", bahnschrift);
        UIManager.put("Slider.font", bahnschrift);

        thdPanel = new ThdPanel(generator, () -> windowSizeSlider.getValue(), () -> harmonicCountSlider.getValue());
        add(thdPanel, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // THD and Fundamental Labels
        thdLabel = new JLabel("THD: N/A");
        fundamentalLabel = new JLabel("Fundamental: N/A");
        controlPanel.add(thdLabel, gbc);
        gbc.gridx = 1;
        controlPanel.add(fundamentalLabel, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;

        // Window Size Slider
        JLabel windowSizeLabel = new JLabel("Window Size (ms):");
        controlPanel.add(windowSizeLabel, gbc);
        gbc.gridy++;
        windowSizeSlider = new JSlider(JSlider.HORIZONTAL, 5, 50, 20);
        windowSizeSlider.setMajorTickSpacing(15);
        windowSizeSlider.setMinorTickSpacing(5);
        windowSizeSlider.setPaintTicks(true);
        windowSizeSlider.setPaintLabels(true);
        windowSizeSlider.addChangeListener(e -> thdPanel.repaint());
        controlPanel.add(windowSizeSlider, gbc);
        gbc.gridy++;

        // Harmonic Count Slider
        JLabel harmonicCountLabel = new JLabel("Harmonic Count:");
        controlPanel.add(harmonicCountLabel, gbc);
        gbc.gridy++;
        harmonicCountSlider = new JSlider(JSlider.HORIZONTAL, 2, 10, 5);
        harmonicCountSlider.setMajorTickSpacing(2);
        harmonicCountSlider.setMinorTickSpacing(1);
        harmonicCountSlider.setPaintTicks(true);
        harmonicCountSlider.setPaintLabels(true);
        harmonicCountSlider.setSnapToTicks(true);
        harmonicCountSlider.addChangeListener(e -> thdPanel.repaint());
        controlPanel.add(harmonicCountSlider, gbc);
        gbc.gridy++;

        // Export Button
        exportButton = new JButton("Export THD CSV");
        controlPanel.add(exportButton, gbc);

        add(controlPanel, BorderLayout.SOUTH);

        exportButton.addActionListener(e -> {
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

    private class ThdPanel extends JPanel {
        private SweepGenerator generator;
        private java.util.function.Supplier<Integer> windowSizeSupplier;
        private java.util.function.Supplier<Integer> harmonicCountSupplier;
        private static final int SAMPLE_RATE = 44100;
        private static final int MAX_FREQ = 10000;
        private double lastTHD;
        private double lastFundamentalFreq;
        private double[] lastHarmonicAmplitudes;

        public ThdPanel(SweepGenerator generator,
                        java.util.function.Supplier<Integer> windowSizeSupplier,
                        java.util.function.Supplier<Integer> harmonicCountSupplier) {
            this.generator = generator;
            this.windowSizeSupplier = windowSizeSupplier;
            this.harmonicCountSupplier = harmonicCountSupplier;
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
            for (int i = 0; i < width; i += width / 10) {
                g2d.drawLine(i, 0, i, height);
            }

            if (!generator.isRunning()) {
                thdLabel.setText("THD: N/A");
                fundamentalLabel.setText("Fundamental: N/A");
                return;
            }

            // Get parameters
            double windowSize = windowSizeSupplier.get() / 1000.0; // ms to s
            int harmonicCount = harmonicCountSupplier.get();
            int windowSamples = (int) (windowSize * SAMPLE_RATE);

            // Generate waveform
            double[] waveform = generator.getWaveformSamples(windowSamples, SAMPLE_RATE, windowSize);

            // Compute FFT (reusing SpectrumAnalyzerWindow's FFT logic)
            double[] fftMagnitude = computeFFTMagnitude(waveform);
            if (fftMagnitude.length == 0) {
                thdLabel.setText("THD: N/A");
                fundamentalLabel.setText("Fundamental: N/A");
                return;
            }

            // Frequency resolution
            double frequencyResolution = SAMPLE_RATE / (double) fftMagnitude.length / 2;

            // Find fundamental frequency
            int fundamentalIndex = 0;
            double maxMagnitude = 0;
            for (int i = 1; i < fftMagnitude.length; i++) {
                if (fftMagnitude[i] > maxMagnitude) {
                    maxMagnitude = fftMagnitude[i];
                    fundamentalIndex = i;
                }
            }
            double fundamentalFreq = fundamentalIndex * frequencyResolution;

            // Compute harmonic amplitudes
            double fundamentalPower = fftMagnitude[fundamentalIndex] * fftMagnitude[fundamentalIndex];
            double harmonicPower = 0;
            lastHarmonicAmplitudes = new double[harmonicCount];
            int[] harmonicIndices = new int[harmonicCount];
            for (int n = 2; n <= harmonicCount + 1; n++) {
                int harmonicIndex = n * fundamentalIndex;
                if (harmonicIndex < fftMagnitude.length) {
                    lastHarmonicAmplitudes[n - 2] = fftMagnitude[harmonicIndex];
                    harmonicPower += fftMagnitude[harmonicIndex] * fftMagnitude[harmonicIndex];
                    harmonicIndices[n - 2] = harmonicIndex;
                }
            }

            // Compute THD
            double thd = fundamentalPower > 0 ? Math.sqrt(harmonicPower / fundamentalPower) * 100 : 0;
            lastTHD = thd;
            lastFundamentalFreq = fundamentalFreq;

            // Update labels
            thdLabel.setText(String.format("THD: %.2f%%", thd));
            fundamentalLabel.setText(String.format("Fundamental: %.1f Hz", fundamentalFreq));

            // Draw spectrum (yellow)
            g2d.setColor(Color.YELLOW);
            double maxMag = 0;
            for (double mag : fftMagnitude) {
                if (mag > maxMag) maxMag = mag;
            }
            for (int i = 0; i < fftMagnitude.length; i++) {
                double freq = i * frequencyResolution;
                if (freq > MAX_FREQ) break;
                int x = (int) (freq / MAX_FREQ * width);
                int y = (int) ((1 - fftMagnitude[i] / maxMag) * height);
                if (i > 0) {
                    int xPrev = (int) ((i - 1) * frequencyResolution / MAX_FREQ * width);
                    int yPrev = (int) ((1 - fftMagnitude[i - 1] / maxMag) * height);
                    g2d.drawLine(xPrev, yPrev, x, y);
                }
            }

            // Draw harmonics (red)
            g2d.setColor(Color.RED);
            for (int idx : harmonicIndices) {
                if (idx < fftMagnitude.length) {
                    double freq = idx * frequencyResolution;
                    int x = (int) (freq / MAX_FREQ * width);
                    int y = (int) ((1 - fftMagnitude[idx] / maxMag) * height);
                    g2d.drawLine(x, y, x, height);
                }
            }
            g2d.drawLine((int) (fundamentalFreq / MAX_FREQ * width), (int) ((1 - fftMagnitude[fundamentalIndex] / maxMag) * height),
                         (int) (fundamentalFreq / MAX_FREQ * width), height);

            // Draw axes labels
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Bahnschrift", Font.PLAIN, 12));
            g2d.drawString("Frequency (Hz)", width - 100, height - 10);
            g2d.drawString("Magnitude", 10, 20);
        }

        private double[] computeFFTMagnitude(double[] input) {
            // Simplified FFT (based on SpectrumAnalyzerWindow's logic)
            int n = nextPowerOfTwo(input.length);
            double[] padded = new double[n];
            System.arraycopy(input, 0, padded, 0, input.length);

            // Perform FFT (Cooley-Tukey)
            Complex[] fft = fft(padded);
            int outputLength = n / 2;
            double[] magnitude = new double[outputLength];
            for (int i = 0; i < outputLength; i++) {
                magnitude[i] = Math.sqrt(fft[i].real * fft[i].real + fft[i].imag * fft[i].imag);
            }
            return magnitude;
        }

        private Complex[] fft(double[] input) {
            int n = input.length;
            Complex[] x = new Complex[n];
            for (int i = 0; i < n; i++) {
                x[i] = new Complex(input[i], 0);
            }
            if (n <= 1) return x;

            Complex[] result = new Complex[n];
            for (int i = 0; i < n; i++) {
                int j = Integer.reverse(i) >>> (32 - Integer.numberOfLeadingZeros(n - 1));
                result[j] = x[i];
            }

            for (int size = 2; size <= n; size *= 2) {
                int halfSize = size / 2;
                double angle = -2 * Math.PI / size;
                Complex w = new Complex(Math.cos(angle), Math.sin(angle));
                for (int i = 0; i < n; i += size) {
                    Complex wk = new Complex(1, 0);
                    for (int j = 0; j < halfSize; j++) {
                        Complex t = wk.multiply(result[i + j + halfSize]);
                        result[i + j + halfSize] = result[i + j].subtract(t);
                        result[i + j] = result[i + j].add(t);
                        wk = wk.multiply(w);
                    }
                }
            }
            return result;
        }

        private int nextPowerOfTwo(int n) {
            int power = 1;
            while (power < n) power *= 2;
            return power;
        }

        private class Complex {
            double real, imag;
            Complex(double real, double imag) {
                this.real = real;
                this.imag = imag;
            }
            Complex add(Complex other) {
                return new Complex(real + other.real, imag + other.imag);
            }
            Complex subtract(Complex other) {
                return new Complex(real - other.real, imag - other.imag);
            }
            Complex multiply(Complex other) {
                return new Complex(real * other.real - imag * other.imag,
                                   real * other.imag + imag * other.real);
            }
        }

        public String getThdCSV() {
            if (lastHarmonicAmplitudes == null) return "Timestamp,THD,FundamentalFrequency\n";
            StringBuilder csv = new StringBuilder("Timestamp,THD,FundamentalFrequency");
            for (int i = 2; i <= lastHarmonicAmplitudes.length + 1; i++) {
                csv.append(",Harmonic").append(i);
            }
            csv.append("\n");
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            csv.append(String.format("%s,%.6f,%.6f", timestamp, lastTHD, lastFundamentalFreq));
            for (double amp : lastHarmonicAmplitudes) {
                csv.append(String.format(",%.6f", amp));
            }
            csv.append("\n");
            return csv.toString();
        }
    }
}