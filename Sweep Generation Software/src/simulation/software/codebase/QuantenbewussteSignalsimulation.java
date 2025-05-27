package simulation.software.codebase;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class QuantenbewussteSignalsimulation extends JFrame {
    private SweepGenerator generator;
    private QuantumWaveformPanel waveformPanel;
    private JComboBox<String> modulationModeCombo;
    private JTextField startFreqField, endFreqField, sweepTimeField, t1Field, t2Field;
    private JButton startButton, stopButton, exportButton;
    private JLabel statusLabel;
    private Timer timer;
    private static final int SAMPLE_RATE = 44100;
    private static final int SAMPLE_COUNT = 1024;
    private static final double WINDOW_SIZE = 0.02; // 20ms
    private double fidelity = 1.0;
    private boolean running = false;

    public QuantenbewussteSignalsimulation() {
        setTitle("Quantum-Aware Signal Simulation");
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
        waveformPanel = new QuantumWaveformPanel();
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
        statusLabel = new JLabel("Status: Idle, Fidelity: 1.00");
        statusLabel.setForeground(Color.WHITE);
        gbc.gridwidth = 2;
        controlPanel.add(statusLabel, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;

        // Modulation Mode
        JLabel modModeLabel = new JLabel("Modulation Mode:");
        modModeLabel.setForeground(Color.WHITE);
        controlPanel.add(modModeLabel, gbc);
        gbc.gridx = 1;
        String[] modModes = {"BPSK", "QPSK", "Entanglement-Inspired"};
        modulationModeCombo = new JComboBox<>(modModes);
        controlPanel.add(modulationModeCombo, gbc);
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

        // T1 (Thermal Relaxation)
        JLabel t1Label = new JLabel("T1 (µs):");
        t1Label.setForeground(Color.WHITE);
        controlPanel.add(t1Label, gbc);
        gbc.gridx = 1;
        t1Field = new JTextField("50", 10);
        controlPanel.add(t1Field, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // T2 (Decoherence)
        JLabel t2Label = new JLabel("T2 (µs):");
        t2Label.setForeground(Color.WHITE);
        controlPanel.add(t2Label, gbc);
        gbc.gridx = 1;
        t2Field = new JTextField("30", 10);
        controlPanel.add(t2Field, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Start/Stop Buttons
        startButton = new JButton("Start Quantum Sweep");
        gbc.gridwidth = 1;
        controlPanel.add(startButton, gbc);
        gbc.gridx = 1;
        stopButton = new JButton("Stop Quantum Sweep");
        controlPanel.add(stopButton, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;

        // Export Button
        exportButton = new JButton("Export CSV");
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
                double t1 = Double.parseDouble(t1Field.getText());
                double t2 = Double.parseDouble(t2Field.getText());
                if (startFreq < 0 || endFreq <= startFreq || sweepTime <= 0) {
                    throw new NumberFormatException("Invalid frequency or time parameters");
                }
                if (t1 <= 0 || t2 <= 0 || t2 > 2 * t1) {
                    throw new NumberFormatException("Invalid T1/T2: T1, T2 > 0, T2 ≤ 2*T1");
                }
                String modMode = (String) modulationModeCombo.getSelectedItem();
                generator.startSweep(startFreq, endFreq, sweepTime, 500, 15, 0.8, 0, 10, 0.5,
                                    "Linear", "Sine", "None", "None");
                running = true;
                statusLabel.setText("Status: Running " + modMode + ", Fidelity: " + String.format("%.2f", fidelity));
                timer.start();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error starting quantum sweep: " + ex.getMessage());
            }
        });

        stopButton.addActionListener(e -> {
            generator.stopSweep();
            running = false;
            timer.stop();
            statusLabel.setText("Status: Idle, Fidelity: 1.00");
            waveformPanel.repaint();
        });

        exportButton.addActionListener(e -> {
            if (!running) {
                JOptionPane.showMessageDialog(this, "Start the quantum sweep to export data.");
                return;
            }
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Quantum Waveform CSV");
            fileChooser.setSelectedFile(new File("quantum_waveform.csv"));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".csv")) {
                    file = new File(file.getPath() + ".csv");
                }
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(getQuantumWaveformCSV());
                    JOptionPane.showMessageDialog(this, "CSV saved to " + file.getPath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving CSV: " + ex.getMessage());
                }
            }
        });
    }

    private double[] getQuantumWaveformSamples() {
        double[] idealSamples = generator.getWaveformSamples(SAMPLE_COUNT, SAMPLE_RATE, WINDOW_SIZE);
        double t1 = Double.parseDouble(t1Field.getText()) * 1e-6; // µs to s
        double t2 = Double.parseDouble(t2Field.getText()) * 1e-6; // µs to s
        String modMode = (String) modulationModeCombo.getSelectedItem();
        Random rand = new Random();

        double[] noisySamples = new double[SAMPLE_COUNT];
        double timeStep = WINDOW_SIZE / SAMPLE_COUNT;

        // Apply modulation
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double phase = 0;
            if (modMode.equals("BPSK")) {
                phase = rand.nextBoolean() ? 0 : Math.PI; // 0° or 180°
            } else if (modMode.equals("QPSK")) {
                int idx = rand.nextInt(4);
                phase = idx * Math.PI / 2 + Math.PI / 4; // 45°, 135°, 225°, 315°
            } else if (modMode.equals("Entanglement-Inspired")) {
                // Simulate Bell-like correlation: two virtual qubits with correlated phases
                double qubit1Phase = rand.nextBoolean() ? 0 : Math.PI;
                double qubit2Phase = qubit1Phase; // Perfect correlation
                phase = (qubit1Phase + qubit2Phase) / 2;
            }
            noisySamples[i] = idealSamples[i] * Math.cos(phase);
        }

        // Apply quantum noise (T1: amplitude damping, T2: phase damping)
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double t = i * timeStep;
            // T1: amplitude damping (exponential decay)
            double ampDamping = Math.exp(-t / t1);
            // T2: phase damping (decoherence)
            double phaseDamping = Math.exp(-t / t2);
            noisySamples[i] *= ampDamping * phaseDamping;
        }

        // Compute Hellinger fidelity
        fidelity = computeHellingerFidelity(idealSamples, noisySamples);

        return noisySamples;
    }

    private double computeHellingerFidelity(double[] ideal, double[] noisy) {
        double sum = 0;
        for (int i = 0; i < ideal.length; i++) {
            sum += Math.sqrt(Math.abs(ideal[i]) * Math.abs(noisy[i]));
        }
        sum /= ideal.length;
        return sum * sum; // Hellinger fidelity
    }

    private String getQuantumWaveformCSV() {
        double[] samples = getQuantumWaveformSamples();
        StringBuilder csv = new StringBuilder("Time (s),Amplitude\n");
        double timeStep = WINDOW_SIZE / SAMPLE_COUNT;
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double time = i * timeStep;
            csv.append(String.format("%.6f,%.6f\n", time, samples[i]));
        }
        return csv.toString();
    }

    private class QuantumWaveformPanel extends JPanel {
        public QuantumWaveformPanel() {
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

            // Draw waveform
            if (running) {
                double[] samples = getQuantumWaveformSamples();
                g2d.setColor(Color.CYAN);
                for (int i = 1; i < SAMPLE_COUNT; i++) {
                    int x1 = (i - 1) * width / SAMPLE_COUNT;
                    int x2 = i * width / SAMPLE_COUNT;
                    int y1 = (int) (midY - samples[i - 1] * (height / 2 - 50));
                    int y2 = (int) (midY - samples[i] * (height / 2 - 50));
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }

            // Draw axes labels
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Bahnschrift", Font.PLAIN, 12));
            g2d.drawString("Time", width / 2 - 20, height - 10);
            g2d.rotate(-Math.PI / 2);
            g2d.drawString("Amplitude", -height / 2 - 20, 20);
            g2d.rotate(Math.PI / 2);
        }
    }
}