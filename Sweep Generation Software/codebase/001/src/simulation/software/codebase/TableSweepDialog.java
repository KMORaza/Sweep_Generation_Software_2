package simulation.software.codebase;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TableSweepDialog extends JDialog {
    private SweepGenerator generator;
    private JTextArea tableInput;
    private boolean confirmed;

    public TableSweepDialog(Frame parent, SweepGenerator generator) {
        super(parent, "Table Sweep Input", true);
        this.generator = generator;
        setSize(400, 300);
        setLocationRelativeTo(parent);
        confirmed = false;

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(Color.BLACK);

        JLabel infoLabel = new JLabel("<html>Enter frequency (10–10000 Hz) and amplitude (0.1–5.0 V) pairs, one per line, separated by commas.<br>Example: 100,1.0</html>");
        infoLabel.setForeground(Color.WHITE);
        infoLabel.setFont(new Font("Bahnschrift", Font.PLAIN, 12));
        panel.add(infoLabel, BorderLayout.NORTH);

        tableInput = new JTextArea(10, 30);
        tableInput.setBackground(Color.BLACK);
        tableInput.setForeground(Color.WHITE);
        tableInput.setFont(new Font("Bahnschrift", Font.PLAIN, 12));
        tableInput.setCaretColor(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(tableInput);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.BLACK);
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        okButton.setBackground(Color.BLACK);
        okButton.setForeground(Color.WHITE);
        cancelButton.setBackground(Color.BLACK);
        cancelButton.setForeground(Color.WHITE);
        okButton.setFont(new Font("Bahnschrift", Font.PLAIN, 12));
        cancelButton.setFont(new Font("Bahnschrift", Font.PLAIN, 12));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        okButton.addActionListener(e -> {
            List<double[]> tableData = validateInput();
            if (tableData != null) {
                generator.setTableData(tableData);
                confirmed = true;
                setVisible(false);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid input. Ensure frequency is 10–10000 Hz, amplitude is 0.1–5.0 V, and format is correct.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelButton.addActionListener(e -> setVisible(false));

        setContentPane(panel);
    }

    private List<double[]> validateInput() {
        List<double[]> tableData = new ArrayList<>();
        String[] lines = tableInput.getText().split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            try {
                String[] parts = line.split(",");
                if (parts.length != 2) continue;
                double freq = Double.parseDouble(parts[0].trim());
                double amp = Double.parseDouble(parts[1].trim());
                if (freq >= 10 && freq <= 10000 && amp >= 0.1 && amp <= 5.0) {
                    tableData.add(new double[]{freq, amp});
                } else {
                    return null;
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (tableData.isEmpty()) {
            tableData.add(new double[]{100, 1.0}); // Default
        }
        return tableData;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}