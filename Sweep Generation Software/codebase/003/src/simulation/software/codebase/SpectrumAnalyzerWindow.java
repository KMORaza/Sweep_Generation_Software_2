package simulation.software.codebase;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class SpectrumAnalyzerWindow extends JFrame {
    private SweepGenerator generator;
    private SpectrumPanel spectrumPanel;
    private JToggleButton scaleToggleButton;
    private JButton exportSpectrumButton;
    private boolean isLogScale = false;

    public SpectrumAnalyzerWindow(SweepGenerator generator) {
        this.generator = generator;
        setTitle("Spectrum Analyzer");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Set Bahnschrift font
        Font bahnschrift = new Font("Bahnschrift", Font.PLAIN, 12);
        UIManager.put("Label.font", bahnschrift);
        UIManager.put("Button.font", bahnschrift);
        UIManager.put("ToggleButton.font", bahnschrift);

        spectrumPanel = new SpectrumPanel(generator, () -> isLogScale);
        add(spectrumPanel, BorderLayout.CENTER);

        // Control panel for buttons
        JPanel controlPanel = new JPanel(new FlowLayout());
        scaleToggleButton = new JToggleButton("Log Scale", false);
        exportSpectrumButton = new JButton("Export Spectrum CSV");

        scaleToggleButton.addActionListener(e -> {
            isLogScale = scaleToggleButton.isSelected();
            scaleToggleButton.setText(isLogScale ? "Linear Scale" : "Log Scale");
            spectrumPanel.repaint();
        });

        exportSpectrumButton.addActionListener(e -> {
            if (!generator.isRunning()) {
                JOptionPane.showMessageDialog(this, "Start the sweep to export data.");
                return;
            }
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Spectrum CSV");
            fileChooser.setSelectedFile(new File("spectrum.csv"));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".csv")) {
                    file = new File(file.getPath() + ".csv");
                }
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(spectrumPanel.getSpectrumCSV());
                    JOptionPane.showMessageDialog(this, "Spectrum CSV saved to " + file.getPath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving CSV: " + ex.getMessage());
                }
            }
        });

        controlPanel.add(scaleToggleButton);
        controlPanel.add(exportSpectrumButton);
        add(controlPanel, BorderLayout.SOUTH);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (spectrumPanel != null) {
            spectrumPanel.repaint();
        }
    }

    private static class SpectrumPanel extends JPanel {
        private SweepGenerator generator;
        private java.util.function.Supplier<Boolean> isLogScaleSupplier;
        private static final int FFT_SIZE = 1024;
        private static final double SAMPLE_RATE = 44100; // Hz

        public SpectrumPanel(SweepGenerator generator, java.util.function.Supplier<Boolean> isLogScaleSupplier) {
            this.generator = generator;
            this.isLogScaleSupplier = isLogScaleSupplier;
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(600, 400));
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

            // Draw spectrum
            if (generator.isRunning()) {
                double[] samples = generator.getWaveformSamples(FFT_SIZE, SAMPLE_RATE);
                double[] spectrum = computeFFT(samples);
                g2d.setColor(Color.GREEN);

                int width = getWidth();
                int height = getHeight();
                double maxFreq = SAMPLE_RATE / 2; // Nyquist frequency
                double maxMagnitude = 0;
                for (double mag : spectrum) {
                    if (mag > maxMagnitude) maxMagnitude = mag;
                }
                if (maxMagnitude == 0) maxMagnitude = 1;

                boolean isLogScale = isLogScaleSupplier.get();
                double[] freqPoints = new double[spectrum.length / 2];
                for (int i = 0; i < spectrum.length / 2; i++) {
                    double freq = i * maxFreq / (spectrum.length / 2);
                    freqPoints[i] = isLogScale ? Math.log10(freq + 1) / Math.log10(maxFreq + 1) : freq / maxFreq;
                }

                for (int i = 0; i < spectrum.length / 2 - 1; i++) {
                    double x1 = freqPoints[i] * width;
                    double x2 = freqPoints[i + 1] * width;
                    double mag1 = spectrum[i];
                    double mag2 = spectrum[i + 1];
                    int y1 = (int) (height - (mag1 / maxMagnitude) * height * 0.9);
                    int y2 = (int) (height - (mag2 / maxMagnitude) * height * 0.9);
                    g2d.drawLine((int) x1, y1, (int) x2, y2);
                }

                // Draw frequency labels
                g2d.setFont(new Font("Bahnschrift", Font.BOLD, 12));
                g2d.setColor(Color.WHITE);
                for (int i = 0; i <= 5; i++) {
                    double freq = isLogScale ? Math.pow(10, i * Math.log10(maxFreq) / 5) : i * maxFreq / 5;
                    double xNorm = isLogScale ? (Math.log10(freq + 1) / Math.log10(maxFreq + 1)) : i / 5.0;
                    int x = (int) (xNorm * width);
                    g2d.drawString(String.format("%.0f Hz", freq), x, height - 10);
                }
            }
        }

        public String getSpectrumCSV() {
            double[] samples = generator.getWaveformSamples(FFT_SIZE, SAMPLE_RATE);
            double[] spectrum = computeFFT(samples);
            double maxFreq = SAMPLE_RATE / 2;
            StringBuilder csv = new StringBuilder("Frequency (Hz),Magnitude\n");
            for (int i = 0; i < spectrum.length / 2; i++) {
                double freq = i * maxFreq / (spectrum.length / 2);
                csv.append(String.format("%.2f,%.6f\n", freq, spectrum[i]));
            }
            return csv.toString();
        }

        private double[] computeFFT(double[] input) {
            int n = input.length;
            Complex[] x = new Complex[n];
            for (int i = 0; i < n; i++) {
                x[i] = new Complex(input[i], 0);
            }
            Complex[] y = fft(x);
            double[] magnitude = new double[n / 2];
            for (int i = 0; i < n / 2; i++) {
                magnitude[i] = Math.sqrt(y[i].re * y[i].re + y[i].im * y[i].im);
            }
            return magnitude;
        }

        private Complex[] fft(Complex[] x) {
            int n = x.length;
            if (n <= 1) return x;
            if ((n & (n - 1)) != 0) {
                x = Arrays.copyOf(x, Integer.highestOneBit(n - 1) << 1);
                n = x.length;
            }

            Complex[] even = new Complex[n / 2];
            Complex[] odd = new Complex[n / 2];
            for (int k = 0; k < n / 2; k++) {
                even[k] = x[2 * k];
                odd[k] = x[2 * k + 1];
            }

            Complex[] q = fft(even);
            Complex[] r = fft(odd);
            Complex[] y = new Complex[n];

            for (int k = 0; k < n / 2; k++) {
                double kth = -2 * k * Math.PI / n;
                Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
                y[k] = q[k].plus(wk.times(r[k]));
                y[k + n / 2] = q[k].minus(wk.times(r[k]));
            }
            return y;
        }

        private static class Complex {
            private final double re;
            private final double im;

            public Complex(double real, double imag) {
                this.re = real;
                this.im = imag;
            }

            public Complex plus(Complex b) {
                return new Complex(this.re + b.re, this.im + b.im);
            }

            public Complex minus(Complex b) {
                return new Complex(this.re - b.re, this.im - b.im);
            }

            public Complex times(Complex b) {
                return new Complex(this.re * b.re - this.im * b.im, 
                                  this.re * b.im + this.im * b.re);
            }
        }
    }
}