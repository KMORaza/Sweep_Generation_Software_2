package simulation.software.codebase;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MehrereSimulierteGeneratoren extends JFrame {
    private GeneratorNetwork network;
    private GeneratorNetworkPanel networkPanel;
    private JLabel statusLabel;
    private JSpinner numGeneratorsSpinner;
    private JTextField startFreqField, endFreqField, sweepTimeField;
    private JTable offsetTable;
    private JButton startButton, stopButton, exportButton;
    private static final int SAMPLE_RATE = 44100;
    private static final int MAX_GENERATORS = 10;

    public MehrereSimulierteGeneratoren() {
        setTitle("Multiple Simulated Generators");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Set Bahnschrift font
        Font bahnschrift = new Font("Bahnschrift", Font.PLAIN, 12);
        UIManager.put("Label.font", bahnschrift);
        UIManager.put("Button.font", bahnschrift);
        UIManager.put("TextField.font", bahnschrift);
        UIManager.put("Spinner.font", bahnschrift);
        UIManager.put("Table.font", bahnschrift);

        network = new GeneratorNetwork();
        networkPanel = new GeneratorNetworkPanel(network);
        add(networkPanel, BorderLayout.CENTER);

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

        // Number of Generators
        JLabel numGenLabel = new JLabel("Number of Generators:");
        controlPanel.add(numGenLabel, gbc);
        gbc.gridx = 1;
        numGeneratorsSpinner = new JSpinner(new SpinnerNumberModel(2, 1, MAX_GENERATORS, 1));
        controlPanel.add(numGeneratorsSpinner, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Start Frequency
        JLabel startFreqLabel = new JLabel("Start Frequency (Hz):");
        controlPanel.add(startFreqLabel, gbc);
        gbc.gridx = 1;
        startFreqField = new JTextField("100", 10);
        controlPanel.add(startFreqField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // End Frequency
        JLabel endFreqLabel = new JLabel("End Frequency (Hz):");
        controlPanel.add(endFreqLabel, gbc);
        gbc.gridx = 1;
        endFreqField = new JTextField("1000", 10);
        controlPanel.add(endFreqField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Sweep Time
        JLabel sweepTimeLabel = new JLabel("Sweep Time (s):");
        controlPanel.add(sweepTimeLabel, gbc);
        gbc.gridx = 1;
        sweepTimeField = new JTextField("10", 10);
        controlPanel.add(sweepTimeField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Offset Table
        JLabel offsetLabel = new JLabel("Phase/Time Offsets:");
        controlPanel.add(offsetLabel, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        offsetTable = new JTable(new DefaultTableModel(new Object[]{"Generator", "Phase Offset (deg)", "Time Delay (ms)"}, 0));
        JScrollPane tableScroll = new JScrollPane(offsetTable);
        tableScroll.setPreferredSize(new Dimension(300, 100));
        controlPanel.add(tableScroll, gbc);
        numGeneratorsSpinner.addChangeListener(e -> updateOffsetTable());

        // Start/Stop Buttons
        gbc.gridy++;
        startButton = new JButton("Start Generators");
        gbc.gridwidth = 1;
        controlPanel.add(startButton, gbc);
        gbc.gridx = 1;
        stopButton = new JButton("Stop Generators");
        controlPanel.add(stopButton, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;

        // Export Button
        exportButton = new JButton("Export CSV");
        controlPanel.add(exportButton, gbc);

        add(controlPanel, BorderLayout.SOUTH);

        startButton.addActionListener(e -> {
            try {
                int numGen = (Integer) numGeneratorsSpinner.getValue();
                double startFreq = Double.parseDouble(startFreqField.getText());
                double endFreq = Double.parseDouble(endFreqField.getText());
                double sweepTime = Double.parseDouble(sweepTimeField.getText());
                if (startFreq < 0 || endFreq <= startFreq || sweepTime <= 0) {
                    throw new NumberFormatException("Invalid frequency or time parameters");
                }
                List<Double> phaseOffsets = new ArrayList<>();
                List<Double> timeOffsets = new ArrayList<>();
                DefaultTableModel model = (DefaultTableModel) offsetTable.getModel();
                for (int i = 0; i < numGen; i++) {
                    double phase = Double.parseDouble(model.getValueAt(i, 1).toString());
                    double time = Double.parseDouble(model.getValueAt(i, 2).toString());
                    if (phase < 0 || phase > 360 || time < 0 || time > 1000) {
                        throw new IllegalArgumentException("Invalid offsets for generator " + (i + 1));
                    }
                    phaseOffsets.add(phase);
                    timeOffsets.add(time);
                }
                network.startGenerators(numGen, startFreq, endFreq, sweepTime, phaseOffsets, timeOffsets);
                statusLabel.setText("Status: Running " + numGen + " generators");
                networkPanel.repaint();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error starting generators: " + ex.getMessage());
            }
        });

        stopButton.addActionListener(e -> {
            network.stopGenerators();
            statusLabel.setText("Status: Idle");
            networkPanel.repaint();
        });

        exportButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Waveform CSV");
            fileChooser.setSelectedFile(new File("multi_waveform.csv"));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".csv")) {
                    file = new File(file.getPath() + ".csv");
                }
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(network.getMultiWaveformCSV(1024, SAMPLE_RATE));
                    JOptionPane.showMessageDialog(this, "CSV saved to " + file.getPath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving CSV: " + ex.getMessage());
                }
            }
        });

        updateOffsetTable();
    }

    private void updateOffsetTable() {
        int numGen = (Integer) numGeneratorsSpinner.getValue();
        DefaultTableModel model = (DefaultTableModel) offsetTable.getModel();
        model.setRowCount(0);
        for (int i = 0; i < numGen; i++) {
            model.addRow(new Object[]{"Generator " + (i + 1), "0.0", "0.0"});
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (networkPanel != null) {
            networkPanel.repaint();
        }
    }

    private class GeneratorNetwork {
        private List<SweepGenerator> generators;
        private List<Double> phaseOffsets;
        private List<Double> timeOffsets;
        private boolean running;

        public GeneratorNetwork() {
            generators = new ArrayList<>();
            phaseOffsets = new ArrayList<>();
            timeOffsets = new ArrayList<>();
            running = false;
        }

        public void startGenerators(int numGenerators, double startFreq, double endFreq, double sweepTime,
                                    List<Double> phaseOffsets, List<Double> timeOffsets) {
            stopGenerators();
            generators.clear();
            this.phaseOffsets = phaseOffsets;
            this.timeOffsets = timeOffsets;
            for (int i = 0; i < numGenerators; i++) {
                SweepGenerator gen = new SweepGenerator();
                gen.startSweep(startFreq, endFreq, sweepTime, 500, 15, 0.8, 0, 10, 0.5,
                               "Linear", "Sine", "None", "None");
                generators.add(gen);
            }
            running = true;
        }

        public void stopGenerators() {
            for (SweepGenerator gen : generators) {
                gen.stopSweep();
            }
            running = false;
        }

        public boolean isRunning() {
            return running;
        }

        public double[] getWaveformSamples(int sampleCount, int sampleRate, double windowSize, int generatorIndex) {
            if (generatorIndex < 0 || generatorIndex >= generators.size()) {
                return new double[sampleCount];
            }
            SweepGenerator gen = generators.get(generatorIndex);
            double phaseOffset = Math.toRadians(phaseOffsets.get(generatorIndex));
            double timeOffset = timeOffsets.get(generatorIndex) / 1000.0; // ms to seconds
            double[] samples = gen.getWaveformSamples(sampleCount, sampleRate, windowSize);
            double[] adjustedSamples = new double[sampleCount];
            double timeStep = windowSize / sampleCount;
            for (int i = 0; i < sampleCount; i++) {
                double time = i * timeStep - timeOffset;
                if (time < 0) {
                    adjustedSamples[i] = 0;
                } else {
                    int sampleIdx = (int) (time / timeStep);
                    if (sampleIdx < sampleCount) {
                        adjustedSamples[i] = samples[sampleIdx] * Math.cos(phaseOffset);
                    }
                }
            }
            return adjustedSamples;
        }

        public int getGeneratorCount() {
            return generators.size();
        }

        public String getMultiWaveformCSV(int sampleCount, int sampleRate) {
            StringBuilder csv = new StringBuilder("Time (s)");
            for (int i = 0; i < generators.size(); i++) {
                csv.append(",Generator ").append(i + 1);
            }
            csv.append("\n");
            double windowSize = 0.02; // 20ms
            double timeStep = windowSize / sampleCount;
            for (int i = 0; i < sampleCount; i++) {
                double time = i * timeStep;
                csv.append(String.format("%.6f", time));
                for (int j = 0; j < generators.size(); j++) {
                    double[] samples = getWaveformSamples(sampleCount, sampleRate, windowSize, j);
                    csv.append(String.format(",%.6f", samples[i]));
                }
                csv.append("\n");
            }
            return csv.toString();
        }
    }

    private class GeneratorNetworkPanel extends JPanel {
        private GeneratorNetwork network;
        private Color[] colors = {Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.GREEN, Color.ORANGE,
                                  Color.RED, Color.BLUE, Color.PINK, Color.WHITE, Color.LIGHT_GRAY};

        public GeneratorNetworkPanel(GeneratorNetwork network) {
            this.network = network;
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

            // Draw waveforms
            if (network.isRunning()) {
                int sampleCount = 1024;
                double windowSize = 0.02; // 20ms
                for (int genIdx = 0; genIdx < network.getGeneratorCount(); genIdx++) {
                    double[] samples = network.getWaveformSamples(sampleCount, SAMPLE_RATE, windowSize, genIdx);
                    g2d.setColor(colors[genIdx % colors.length]);
                    for (int i = 1; i < sampleCount; i++) {
                        int x1 = (i - 1) * width / sampleCount;
                        int x2 = i * width / sampleCount;
                        int y1 = (int) (midY - samples[i - 1] * (height / 2 - 50));
                        int y2 = (int) (midY - samples[i] * (height / 2 - 50));
                        g2d.drawLine(x1, y1, x2, y2);
                    }
                }
            }

            // Draw axes labels
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Bahnschrift", Font.PLAIN, 12));
            g2d.drawString("Time", width / 2 - 20, height - 10);
            g2d.rotate(-Math.PI / 2);
            g2d.drawString("Amplitude", -height / 2 - 20, 20);
            g2d.rotate(Math.PI / 2);

            // Draw legend
            for (int i = 0; i < network.getGeneratorCount(); i++) {
                g2d.setColor(colors[i % colors.length]);
                g2d.drawString("Gen " + (i + 1), 10, 20 + i * 15);
            }
        }
    }
}