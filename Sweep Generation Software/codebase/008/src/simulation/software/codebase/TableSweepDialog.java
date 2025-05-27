package simulation.software.codebase;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TableSweepDialog extends JDialog {
    private SweepGeneratorUI parent;
    private SweepGenerator generator;
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private JButton addRowButton, removeRowButton, applyButton, cancelButton;
    private static final int MAX_ROWS = 100;

    public TableSweepDialog(SweepGeneratorUI parent) {
        super(parent, "Set Table Sweep Data", true);
        this.parent = parent;
        this.generator = parent.getGenerator(); // Assumes SweepGeneratorUI has getGenerator()
        setSize(600, 400);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Set Bahnschrift font
        Font bahnschrift = new Font("Bahnschrift", Font.PLAIN, 12);
        UIManager.put("Label.font", bahnschrift);
        UIManager.put("Button.font", bahnschrift);
        UIManager.put("Table.font", bahnschrift);

        // Initialize table
        String[] columnNames = {"Frequency (Hz)", "Amplitude"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        dataTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(dataTable);
        tableScroll.setPreferredSize(new Dimension(500, 250));
        tableScroll.setBackground(Color.BLACK);
        tableScroll.getViewport().setBackground(Color.BLACK);
        dataTable.setBackground(Color.BLACK);
        dataTable.setForeground(Color.WHITE);
        dataTable.setGridColor(Color.DARK_GRAY);

        // Load existing table data if available
        List<double[]> existingData = generator.getTableData();
        if (existingData != null && !existingData.isEmpty()) {
            for (double[] entry : existingData) {
                tableModel.addRow(new Object[]{entry[0], entry[1]});
            }
        } else {
            // Add a few default rows
            tableModel.addRow(new Object[]{100.0, 1.0});
            tableModel.addRow(new Object[]{500.0, 0.8});
            tableModel.addRow(new Object[]{1000.0, 0.6});
        }

        // Control panel
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBackground(Color.BLACK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Instructions
        JLabel instructionsLabel = new JLabel("Enter frequency (Hz) and amplitude (-1 to 1) pairs:");
        instructionsLabel.setForeground(Color.WHITE);
        gbc.gridwidth = 4;
        controlPanel.add(instructionsLabel, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;

        // Add Row Button
        addRowButton = new JButton("Add Row");
        controlPanel.add(addRowButton, gbc);
        gbc.gridx++;

        // Remove Row Button
        removeRowButton = new JButton("Remove Selected Row");
        controlPanel.add(removeRowButton, gbc);
        gbc.gridx++;

        // Apply Button
        applyButton = new JButton("Apply");
        controlPanel.add(applyButton, gbc);
        gbc.gridx++;

        // Cancel Button
        cancelButton = new JButton("Cancel");
        controlPanel.add(cancelButton, gbc);

        // Layout
        setLayout(new BorderLayout());
        add(tableScroll, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // Button actions
        addRowButton.addActionListener(e -> {
            if (tableModel.getRowCount() < MAX_ROWS) {
                tableModel.addRow(new Object[]{0.0, 0.0});
            } else {
                JOptionPane.showMessageDialog(this, "Maximum number of rows (" + MAX_ROWS + ") reached.");
            }
        });

        removeRowButton.addActionListener(e -> {
            int selectedRow = dataTable.getSelectedRow();
            if (selectedRow >= 0) {
                tableModel.removeRow(selectedRow);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a row to remove.");
            }
        });

        applyButton.addActionListener(e -> {
            try {
                List<double[]> tableData = new ArrayList<>();
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    double freq = Double.parseDouble(tableModel.getValueAt(i, 0).toString());
                    double amp = Double.parseDouble(tableModel.getValueAt(i, 1).toString());
                    if (freq < 0) {
                        throw new NumberFormatException("Frequency must be non-negative at row " + (i + 1));
                    }
                    if (amp < -1 || amp > 1) {
                        throw new NumberFormatException("Amplitude must be between -1 and 1 at row " + (i + 1));
                    }
                    tableData.add(new double[]{freq, amp});
                }
                if (tableData.isEmpty()) {
                    throw new IllegalStateException("Table data cannot be empty.");
                }
                generator.setTableData(tableData);
                JOptionPane.showMessageDialog(this, "Table data applied successfully.");
                dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
            } catch (IllegalStateException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            }
        });

        cancelButton.addActionListener(e -> dispose());
    }
}