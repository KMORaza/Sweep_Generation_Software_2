package simulation.software.codebase;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ArbitraryWaveformDialog extends JDialog {
    private SweepGenerator generator;
    private JTextArea waveformInput;
    private JButton saveButton;

    public ArbitraryWaveformDialog(JFrame parent, SweepGenerator generator) {
        super(parent, "Arbitrary Waveform Data", true);
        this.generator = generator;
        setSize(300, 400);
        setLocationRelativeTo(parent);

        // Set Bahnschrift font
        Font bahnschrift = new Font("Bahnschrift", Font.PLAIN, 12);

        setLayout(new BorderLayout());
        JLabel instructionLabel = new JLabel("Enter time,amplitude pairs (one per line, e.g., 0.0,0.5):");
        instructionLabel.setFont(bahnschrift);
        add(instructionLabel, BorderLayout.NORTH);

        waveformInput = new JTextArea(10, 20);
        waveformInput.setFont(bahnschrift);
        add(new JScrollPane(waveformInput), BorderLayout.CENTER);

        saveButton = new JButton("Save Waveform");
        saveButton.setFont(bahnschrift);
        add(saveButton, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> saveWaveformData());
    }

    private void saveWaveformData() {
        List<double[]> waveformData = new ArrayList<>();
        String[] lines = waveformInput.getText().split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",");
            try {
                double time = Double.parseDouble(parts[0].trim());
                double amp = Double.parseDouble(parts[1].trim());
                waveformData.add(new double[]{time, amp});
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid format in line: " + line);
                return;
            }
        }
        if (waveformData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No valid data entered.");
            return;
        }
        generator.setArbitraryData(waveformData);
        dispose();
    }
}