package simulation.software.codebase;

import java.util.List;
import java.util.Random;

public class SweepGenerator {
    private double startFreq;
    private double endFreq;
    private double sweepTime;
    private long holdTime; // in milliseconds
    private double repeatInterval; // in seconds
    private double dutyCycle; // 0.0 to 1.0
    private double noiseAmplitude;
    private double modFreq;
    private double modIndex;
    private String mode;
    private String signalType;
    private String noiseType;
    private String modType;
    private boolean isRunning;
    private long startTime;
    private List<double[]> tableData; // For Table Sweep: [frequency, amplitude]
    private List<double[]> arbitraryData; // For Arbitrary Waveform: [time, amplitude]
    private Random random = new Random();
    private double pinkNoiseState = 0; // For pink noise generation
    private static final int STEPS = 10;

    public void startSweep(double startFreq, double endFreq, double sweepTime, long holdTime, 
                          double repeatInterval, double dutyCycle, double noiseAmplitude, 
                          double modFreq, double modIndex, String mode, String signalType, 
                          String noiseType, String modType) {
        this.startFreq = startFreq;
        this.endFreq = endFreq;
        this.sweepTime = sweepTime;
        this.holdTime = holdTime;
        this.repeatInterval = repeatInterval;
        this.dutyCycle = dutyCycle;
        this.noiseAmplitude = noiseAmplitude;
        this.modFreq = modFreq;
        this.modIndex = modIndex;
        this.mode = mode;
        this.signalType = signalType;
        this.noiseType = noiseType;
        this.modType = modType;
        this.isRunning = true;
        this.startTime = System.currentTimeMillis();
        System.out.println("Started sweep: Mode=" + mode + ", SignalType=" + signalType);
    }

    public void stopSweep() {
        this.isRunning = false;
        System.out.println("Stopped sweep");
    }

    public void setTableData(List<double[]> tableData) {
        this.tableData = tableData;
        System.out.println("Table data set: " + (tableData != null ? tableData.size() : 0) + " entries");
    }

    public void setArbitraryData(List<double[]> arbitraryData) {
        this.arbitraryData = arbitraryData;
        System.out.println("Arbitrary data set: " + (arbitraryData != null ? arbitraryData.size() : 0) + " entries");
    }

    public List<double[]> getTableData() {
        return tableData;
    }

