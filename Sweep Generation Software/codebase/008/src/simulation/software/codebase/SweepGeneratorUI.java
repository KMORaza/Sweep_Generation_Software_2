package simulation.software.codebase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SweepGeneratorUI extends JFrame {
    private SweepGenerator generator;
    private WaveformPanel waveformPanel;
    private JComboBox<String> sweepModeCombo, signalTypeCombo, noiseTypeCombo, modulationTypeCombo;
    private JTextField startFreqField, endFreqField, sweepTimeField, holdTimeField, repeatIntervalField, dutyCycleField;
    private JTextField noiseAmplitudeField, modFreqField, modIndexField;
    private JButton startButton, stopButton, tableButton, arbitraryButton, spectrumButton, exportWaveformButton, dacButton, thdButton, phaseButton, crossCouplingButton, optimizationButton, adaptationButton, multiGeneratorButton, quantumButton, nonLinearButton;
    private JLabel statusLabel;
    private Timer timer;
    private SpectrumAnalyzerWindow spectrumWindow;
    private DigitalToAnalog dacWindow;
    private TotalHarmonicDistortion thdWindow;
    private PhaseAnalyzer phaseWindow;
    private CrossCoupling crossCouplingWindow;
    private AutomatischeOptimierung optimizationWindow;
    private SweepMusterAnpassen adaptationWindow;
    private MehrereSimulierteGeneratoren multiGeneratorWindow;
    private QuantenbewussteSignalsimulation quantumSimulationWindow;
    private NonLinearTransmissionSimulation nonLinearSimulationWindow;

    public SweepGeneratorUI() {
        setTitle("Sweep Generator Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        // Set Bahnschrift font
        Font bahnschrift = new Font("Bahnschrift", Font.PLAIN, 12);
        UIManager.put("Label.font", bahnschrift);
        UIManager.put("Button.font", bahnschrift);
        UIManager.put("TextField.font", bahnschrift);
        UIManager.put("ComboBox.font", bahnschrift);

        // Initialize generator and waveform panel
        generator = new SweepGenerator();
        waveformPanel = new WaveformPanel(generator);

        // Create control panel
        JScrollPane controlScrollPane = createControlPanel();
        controlScrollPane.setBorder(BorderFactory.createTitledBorder("Control Panel"));

        // Status label
        statusLabel = new JLabel("Sweep Mode: None, Signal Type: None");
        statusLabel.setFont(bahnschrift);

        // Layout
        setLayout(new BorderLayout());
        add(controlScrollPane, BorderLayout.WEST);
        add(waveformPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Timer for updating waveform (50ms for smooth updates)
        timer = new Timer(50, e -> {
            waveformPanel.repaint();
            if (spectrumWindow != null && spectrumWindow.isVisible()) {
                spectrumWindow.repaint();
            }
            if (dacWindow != null && dacWindow.isVisible()) {
                dacWindow.repaint();
            }
            if (thdWindow != null && thdWindow.isVisible()) {
                thdWindow.repaint();
            }
            if (phaseWindow != null && phaseWindow.isVisible()) {
                phaseWindow.repaint();
            }
            if (crossCouplingWindow != null && crossCouplingWindow.isVisible()) {
                crossCouplingWindow.repaint();
            }
            if (optimizationWindow != null && optimizationWindow.isVisible()) {
                optimizationWindow.repaint();
            }
            if (adaptationWindow != null && adaptationWindow.isVisible()) {
                adaptationWindow.repaint();
            }
            if (multiGeneratorWindow != null && multiGeneratorWindow.isVisible()) {
                multiGeneratorWindow.repaint();
            }
            if (quantumSimulationWindow != null && quantumSimulationWindow.isVisible()) {
                quantumSimulationWindow.repaint();
            }
            if (nonLinearSimulationWindow != null && nonLinearSimulationWindow.isVisible()) {
                nonLinearSimulationWindow.repaint();
            }
        });
    }

    public SweepGenerator getGenerator() {
        return generator;
    }

    private JScrollPane createControlPanel() {
        JPanel innerPanel = new JPanel(new GridBagLayout());
        innerPanel.setBackground(Color.BLACK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 5, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;

        // Sweep Mode
        JLabel modeLabel = new JLabel("Sweep Mode:");
        modeLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(modeLabel, gbc);

        String[] modes = {"Linear", "Logarithmic", "Glide", "Stepped", "Bidirectional", "Time", "Table"};
        sweepModeCombo = new JComboBox<>(modes);
        gbc.gridx = 1;
        innerPanel.add(sweepModeCombo, gbc);

        // Signal Type
        JLabel signalTypeLabel = new JLabel("Signal Type:");
        signalTypeLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(signalTypeLabel, gbc);

        String[] signalTypes = {"Sine", "Triangle", "Square", "Ramp"};
        signalTypeCombo = new JComboBox<>(signalTypes);
        gbc.gridx = 1;
        innerPanel.add(signalTypeCombo, gbc);

        // Noise Type
        JLabel noiseTypeLabel = new JLabel("Noise Type:");
        noiseTypeLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(noiseTypeLabel, gbc);

        String[] noiseTypes = {"None", "White", "Pink", "Gaussian"};
        noiseTypeCombo = new JComboBox<>(noiseTypes);
        gbc.gridx = 1;
        innerPanel.add(noiseTypeCombo, gbc);

        // Noise Amplitude
        JLabel noiseAmpLabel = new JLabel("Noise Amplitude:");
        noiseAmpLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(noiseAmpLabel, gbc);

        noiseAmplitudeField = new JTextField("0.1", 10);
        gbc.gridx = 1;
        innerPanel.add(noiseAmplitudeField, gbc);

        // Modulation Type
        JLabel modTypeLabel = new JLabel("Modulation Type:");
        modTypeLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(modTypeLabel, gbc);

        String[] modTypes = {"None", "AM", "FM", "PM"};
        modulationTypeCombo = new JComboBox<>(modTypes);
        gbc.gridx = 1;
        innerPanel.add(modulationTypeCombo, gbc);

        // Modulation Frequency
        JLabel modFreqLabel = new JLabel("Modulation Freq (Hz):");
        modFreqLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(modFreqLabel, gbc);

        modFreqField = new JTextField("10", 10);
        gbc.gridx = 1;
        innerPanel.add(modFreqField, gbc);

        // Modulation Index
        JLabel modIndexLabel = new JLabel("Modulation Index:");
        modIndexLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(modIndexLabel, gbc);

        modIndexField = new JTextField("0.5", 10);
        gbc.gridx = 1;
        innerPanel.add(modIndexField, gbc);

        // Start Frequency
        JLabel startFreqLabel = new JLabel("Start Frequency (Hz):");
        startFreqLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(startFreqLabel, gbc);

        startFreqField = new JTextField("100", 10);
        gbc.gridx = 1;
        innerPanel.add(startFreqField, gbc);

        // End Frequency
        JLabel endFreqLabel = new JLabel("End Frequency (Hz):");
        endFreqLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(endFreqLabel, gbc);

        endFreqField = new JTextField("1000", 10);
        gbc.gridx = 1;
        innerPanel.add(endFreqField, gbc);

        // Sweep Time
        JLabel sweepTimeLabel = new JLabel("Sweep Time (s):");
        sweepTimeLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(sweepTimeLabel, gbc);

        sweepTimeField = new JTextField("10", 10);
        gbc.gridx = 1;
        innerPanel.add(sweepTimeField, gbc);

        // Hold Time (for Stepped Sweep)
        JLabel holdTimeLabel = new JLabel("Hold Time per Step (ms):");
        holdTimeLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(holdTimeLabel, gbc);

        holdTimeField = new JTextField("500", 10);
        gbc.gridx = 1;
        innerPanel.add(holdTimeField, gbc);

        // Repetition Interval
        JLabel repeatIntervalLabel = new JLabel("Repetition Interval (s):");
        repeatIntervalLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(repeatIntervalLabel, gbc);

        repeatIntervalField = new JTextField("15", 10);
        gbc.gridx = 1;
        innerPanel.add(repeatIntervalField, gbc);

        // Duty Cycle
        JLabel dutyCycleLabel = new JLabel("Duty Cycle (%):");
        dutyCycleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(dutyCycleLabel, gbc);

        dutyCycleField = new JTextField("80", 10);
        gbc.gridx = 1;
        innerPanel.add(dutyCycleField, gbc);

        // Table Sweep Button
        tableButton = new JButton("Set Table Data");
        gbc.gridx = 0;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        innerPanel.add(tableButton, gbc);

        // Arbitrary Waveform Button
        arbitraryButton = new JButton("Set Arbitrary Waveform");
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(arbitraryButton, gbc);

        // Spectrum Analyzer Button
        spectrumButton = new JButton("Show Spectrum");
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(spectrumButton, gbc);

        // Export Waveform Button
        exportWaveformButton = new JButton("Export Waveform CSV");
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(exportWaveformButton, gbc);

        // DAC Button
        dacButton = new JButton("Show DAC");
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(dacButton, gbc);

        // THD Button
        thdButton = new JButton("Show THD");
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(thdButton, gbc);

        // Phase Button
        phaseButton = new JButton("Show Phase");
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(phaseButton, gbc);

        // Cross-Coupling Button
        crossCouplingButton = new JButton("Show Cross-Coupling");
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(crossCouplingButton, gbc);

        // Optimization Button
        optimizationButton = new JButton("Show Auto-Optimization");
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(optimizationButton, gbc);

        // Adaptation Button
        adaptationButton = new JButton("Show RL Adaptation");
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(adaptationButton, gbc);

        // Multi-Generator Button
        multiGeneratorButton = new JButton("Show Multiple Generators");
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(multiGeneratorButton, gbc);

        // Quantum Simulation Button
        quantumButton = new JButton("Show Quantum Simulation");
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(quantumButton, gbc);

        // Non-Linear Simulation Button
        nonLinearButton = new JButton("Show Non-Linear Simulation");
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(nonLinearButton, gbc);
        gbc.gridwidth = 1;

        // Start and Stop Buttons
        startButton = new JButton("Start Sweep");
        gbc.gridx = 0;
        gbc.gridy = y++;
        innerPanel.add(startButton, gbc);

        stopButton = new JButton("Stop Sweep");
        gbc.gridx = 1;
        innerPanel.add(stopButton, gbc);

        // Create scroll pane
        JScrollPane scrollPane = new JScrollPane(innerPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBackground(Color.BLACK);
        scrollPane.getViewport().setBackground(Color.BLACK);

        // Button actions
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    double startFreq = Double.parseDouble(startFreqField.getText());
                    double endFreq = Double.parseDouble(endFreqField.getText());
                    double sweepTime = Double.parseDouble(sweepTimeField.getText());
                    long holdTime = Long.parseLong(holdTimeField.getText());
                    double repeatInterval = Double.parseDouble(repeatIntervalField.getText());
                    double dutyCycle = Double.parseDouble(dutyCycleField.getText());
                    double noiseAmplitude = Double.parseDouble(noiseAmplitudeField.getText());
                    double modFreq = Double.parseDouble(modFreqField.getText());
                    double modIndex = Double.parseDouble(modIndexField.getText());
                    if (dutyCycle < 0 || dutyCycle > 100) throw new NumberFormatException("Duty cycle must be 0-100");
                    if (noiseAmplitude < 0) throw new NumberFormatException("Noise amplitude must be non-negative");
                    String mode = (String) sweepModeCombo.getSelectedItem();
                    String signalType = (String) signalTypeCombo.getSelectedItem();
                    String noiseType = (String) noiseTypeCombo.getSelectedItem();
                    String modType = (String) modulationTypeCombo.getSelectedItem();
                    if (mode.equals("Table") && (generator.getTableData() == null || generator.getTableData().isEmpty())) {
                        throw new IllegalStateException("Table data not set for Table sweep mode");
                    }
                    if (signalType.equals("Arbitrary") && (generator.getArbitraryData() == null || generator.getArbitraryData().isEmpty())) {
                        throw new IllegalStateException("Arbitrary waveform data not set");
                    }
                    generator.startSweep(startFreq, endFreq, sweepTime, holdTime, repeatInterval, 
                                        dutyCycle / 100.0, noiseAmplitude, modFreq, modIndex, 
                                        mode, signalType, noiseType, modType);
                    statusLabel.setText(String.format("Sweep Mode: %s, Signal Type: %s", mode, signalType));
                    timer.start();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(SweepGeneratorUI.this, "Please enter valid numbers: " + ex.getMessage());
                } catch (IllegalStateException ex) {
                    JOptionPane.showMessageDialog(SweepGeneratorUI.this, ex.getMessage());
                }
            }
        });

        stopButton.addActionListener(e -> {
            generator.stopSweep();
            timer.stop();
            statusLabel.setText("Sweep Mode: None, Signal Type: None");
            waveformPanel.repaint();
            if (spectrumWindow != null) {
                spectrumWindow.repaint();
            }
            if (dacWindow != null) {
                dacWindow.repaint();
            }
            if (thdWindow != null) {
                thdWindow.repaint();
            }
            if (phaseWindow != null) {
                phaseWindow.repaint();
            }
            if (crossCouplingWindow != null) {
                crossCouplingWindow.repaint();
            }
            if (optimizationWindow != null) {
                optimizationWindow.repaint();
            }
            if (adaptationWindow != null) {
                adaptationWindow.repaint();
            }
            if (multiGeneratorWindow != null) {
                multiGeneratorWindow.repaint();
            }
            if (quantumSimulationWindow != null) {
                quantumSimulationWindow.repaint();
            }
            if (nonLinearSimulationWindow != null) {
                nonLinearSimulationWindow.repaint();
            }
        });

        tableButton.addActionListener(e -> {
            TableSweepDialog dialog = new TableSweepDialog(this);
            dialog.setVisible(true);
        });

        arbitraryButton.addActionListener(e -> {
            ArbitraryWaveformDialog dialog = new ArbitraryWaveformDialog(SweepGeneratorUI.this, generator);
            dialog.setVisible(true);
        });

        spectrumButton.addActionListener(e -> {
            if (spectrumWindow == null || !spectrumWindow.isVisible()) {
                spectrumWindow = new SpectrumAnalyzerWindow(generator);
                spectrumWindow.setVisible(true);
            }
        });

        exportWaveformButton.addActionListener(e -> {
            if (!generator.isRunning()) {
                JOptionPane.showMessageDialog(SweepGeneratorUI.this, "Start the sweep to export data.");
                return;
            }
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Waveform CSV");
            fileChooser.setSelectedFile(new File("waveform.csv"));
            if (fileChooser.showSaveDialog(SweepGeneratorUI.this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".csv")) {
                    file = new File(file.getPath() + ".csv");
                }
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(generator.getWaveformCSV(1024, 44100));
                    JOptionPane.showMessageDialog(SweepGeneratorUI.this, "Waveform CSV saved to " + file.getPath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(SweepGeneratorUI.this, "Error saving CSV: " + ex.getMessage());
                }
            }
        });

        dacButton.addActionListener(e -> {
            if (dacWindow == null || !dacWindow.isVisible()) {
                dacWindow = new DigitalToAnalog(generator);
                dacWindow.setVisible(true);
            }
        });

        thdButton.addActionListener(e -> {
            if (thdWindow == null || !thdWindow.isVisible()) {
                thdWindow = new TotalHarmonicDistortion(generator);
                thdWindow.setVisible(true);
            }
        });

        phaseButton.addActionListener(e -> {
            if (phaseWindow == null || !phaseWindow.isVisible()) {
                phaseWindow = new PhaseAnalyzer(generator);
                phaseWindow.setVisible(true);
            }
        });

        crossCouplingButton.addActionListener(e -> {
            if (crossCouplingWindow == null || !crossCouplingWindow.isVisible()) {
                crossCouplingWindow = new CrossCoupling(generator);
                crossCouplingWindow.setVisible(true);
            }
        });

        optimizationButton.addActionListener(e -> {
            if (optimizationWindow == null || !optimizationWindow.isVisible()) {
                optimizationWindow = new AutomatischeOptimierung(generator);
                optimizationWindow.setVisible(true);
            }
        });

        adaptationButton.addActionListener(e -> {
            if (adaptationWindow == null || !adaptationWindow.isVisible()) {
                adaptationWindow = new SweepMusterAnpassen(generator);
                adaptationWindow.setVisible(true);
            }
        });

        multiGeneratorButton.addActionListener(e -> {
            if (multiGeneratorWindow == null || !multiGeneratorWindow.isVisible()) {
                multiGeneratorWindow = new MehrereSimulierteGeneratoren();
                multiGeneratorWindow.setVisible(true);
            }
        });

        quantumButton.addActionListener(e -> {
            if (quantumSimulationWindow == null || !quantumSimulationWindow.isVisible()) {
                quantumSimulationWindow = new QuantenbewussteSignalsimulation();
                quantumSimulationWindow.setVisible(true);
            }
        });

        nonLinearButton.addActionListener(e -> {
            if (nonLinearSimulationWindow == null || !nonLinearSimulationWindow.isVisible()) {
                nonLinearSimulationWindow = new NonLinearTransmissionSimulation();
                nonLinearSimulationWindow.setVisible(true);
            }
        });

        return scrollPane;
    }
}