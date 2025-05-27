package simulation.software.codebase;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SweepMusterAnpassen extends JFrame {
    private SweepGenerator generator;
    private AdaptationPanel adaptationPanel;
    private JLabel statusLabel;
    private JTextField targetResponseField;
    private JSlider startFreqMinSlider, startFreqMaxSlider, endFreqMinSlider, endFreqMaxSlider, sweepTimeMinSlider, sweepTimeMaxSlider;
    private JButton startAdaptationButton, exportButton;
    private static final int SAMPLE_RATE = 44100;

    public SweepMusterAnpassen(SweepGenerator generator) {
        this.generator = generator;
        setTitle("RL Sweep Adaptation");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Set Bahnschrift font
        Font bahnschrift = new Font("Bahnschrift", Font.PLAIN, 12);
        UIManager.put("Label.font", bahnschrift);
        UIManager.put("Button.font", bahnschrift);
        UIManager.put("TextField.font", bahnschrift);
        UIManager.put("Slider.font", bahnschrift);

        adaptationPanel = new AdaptationPanel(generator);
        add(adaptationPanel, BorderLayout.CENTER);

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

        // Target Response
        JLabel targetLabel = new JLabel("Target Response (freq,dB;...):");
        controlPanel.add(targetLabel, gbc);
        gbc.gridx = 1;
        targetResponseField = new JTextField("100,0;500,0;1000,0", 20);
        controlPanel.add(targetResponseField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Start Frequency Min Slider
        JLabel startFreqMinLabel = new JLabel("Start Freq Min (Hz):");
        controlPanel.add(startFreqMinLabel, gbc);
        gbc.gridx = 1;
        startFreqMinSlider = new JSlider(JSlider.HORIZONTAL, 20, 1000, 50);
        startFreqMinSlider.setMajorTickSpacing(250);
        startFreqMinSlider.setPaintTicks(true);
        startFreqMinSlider.setPaintLabels(true);
        controlPanel.add(startFreqMinSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Start Frequency Max Slider
        JLabel startFreqMaxLabel = new JLabel("Start Freq Max (Hz):");
        controlPanel.add(startFreqMaxLabel, gbc);
        gbc.gridx = 1;
        startFreqMaxSlider = new JSlider(JSlider.HORIZONTAL, 50, 2000, 500);
        startFreqMaxSlider.setMajorTickSpacing(500);
        startFreqMaxSlider.setPaintTicks(true);
        startFreqMaxSlider.setPaintLabels(true);
        controlPanel.add(startFreqMaxSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // End Frequency Min Slider
        JLabel endFreqMinLabel = new JLabel("End Freq Min (Hz):");
        controlPanel.add(endFreqMinLabel, gbc);
        gbc.gridx = 1;
        endFreqMinSlider = new JSlider(JSlider.HORIZONTAL, 500, 5000, 1000);
        endFreqMinSlider.setMajorTickSpacing(1000);
        endFreqMinSlider.setPaintTicks(true);
        endFreqMinSlider.setPaintLabels(true);
        controlPanel.add(endFreqMinSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // End Frequency Max Slider
        JLabel endFreqMaxLabel = new JLabel("End Freq Max (Hz):");
        controlPanel.add(endFreqMaxLabel, gbc);
        gbc.gridx = 1;
        endFreqMaxSlider = new JSlider(JSlider.HORIZONTAL, 1000, 20000, 5000);
        endFreqMaxSlider.setMajorTickSpacing(5000);
        endFreqMaxSlider.setPaintTicks(true);
        endFreqMaxSlider.setPaintLabels(true);
        controlPanel.add(endFreqMaxSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Sweep Time Min Slider
        JLabel sweepTimeMinLabel = new JLabel("Sweep Time Min (s):");
        controlPanel.add(sweepTimeMinLabel, gbc);
        gbc.gridx = 1;
        sweepTimeMinSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, 5);
        sweepTimeMinSlider.setMajorTickSpacing(5);
        sweepTimeMinSlider.setPaintTicks(true);
        sweepTimeMinSlider.setPaintLabels(true);
        controlPanel.add(sweepTimeMinSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Sweep Time Max Slider
        JLabel sweepTimeMaxLabel = new JLabel("Sweep Time Max (s):");
        controlPanel.add(sweepTimeMaxLabel, gbc);
        gbc.gridx = 1;
        sweepTimeMaxSlider = new JSlider(JSlider.HORIZONTAL, 5, 60, 30);
        sweepTimeMaxSlider.setMajorTickSpacing(15);
        sweepTimeMaxSlider.setPaintTicks(true);
        sweepTimeMaxSlider.setPaintLabels(true);
        controlPanel.add(sweepTimeMaxSlider, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Start/Stop Adaptation Button
        startAdaptationButton = new JButton("Start RL Adaptation");
        gbc.gridwidth = 2;
        controlPanel.add(startAdaptationButton, gbc);
        gbc.gridy++;

        // Export Button
        exportButton = new JButton("Export Adaptation CSV");
        gbc.gridwidth = 2;
        controlPanel.add(exportButton, gbc);

        add(controlPanel, BorderLayout.SOUTH);

        startAdaptationButton.addActionListener(e -> {
            if (adaptationPanel.isAdapting()) {
                adaptationPanel.stopAdaptation();
                startAdaptationButton.setText("Start RL Adaptation");
                statusLabel.setText("Status: Idle");
            } else {
                try {
                    adaptationPanel.startAdaptation(
                        parseTargetResponse(targetResponseField.getText()),
                        startFreqMinSlider.getValue(), startFreqMaxSlider.getValue(),
                        endFreqMinSlider.getValue(), endFreqMaxSlider.getValue(),
                        sweepTimeMinSlider.getValue(), sweepTimeMaxSlider.getValue()
                    );
                    startAdaptationButton.setText("Stop RL Adaptation");
                    statusLabel.setText("Status: Adapting...");
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid target response: " + ex.getMessage());
                }
            }
        });

        exportButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Adaptation CSV");
            fileChooser.setSelectedFile(new File("adaptation_data.csv"));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".csv")) {
                    file = new File(file.getPath() + ".csv");
                }
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(adaptationPanel.getAdaptationCSV());
                    JOptionPane.showMessageDialog(this, "Adaptation CSV saved to " + file.getPath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving CSV: " + ex.getMessage());
                }
            }
        });
    }

    private double[][] parseTargetResponse(String input) {
        try {
            String[] pairs = input.split(";");
            double[][] response = new double[pairs.length][2];
            for (int i = 0; i < pairs.length; i++) {
                String[] parts = pairs[i].split(",");
                response[i][0] = Double.parseDouble(parts[0].trim()); // Frequency
                response[i][1] = Double.parseDouble(parts[1].trim()); // dB
            }
            return response;
        } catch (Exception e) {
            throw new IllegalArgumentException("Format: freq,dB;freq,dB (e.g., 100,0;500,0)");
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (adaptationPanel != null) {
            adaptationPanel.repaint();
        }
    }

    private class AdaptationPanel extends JPanel {
        private SweepGenerator generator;
        private List<Double> rewardHistory;
        private List<Double> timeHistory;
        private double[][] targetResponse;
        private double startFreq, endFreq, sweepTime;
        private double reward;
        private boolean adapting;
        private Thread adaptationThread;
        private QLearningAgent agent;
        private static final int MAX_POINTS = 1000;
        private static final double WINDOW_SIZE = 0.02; // 20ms

        public AdaptationPanel(SweepGenerator generator) {
            this.generator = generator;
            this.rewardHistory = new ArrayList<>();
            this.timeHistory = new ArrayList<>();
            this.startFreq = 100;
            this.endFreq = 1000;
            this.sweepTime = 10;
            this.reward = 0;
            this.adapting = false;
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(600, 400));
        }

        public void startAdaptation(double[][] targetResponse, int startFreqMin, int startFreqMax,
                                   int endFreqMin, int endFreqMax, int sweepTimeMin, int sweepTimeMax) {
            if (adapting) return;
            this.targetResponse = targetResponse;
            this.adapting = true;
            rewardHistory.clear();
            timeHistory.clear();

            // Initialize parameters within bounds
            this.startFreq = Math.max(startFreqMin, Math.min(startFreqMax, this.startFreq));
            this.endFreq = Math.max(endFreqMin, Math.min(endFreqMax, this.endFreq));
            this.sweepTime = Math.max(sweepTimeMin, Math.min(sweepTimeMax, this.sweepTime));

            agent = new QLearningAgent(startFreqMin, startFreqMax, endFreqMin, endFreqMax, sweepTimeMin, sweepTimeMax);
            adaptationThread = new Thread(() -> {
                while (adapting) {
                    // Get current state
                    int[] state = agent.getState(startFreq, endFreq, sweepTime);

                    // Choose action
                    int action = agent.chooseAction(state);

                    // Apply action
                    double[] newParams = agent.applyAction(action, startFreq, endFreq, sweepTime);
                    startFreq = newParams[0];
                    endFreq = newParams[1];
                    sweepTime = newParams[2];

                    // Simulate sweep
                    generator.startSweep(startFreq, endFreq, sweepTime, 500, 15, 
                                        0.8, 0, 10, 0.5, "Linear", "Sine", "None", "None");
                    try {
                        Thread.sleep(100); // Allow sweep to stabilize
                    } catch (InterruptedException e) {
                        break;
                    }

                    // Compute reward
                    reward = -computeMSE();
                    int[] nextState = agent.getState(startFreq, endFreq, sweepTime);
                    agent.updateQTable(state, action, reward, nextState);

                    // Update history
                    double currentTime = System.currentTimeMillis() / 1000.0;
                    rewardHistory.add(reward);
                    timeHistory.add(currentTime);
                    if (rewardHistory.size() > MAX_POINTS) {
                        rewardHistory.remove(0);
                        timeHistory.remove(0);
                    }

                    repaint();
                }
                final double finalReward = reward;
                SwingUtilities.invokeLater(() -> {
                    startAdaptationButton.setText("Start RL Adaptation");
                    statusLabel.setText(String.format("Status: Done (Reward: %.2f)", finalReward));
                });
            });
            adaptationThread.start();
        }

        public void stopAdaptation() {
            adapting = false;
            if (adaptationThread != null) {
                adaptationThread.interrupt();
            }
        }

        public boolean isAdapting() {
            return adapting;
        }

        private double computeMSE() {
            int sampleCount = (int) (WINDOW_SIZE * SAMPLE_RATE);
            double[] samples = generator.getWaveformSamples(sampleCount, SAMPLE_RATE, WINDOW_SIZE);
            FFTCalculator fft = new FFTCalculator();
            double[] magnitude = fft.computeFFTMagnitude(samples);
            double[] freqs = new double[magnitude.length];
            double freqStep = SAMPLE_RATE / (double) (magnitude.length * 2);
            for (int i = 0; i < freqs.length; i++) {
                freqs[i] = i * freqStep;
            }

            // Interpolate target response
            double[] targetDB = new double[magnitude.length];
            for (int i = 0; i < magnitude.length; i++) {
                double freq = freqs[i];
                targetDB[i] = interpolateTarget(freq);
            }

            // Compute MSE
            double sumSquaredError = 0;
            for (int i = 0; i < magnitude.length; i++) {
                double generatedDB = 20 * Math.log10(magnitude[i] + 1e-10);
                double error = generatedDB - targetDB[i];
                sumSquaredError += error * error;
            }
            return sumSquaredError / magnitude.length;
        }

        private double interpolateTarget(double freq) {
            if (targetResponse.length < 2) return targetResponse[0][1];
            for (int i = 0; i < targetResponse.length - 1; i++) {
                double f1 = targetResponse[i][0], dB1 = targetResponse[i][1];
                double f2 = targetResponse[i + 1][0], dB2 = targetResponse[i + 1][1];
                if (freq >= f1 && freq <= f2) {
                    return dB1 + (dB2 - dB1) * (freq - f1) / (f2 - f1);
                }
            }
            return targetResponse[targetResponse.length - 1][1];
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

            // Plot target and current response
            if (targetResponse != null) {
                int sampleCount = (int) (WINDOW_SIZE * SAMPLE_RATE);
                double[] samples = generator.getWaveformSamples(sampleCount, SAMPLE_RATE, WINDOW_SIZE);
                FFTCalculator fft = new FFTCalculator();
                double[] magnitude = fft.computeFFTMagnitude(samples);
                double[] freqs = new double[magnitude.length];
                double freqStep = SAMPLE_RATE / (double) (magnitude.length * 2);
                for (int i = 0; i < freqs.length; i++) {
                    freqs[i] = i * freqStep;
                }

                // Target response (white)
                g2d.setColor(Color.WHITE);
                for (int i = 1; i < freqs.length; i++) {
                    int x1 = (int) (freqs[i - 1] / 20000.0 * width);
                    int x2 = (int) (freqs[i] / 20000.0 * width);
                    int y1 = (int) (midY - interpolateTarget(freqs[i - 1]) / 20.0 * (height / 2 - 50));
                    int y2 = (int) (midY - interpolateTarget(freqs[i]) / 20.0 * (height / 2 - 50));
                    g2d.drawLine(x1, y1, x2, y2);
                }

                // Current response (cyan)
                g2d.setColor(Color.CYAN);
                for (int i = 1; i < magnitude.length; i++) {
                    int x1 = (int) (freqs[i - 1] / 20000.0 * width);
                    int x2 = (int) (freqs[i] / 20000.0 * width);
                    double dB1 = 20 * Math.log10(magnitude[i - 1] + 1e-10);
                    double dB2 = 20 * Math.log10(magnitude[i] + 1e-10);
                    int y1 = (int) (midY - dB1 / 20.0 * (height / 2 - 50));
                    int y2 = (int) (midY - dB2 / 20.0 * (height / 2 - 50));
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }

            // Plot reward history (yellow)
            g2d.setColor(Color.YELLOW);
            double minTime = timeHistory.isEmpty() ? 0 : timeHistory.get(0);
            double maxTime = timeHistory.isEmpty() ? 1 : timeHistory.get(timeHistory.size() - 1);
            double timeRange = maxTime - minTime > 0 ? maxTime - minTime : 1;
            for (int i = 1; i < rewardHistory.size(); i++) {
                int x1 = (int) ((timeHistory.get(i - 1) - minTime) / timeRange * width);
                int x2 = (int) ((timeHistory.get(i) - minTime) / timeRange * width);
                int y1 = (int) (midY - rewardHistory.get(i - 1) / -100.0 * (height / 2 - 50));
                int y2 = (int) (midY - rewardHistory.get(i) / -100.0 * (height / 2 - 50));
                g2d.drawLine(x1, y1, x2, y2);
            }

            // Draw axes labels
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Bahnschrift", Font.PLAIN, 12));
            g2d.drawString("Frequency (Hz)", width / 2 - 30, height - 10);
            g2d.rotate(-Math.PI / 2);
            g2d.drawString("Amplitude (dB) / Reward", -height / 2 - 30, 20);
            g2d.rotate(Math.PI / 2);

            // Draw current parameters
            g2d.drawString(String.format("Start: %.0f Hz, End: %.0f Hz, Time: %.1f s, Reward: %.2f",
                                        startFreq, endFreq, sweepTime, reward), 10, 20);
        }

        public String getAdaptationCSV() {
            StringBuilder csv = new StringBuilder("Time (s),Start Freq (Hz),End Freq (Hz),Sweep Time (s),Reward\n");
            if (rewardHistory.isEmpty()) return csv.toString();
            for (int i = 0; i < rewardHistory.size(); i++) {
                csv.append(String.format("%.3f,%.0f,%.0f,%.1f,%.2f\n",
                                        timeHistory.get(i), startFreq, endFreq, sweepTime, rewardHistory.get(i)));
            }
            return csv.toString();
        }
    }

    private class QLearningAgent {
        private double[][][][] qTable;
        private int startFreqBins, endFreqBins, sweepTimeBins;
        private double startFreqMin, startFreqMax, endFreqMin, endFreqMax, sweepTimeMin, sweepTimeMax;
        private double alpha = 0.1; // Learning rate
        private double gamma = 0.9; // Discount factor
        private double epsilon = 0.1; // Exploration rate
        private Random rand = new Random();
        private static final int ACTIONS = 27; // 3^3 (increase/decrease/no change for each parameter)

        public QLearningAgent(int startFreqMin, int startFreqMax, int endFreqMin, int endFreqMax,
                              int sweepTimeMin, int sweepTimeMax) {
            this.startFreqMin = startFreqMin;
            this.startFreqMax = startFreqMax;
            this.endFreqMin = endFreqMin;
            this.endFreqMax = endFreqMax;
            this.sweepTimeMin = sweepTimeMin;
            this.sweepTimeMax = sweepTimeMax;

            // Ensure at least one bin to avoid zero-sized arrays
            startFreqBins = Math.max(1, (int) ((startFreqMax - startFreqMin) / 50) + 1);
            endFreqBins = Math.max(1, (int) ((endFreqMax - endFreqMin) / 100) + 1);
            sweepTimeBins = Math.max(1, (int) ((sweepTimeMax - sweepTimeMin) / 1) + 1);

            qTable = new double[startFreqBins][endFreqBins][sweepTimeBins][ACTIONS];
        }

        public int[] getState(double startFreq, double endFreq, double sweepTime) {
            // Clamp parameters to bounds
            startFreq = Math.max(startFreqMin, Math.min(startFreqMax, startFreq));
            endFreq = Math.max(endFreqMin, Math.min(endFreqMax, endFreq));
            sweepTime = Math.max(sweepTimeMin, Math.min(sweepTimeMax, sweepTime));

            // Calculate indices, ensuring non-negative
            int startFreqIdx = Math.max(0, Math.min(startFreqBins - 1, (int) ((startFreq - startFreqMin) / 50)));
            int endFreqIdx = Math.max(0, Math.min(endFreqBins - 1, (int) ((endFreq - endFreqMin) / 100)));
            int sweepTimeIdx = Math.max(0, Math.min(sweepTimeBins - 1, (int) ((sweepTime - sweepTimeMin) / 1)));

            return new int[]{startFreqIdx, endFreqIdx, sweepTimeIdx};
        }

        public int chooseAction(int[] state) {
            int startFreqIdx = state[0], endFreqIdx = state[1], sweepTimeIdx = state[2];

            // Validate indices
            if (startFreqIdx < 0 || startFreqIdx >= startFreqBins ||
                endFreqIdx < 0 || endFreqIdx >= endFreqBins ||
                sweepTimeIdx < 0 || sweepTimeIdx >= sweepTimeBins) {
                System.err.println("Invalid state indices: startFreqIdx=" + startFreqIdx +
                                   ", endFreqIdx=" + endFreqIdx + ", sweepTimeIdx=" + sweepTimeIdx);
                return rand.nextInt(ACTIONS); // Fallback to random action
            }

            if (rand.nextDouble() < epsilon) {
                return rand.nextInt(ACTIONS);
            }
            int bestAction = 0;
            double maxQ = qTable[startFreqIdx][endFreqIdx][sweepTimeIdx][0];
            for (int a = 1; a < ACTIONS; a++) {
                if (qTable[startFreqIdx][endFreqIdx][sweepTimeIdx][a] > maxQ) {
                    maxQ = qTable[startFreqIdx][endFreqIdx][sweepTimeIdx][a];
                    bestAction = a;
                }
            }
            return bestAction;
        }

        public double[] applyAction(int action, double startFreq, double endFreq, double sweepTime) {
            int startFreqChange = (action / 9) % 3 - 1; // -1, 0, 1
            int endFreqChange = (action / 3) % 3 - 1;
            int sweepTimeChange = action % 3 - 1;

            // Apply changes and enforce bounds
            startFreq = Math.max(startFreqMin, Math.min(startFreqMax, startFreq + startFreqChange * 50));
            endFreq = Math.max(endFreqMin, Math.min(endFreqMax, endFreq + endFreqChange * 100));
            sweepTime = Math.max(sweepTimeMin, Math.min(sweepTimeMax, sweepTime + sweepTimeChange * 1));

            // Ensure endFreq > startFreq
            if (endFreq <= startFreq + 50) {
                endFreq = startFreq + 50;
            }

            return new double[]{startFreq, endFreq, sweepTime};
        }

        public void updateQTable(int[] state, int action, double reward, int[] nextState) {
            int s1 = state[0], s2 = state[1], s3 = state[2];
            int ns1 = nextState[0], ns2 = nextState[1], ns3 = nextState[2];

            // Validate indices
            if (s1 < 0 || s1 >= startFreqBins || s2 < 0 || s2 >= endFreqBins || s3 < 0 || s3 >= sweepTimeBins ||
                ns1 < 0 || ns1 >= startFreqBins || ns2 < 0 || ns2 >= endFreqBins || ns3 < 0 || ns3 >= sweepTimeBins) {
                System.err.println("Invalid Q-table indices: state=[" + s1 + "," + s2 + "," + s3 +
                                   "], nextState=[" + ns1 + "," + ns2 + "," + ns3 + "]");
                return;
            }

            double maxNextQ = qTable[ns1][ns2][ns3][0];
            for (int a = 1; a < ACTIONS; a++) {
                maxNextQ = Math.max(maxNextQ, qTable[ns1][ns2][ns3][a]);
            }

            qTable[s1][s2][s3][action] += alpha * (reward + gamma * maxNextQ - qTable[s1][s2][s3][action]);
        }
    }
}