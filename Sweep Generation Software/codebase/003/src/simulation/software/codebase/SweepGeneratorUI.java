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
    private JButton startButton, stopButton, tableButton, arbitraryButton, spectrumButton, exportWaveformButton, dacButton, thdButton, phaseButton, crossCouplingButton, optimizationButton;
    private JLabel statusLabel;
    private Timer timer;
    private SpectrumAnalyzerWindow spectrumWindow;
    private DigitalToAnalog dacWindow;
    private TotalHarmonicDistortion thdWindow;
    private PhaseAnalyzer phaseWindow;
    private CrossCoupling crossCouplingWindow;
    private AutomatischeOptimierung optimizationWindow;

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
        JPanel controlPanel = createControlPanel();
        controlPanel.setBorder(BorderFactory.createTitledBorder("Control Panel"));

        // Status label
        statusLabel = new JLabel("Sweep Mode: None, Signal Type: None");
        statusLabel.setFont(bahnschrift);

        // Layout
        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.WEST);
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
        });
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;

        // Sweep Mode
        JLabel modeLabel = new JLabel("Sweep Mode:");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(modeLabel, gbc);

        String[] modes = {"Linear", "Logarithmic", "Glide", "Stepped", "Bidirectional", "Time", "Table"};
        sweepModeCombo = new JComboBox<>(modes);
        gbc.gridx = 1;
        panel.add(sweepModeCombo, gbc);

        // Signal Type
        JLabel signalTypeLabel = new JLabel("Signal Type:");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(signalTypeLabel, gbc);

        String[] signalTypes = {"Sine", "Triangle", "Square", "Ramp", "Arbitrary", "Noise"};
        signalTypeCombo = new JComboBox<>(signalTypes);
        gbc.gridx = 1;
        panel.add(signalTypeCombo, gbc);

        // Noise Type
        JLabel noiseTypeLabel = new JLabel("Noise Type:");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(noiseTypeLabel, gbc);

        String[] noiseTypes = {"None", "White", "Pink", "Gaussian"};
        noiseTypeCombo = new JComboBox<>(noiseTypes);
        gbc.gridx = 1;
        panel.add(noiseTypeCombo, gbc);

        // Noise Amplitude
        JLabel noiseAmpLabel = new JLabel("Noise Amplitude:");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(noiseAmpLabel, gbc);

        noiseAmplitudeField = new JTextField("0.1", 10);
        gbc.gridx = 1;
        panel.add(noiseAmplitudeField, gbc);

        // Modulation Type
        JLabel modTypeLabel = new JLabel("Modulation Type:");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(modTypeLabel, gbc);

        String[] modTypes = {"None", "AM", "FM", "PM"};
        modulationTypeCombo = new JComboBox<>(modTypes);
        gbc.gridx = 1;
        panel.add(modulationTypeCombo, gbc);

        // Modulation Frequency
        JLabel modFreqLabel = new JLabel("Modulation Freq (Hz):");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(modFreqLabel, gbc);

        modFreqField = new JTextField("10", 10);
        gbc.gridx = 1;
        panel.add(modFreqField, gbc);

        // Modulation Index
        JLabel modIndexLabel = new JLabel("Modulation Index:");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(modIndexLabel, gbc);

        modIndexField = new JTextField("0.5", 10);
        gbc.gridx = 1;
        panel.add(modIndexField, gbc);

        // Start Frequency
        JLabel startFreqLabel = new JLabel("Start Frequency (Hz):");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(startFreqLabel, gbc);

        startFreqField = new JTextField("100", 10);
        gbc.gridx = 1;
        panel.add(startFreqField, gbc);

        // End Frequency
        JLabel endFreqLabel = new JLabel("End Frequency (Hz):");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(endFreqLabel, gbc);

        endFreqField = new JTextField("1000", 10);
        gbc.gridx = 1;
        panel.add(endFreqField, gbc);

        // Sweep Time
        JLabel sweepTimeLabel = new JLabel("Sweep Time (s):");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(sweepTimeLabel, gbc);

        sweepTimeField = new JTextField("10", 10);
        gbc.gridx = 1;
        panel.add(sweepTimeField, gbc);

        // Hold Time (for Stepped Sweep)
        JLabel holdTimeLabel = new JLabel("Hold Time per Step (ms):");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(holdTimeLabel, gbc);

        holdTimeField = new JTextField("500", 10);
        gbc.gridx = 1;
        panel.add(holdTimeField, gbc);

        // Repetition Interval
        JLabel repeatIntervalLabel = new JLabel("Repetition Interval (s):");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(repeatIntervalLabel, gbc);

        repeatIntervalField = new JTextField("15", 10);
        gbc.gridx = 1;
        panel.add(repeatIntervalField, gbc);

        // Duty Cycle
        JLabel dutyCycleLabel = new JLabel("Duty Cycle (%):");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(dutyCycleLabel, gbc);

        dutyCycleField = new JTextField("80", 10);
        gbc.gridx = 1;
        panel.add(dutyCycleField, gbc);

        // Table Sweep Button
        tableButton = new JButton("Set Table Data");
        gbc.gridx = 0;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        panel.add(tableButton, gbc);

        // Arbitrary Waveform Button
        arbitraryButton = new JButton("Set Arbitrary Waveform");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(arbitraryButton, gbc);

        // Spectrum Analyzer Button
        spectrumButton = new JButton("Show Spectrum");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(spectrumButton, gbc);

        // Export Waveform Button
        exportWaveformButton = new JButton("Export Waveform CSV");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(exportWaveformButton, gbc);

        // DAC Button
        dacButton = new JButton("Show DAC");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(dacButton, gbc);

        // THD Button
        thdButton = new JButton("Show THD");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(thdButton, gbc);

        // Phase Button
        phaseButton = new JButton("Show Phase");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(phaseButton, gbc);

        // Cross-Coupling Button
        crossCouplingButton = new JButton("Show Cross-Coupling");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(crossCouplingButton, gbc);

        // Optimization Button
        optimizationButton = new JButton("Show Auto-Optimization");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(optimizationButton, gbc);
        gbc.gridwidth = 1;

        // Start and Stop Buttons
        startButton = new JButton("Start Sweep");
        gbc.gridx = 0;
        gbc.gridy = y++;
        panel.add(startButton, gbc);

        stopButton = new JButton("Stop Sweep");
        gbc.gridx = 1;
        panel.add(stopButton, gbc);

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

        return panel;
    }
}