    public List<double[]> getArbitraryData() {
        return arbitraryData;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public double getCurrentValue() {
        if (!isRunning) return 0;
        double t = getNormalizedTime();
        return calculateValue(t);
    }

    public boolean isTimeSweep() {
        return mode.equals("Time");
    }

    public double calculateWaveform(double t) {
        if (!isRunning || !isActive()) return 0;

        double freq = isTimeSweep() ? (startFreq + endFreq) / 2 : calculateValue(t);
        double phase = 2 * Math.PI * freq * t;
        double carrier = generateSignal(phase);
        double modulated = applyModulation(carrier, phase, t);
        return modulated + generateNoise();
    }

    public double[] getWaveformSamples(int sampleCount, double sampleRate) {
        double[] samples = new double[sampleCount];
        double dt = 1.0 / sampleRate;
        for (int i = 0; i < sampleCount; i++) {
            samples[i] = calculateWaveform(i * dt);
        }
        return samples;
    }

    public double[] getWaveformSamples(int sampleCount, double sampleRate, double duration) {
        double[] samples = new double[sampleCount];
        double dt = duration / (sampleCount - 1);
        for (int i = 0; i < sampleCount; i++) {
            samples[i] = calculateWaveform(i * dt);
        }
        return samples;
    }

    public String getWaveformCSV(int sampleCount, double sampleRate) {
        StringBuilder csv = new StringBuilder("Time (s),Amplitude\n");
        double dt = 1.0 / sampleRate;
        double[] samples = getWaveformSamples(sampleCount, sampleRate);
        for (int i = 0; i < sampleCount; i++) {
            csv.append(String.format("%.6f,%.6f\n", i * dt, samples[i]));
        }
        return csv.toString();
    }

    public String getDacCSV(int sampleCount, double sampleRate, int bitDepth) {
        StringBuilder csv = new StringBuilder("Time (s),Analog Amplitude,Quantized Amplitude\n");
        double dt = 1.0 / sampleRate;
        double[] samples = getWaveformSamples(sampleCount, sampleRate);
        int levels = 1 << bitDepth;
        double maxAmplitude = 1.0; // Assuming waveform is normalized to [-1, 1]
        double stepSize = 2.0 * maxAmplitude / levels;
        for (int i = 0; i < sampleCount; i++) {
            double sample = samples[i];
            double quantized = Math.round(sample / stepSize) * stepSize;
            csv.append(String.format("%.6f,%.6f,%.6f\n", i * dt, sample, quantized));
        }
        return csv.toString();
    }

    private double calculateValue(double t) {
        switch (mode) {
            case "Linear":
                return startFreq + (endFreq - startFreq) * t;
            case "Logarithmic":
            case "Glide":
                double logStart = Math.log10(startFreq);
                double logEnd = Math.log10(endFreq);
                return Math.pow(10, logStart + (logEnd - logStart) * t);
            case "Stepped":
                long elapsed = System.currentTimeMillis() - startTime;
                long cycleTime = (long) (repeatInterval * 1000);
                elapsed = elapsed % cycleTime;
                int step = (int) (elapsed / holdTime);
                if (step > STEPS) step = STEPS;
                double stepSize = (endFreq - startFreq) / STEPS;
                return startFreq + step * stepSize;
            case "Bidirectional":
                double midPoint = 0.5;
                if (t < midPoint) {
                    return startFreq + (endFreq - startFreq) * (t / midPoint);
                } else {
                    return endFreq - (endFreq - startFreq) * ((t - midPoint) / midPoint);
                }
            case "Time":
                return Math.sin(2 * Math.PI * (startFreq + endFreq) / 2 * t * sweepTime) * 0.5;
            case "Table":
                if (tableData == null || tableData.isEmpty()) {
                    System.out.println("Warning: Table data is empty, using startFreq");
                    return startFreq;
                }
                int index = (int) (t * (tableData.size() - 1));
                if (index >= tableData.size() - 1) return tableData.get(tableData.size() - 1)[0];
                double t0 = (double) index / (tableData.size() - 1);
                double t1 = (double) (index + 1) / (tableData.size() - 1);
                double f0 = tableData.get(index)[0];
                double f1 = tableData.get(index + 1)[0];
                return f0 + (f1 - f0) * (t - t0) / (t1 - t0);
            default:
                System.out.println("Warning: Unknown mode " + mode + ", using startFreq");
                return startFreq;
        }
    }

    private double generateSignal(double phase) {
        switch (signalType) {
            case "Sine":
                return Math.sin(phase);
            case "Triangle":
                return 2 * Math.abs(phase / Math.PI - Math.floor(phase / Math.PI + 0.5)) - 1;
            case "Square":
                return Math.sin(phase) >= 0 ? 1 : -1;
            case "Ramp":
                return (phase % (2 * Math.PI)) / Math.PI - 1;
            case "Arbitrary":
                if (arbitraryData == null || arbitraryData.isEmpty()) {
                    System.out.println("Warning: Arbitrary data is empty, returning 0");
                    return 0;
                }
                double t = (phase % (2 * Math.PI)) / (2 * Math.PI);
                int index = (int) (t * (arbitraryData.size() - 1));
                if (index >= arbitraryData.size() - 1) return arbitraryData.get(arbitraryData.size() - 1)[1];
                double t0 = (double) index / (arbitraryData.size() - 1);
                double t1 = (double) (index + 1) / (arbitraryData.size() - 1);
                double a0 = arbitraryData.get(index)[1];
                double a1 = arbitraryData.get(index + 1)[1];
                return a0 + (a1 - a0) * (t - t0) / (t1 - t0);
            case "Noise":
                return generateNoise();
            default:
                System.out.println("Warning: Unknown signal type " + signalType + ", using sine");
                return Math.sin(phase);
        }
    }

    private double applyModulation(double carrier, double phase, double t) {
        double modSignal = Math.sin(2 * Math.PI * modFreq * t);
        switch (modType) {
            case "AM":
                return carrier * (1 + modIndex * modSignal);
            case "FM":
                return Math.sin(phase + modIndex * modSignal);
            case "PM":
                return Math.sin(phase + modIndex * modSignal * 2 * Math.PI);
            default:
                return carrier;
        }
    }

    private double generateNoise() {
        if (noiseType.equals("None") || noiseAmplitude == 0) return 0;
        switch (noiseType) {
            case "White":
                return noiseAmplitude * (random.nextDouble() * 2 - 1);
            case "Pink":
                pinkNoiseState = pinkNoiseState * 0.95 + (random.nextDouble() * 2 - 1) * 0.05;
                return noiseAmplitude * pinkNoiseState;
            case "Gaussian":
                return noiseAmplitude * random.nextGaussian();
            default:
                return 0;
        }
    }

    private double getNormalizedTime() {
        long elapsed = System.currentTimeMillis() - startTime;
        long cycleTime = (long) (repeatInterval * 1000);
        if (cycleTime <= 0) cycleTime = (long) (sweepTime * 1000);
        elapsed = elapsed % cycleTime;
        double activeTime = cycleTime * dutyCycle;
        if (elapsed > activeTime) return 0;
        double t = elapsed / (sweepTime * 1000.0);
        return Math.min(t, 1.0);
    }

    private boolean isActive() {
        long elapsed = System.currentTimeMillis() - startTime;
        long cycleTime = (long) (repeatInterval * 1000);
        if (cycleTime <= 0) return true;
        elapsed = elapsed % cycleTime;
        double activeTime = cycleTime * dutyCycle;
        return elapsed <= activeTime;
    }
}