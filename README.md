# Sweep Generation Software

_This software is an enhanced version of another sweep generation software which I previously wrote in JavaFX. I wrote this software in Java Swing. The previous version has some defects in a few features that might cause errors so I wrote the software again from scratch with improvements as well as some other advanced features._


## Functioning Logic
- **Core Components**:
  - **SweepGenerator**: The central class responsible for generating waveforms based on user-defined parameters (e.g., frequency range, sweep mode, signal type, modulation, noise).
  - **SweepGeneratorUI**: The main GUI, providing controls for configuring the sweep parameters and launching analysis windows (e.g., spectrum analyzer, phase analyzer).
  - **Analysis Modules**: Classes like `PhaseAnalyzer`, `SpectrumAnalyzerWindow`, `TotalHarmonicDistortion`, etc., provide specialized signal analysis and visualization.
  - **Simulation Modules**: Classes like `QuantenbewussteSignalsimulation` (quantum-aware simulation) and `SweepMusterAnpassen` (reinforcement learning adaptation) simulate advanced physical or computational effects.
- **Workflow**:
  1. Users configure parameters via the GUI (`SweepGeneratorUI`), including sweep mode, signal type, frequency range, modulation, and noise.
  2. The `SweepGenerator` class generates the waveform samples based on the selected parameters.
  3. Analysis modules process the waveform data (e.g., FFT for spectrum analysis, phase difference calculation) and display results visually.
  4. Users can export data as CSV files for further analysis.
  5. Advanced simulations (e.g., quantum effects, reinforcement learning) modify the waveform based on specific models.
- **GUI Interaction**:
  - The GUI uses `JComboBox`, `JTextField`, `JSlider`, and `JButton` components for user input.
  - A `Timer` updates visualizations every 50ms for real-time waveform and analysis display.
  - Dialogs like `TableSweepDialog` and `ArbitraryWaveformDialog` allow users to input custom data for table-based or arbitrary waveforms.
- **Data Flow**:
  - **Input**: User parameters (e.g., start/end frequency, sweep time) are validated and passed to `SweepGenerator`.
  - **Processing**: `SweepGenerator` computes waveform samples using mathematical models (e.g., sine, triangle, or arbitrary signals).
  - **Output**: Samples are visualized in panels (e.g., `WaveformPanel`, `SpectrumPanel`) or exported as CSV.

## Simulation Logic
- **Waveform Generation** (`SweepGenerator`):
  - Generates waveforms based on sweep mode (Linear, Logarithmic, Stepped, Bidirectional, Time, Table).
  - Supports signal types: Sine, Triangle, Square, Ramp, Arbitrary, Noise.
  - Applies modulation (AM, FM, PM) and noise (White, Pink, Gaussian).
- **Sweep Modes**:
  - **Linear**: Frequency changes linearly from startFreq to endFreq over sweepTime.
  - **Logarithmic/Glide**: Frequency changes logarithmically, using log10(startFreq) to log10(endFreq).
  - **Stepped**: Frequency changes in discrete steps, holding each step for holdTime.
  - **Bidirectional**: Frequency sweeps from startFreq to endFreq and back within sweepTime.
  - **Time**: Generates a fixed-frequency sine wave with amplitude modulation.
  - **Table**: Interpolates frequency-amplitude pairs from user-defined table data.
- **Analysis Simulations**:
  - **Spectrum Analysis** (`SpectrumAnalyzerWindow`): Computes the frequency spectrum using FFT and displays it in linear or logarithmic scale.
  - **Phase Analysis** (`PhaseAnalyzer`): Calculates phase differences between two channels with a user-defined phase shift.
  - **Total Harmonic Distortion (THD)** (`TotalHarmonicDistortion`): Measures harmonic distortion by comparing fundamental and harmonic magnitudes.
  - **Cross-Coupling** (`CrossCoupling`): Simulates crosstalk between two channels and computes crosstalk in dB.
  - **Digital-to-Analog Conversion** (`DigitalToAnalog`): Simulates DAC effects like quantization, nonlinearity, and thermal noise.
- **Advanced Simulations**:
  - **Quantum-Aware Simulation** (`QuantenbewussteSignalsimulation`):
    - Simulates quantum effects like thermal relaxation (T1) and decoherence (T2).
    - Applies modulation modes: BPSK, QPSK, or Entanglement-Inspired (Bell-like phase correlation).
    - Computes Hellinger fidelity to measure similarity between ideal and noisy waveforms.
  - **Reinforcement Learning Adaptation** (`SweepMusterAnpassen`):
    - Uses Q-learning to optimize sweep parameters (start/end frequency, sweep time) to match a target frequency response.
    - Minimizes mean squared error (MSE) between generated and target spectra.
- **Real-Time Updates**:
  - A `Timer` triggers repaints every 50ms, ensuring dynamic visualization of waveforms and analysis results.
  - Analysis modules fetch fresh samples from `SweepGenerator` for each update.

## Physics Models
- **Signal Generation**:
  - **Sine Wave**: y(t) = sin(2 * pi * f * t), where f is the instantaneous frequency.
  - **Triangle Wave**: y(t) = 2 * | (phase / pi) - floor(phase / pi + 0.5) | - 1.
  - **Square Wave**: y(t) = sign(sin(phase)), where sign(x) = 1 if x >= 0, else -1.
  - **Ramp Wave**: y(t) = (phase mod (2 * pi)) / pi - 1.
  - **Arbitrary Waveform**: Linear interpolation between user-defined time-amplitude pairs.
- **Modulation**:
  - **Amplitude Modulation (AM)**: y(t) = carrier * (1 + modIndex * sin(2 * pi * modFreq * t)).
  - **Frequency Modulation (FM)**: y(t) = sin(phase + modIndex * sin(2 * pi * modFreq * t)).
  - **Phase Modulation (PM)**: y(t) = sin(phase + modIndex * sin(2 * pi * modFreq * t) * 2 * pi).
- **Noise Models**:
  - **White Noise**: Uniform random values in [-1, 1], scaled by noiseAmplitude.
  - **Pink Noise**: Low-pass filtered white noise, using pinkNoiseState = 0.95 * pinkNoiseState + 0.05 * random(-1, 1).
  - **Gaussian Noise**: Random values from a Gaussian distribution, scaled by noiseAmplitude.
- **Quantum Noise** (`QuantenbewussteSignalsimulation`):
  - **Thermal Relaxation (T1)**: Amplitude damping, modeled as exp(-t / T1), where T1 is in seconds.
  - **Decoherence (T2)**: Phase damping, modeled as exp(-t / T2), where T2 <= 2 * T1.
  - **Modulation**:
    - BPSK: Phase = 0 or pi (0° or 180°).
    - QPSK: Phase = (idx * pi / 2) + pi / 4, where idx = 0, 1, 2, 3 (45°, 135°, 225°, 315°).
    - Entanglement-Inspired: Correlated phases for two virtual qubits, phase = (qubit1Phase + qubit2Phase) / 2.
- **Digital to Analog Conversion** 
  - **Quantization**: y_quantized = round(y / stepSize) * stepSize, where stepSize = 2 * maxAmplitude / (2^bitDepth).
  - **Nonlinearity**: Adds distortion to quantized signal (implementation not shown in provided files).
  - **Thermal Noise**: Adds Gaussian noise to the signal.
- **Cross-Coupling** 
  - Simulates crosstalk as a fraction of one channel’s signal leaking into another.
  - Crosstalk (dB) = 20 * log10(|crosstalk_signal| / |main_signal|).
- **Total Harmonic Distortion**
  - THD (%) = 100 * sqrt(harmonicPower / fundamentalPower), where harmonicPower is the sum of squared harmonic magnitudes, and fundamentalPower is the squared fundamental magnitude.
  - THD (dB) = 20 * log10(THD / 100).


## Algorithms
- **Fast Fourier Transform (FFT)** 
  - **Algorithm**: Cooley-Tukey FFT (recursive divide-and-conquer).
  - **Steps**:
    1. Split input into even and odd indices.
    2. Recursively compute FFT on even and odd parts.
    3. Combine results using twiddle factors: wk = cos(-2 * k * pi / n) + i * sin(-2 * k * pi / n).
  - **Output**: Complex spectrum, converted to magnitude as sqrt(real^2 + imag^2).
  - **Used In**: Spectrum analysis, phase analysis, THD, and RL adaptation.
- **Phase Difference Calculation**
  - **Steps**:
    1. Compute FFT of left and right channel samples.
    2. Identify fundamental frequency by finding the peak magnitude in the FFT.
    3. Calculate phase: phase = atan2(imag, real) for the fundamental frequency bin.
    4. Compute phase difference: delta_phase = rightPhase - leftPhase, normalized to [-180°, 180°].
  - **Output**: Phase difference in degrees and fundamental frequency in Hz.
- **Total Harmonic Distortion**
  - **Steps**:
    1. Compute FFT magnitude spectrum.
    2. Identify fundamental frequency (peak magnitude).
    3. Extract magnitudes at harmonic frequencies (2f, 3f, ..., 10f).
    4. Compute THD as described in the physics models section.
  - **Output**: THD in percentage and dB, plus harmonic magnitudes.
- **Q-Learning for Sweep Adaptation** 
  - **Algorithm**: Q-learning (reinforcement learning).
  - **Components**:
    - **State**: Discretized start frequency, end frequency, and sweep time.
    - **Actions**: 27 possible actions (3^3 combinations of increase/decrease/no change for each parameter).
    - **Reward**: Negative MSE between generated and target frequency response.
    - **Q-Table**: 4D array [startFreqBins][endFreqBins][sweepTimeBins][actions].
  - **Steps**:
    1. Initialize Q-table with zeros.
    2. For each iteration:
       - Get current state (discretized parameters).
       - Choose action (epsilon-greedy: random with probability epsilon, else best Q-value).
       - Apply action (adjust parameters within bounds).
       - Simulate sweep and compute reward (-MSE).
       - Update Q-table: Q(s, a) += alpha * (reward + gamma * maxQ(nextState) - Q(s, a)).
  - **Parameters**:
    - alpha = 0.1 (learning rate).
    - gamma = 0.9 (discount factor).
    - epsilon = 0.1 (exploration rate).
  - **Output**: Optimized sweep parameters and reward history.
- **Hellinger Fidelity**
  - **Steps**:
    1. Compute sum = sum(sqrt(|ideal[i]| * |noisy[i]|)) for all samples.
    2. Normalize: sum /= sampleCount.
    3. Return fidelity = sum^2.
  - **Output**: Fidelity measure (0 to 1) between ideal and noisy quantum waveforms.
- **Table Interpolation**
  - **Algorithm**: Linear interpolation.
  - **Steps**:
    1. For a normalized time t (0 to 1), find index = t * (tableSize - 1).
    2. Interpolate between table[index] and table[index + 1] using: value = f0 + (f1 - f0) * (t - t0) / (t1 - t0).
  - **Used In**: Table sweep mode and arbitrary waveform generation.

## Equations

- **Linear Sweep Frequency**:
  f(t) = startFreq + (endFreq - startFreq) * t
  where t is normalized time (0 to 1).
- **Logarithmic Sweep Frequency**:
  f(t) = 10^(log10(startFreq) + (log10(endFreq) - log10(startFreq)) * t)
- **Stepped Sweep Frequency**:
  f(t) = startFreq + floor(elapsed / holdTime) * (endFreq - startFreq) / STEPS
  where elapsed is time since sweep start, and STEPS = 10.
- **Bidirectional Sweep Frequency**:
  if t < 0.5: f(t) = startFreq + (endFreq - startFreq) * (t / 0.5)
  else: f(t) = endFreq - (endFreq - startFreq) * ((t - 0.5) / 0.5)
- **Time Sweep Amplitude**:
  y(t) = sin(2 * pi * (startFreq + endFreq) / 2 * t * sweepTime) * 0.5
- **Waveform Calculation**:
  y(t) = generateSignal(2 * pi * f * t) * applyModulation(t) + generateNoise()
  where f is the instantaneous frequency.
- **AM Modulation**:
  y(t) = carrier * (1 + modIndex * sin(2 * pi * modFreq * t))
- **FM Modulation**:
  y(t) = sin(phase + modIndex * sin(2 * pi * modFreq * t))
- **PM Modulation**:
  y(t) = sin(phase + modIndex * sin(2 * pi * modFreq * t) * 2 * pi)
- **White Noise**:
  noise(t) = noiseAmplitude * (2 * random() - 1)
- **Pink Noise**:
  pinkNoiseState = 0.95 * pinkNoiseState + 0.05 * (2 * random() - 1)
  noise(t) = noiseAmplitude * pinkNoiseState
- **Gaussian Noise**:
  noise(t) = noiseAmplitude * gaussianRandom()
- **Quantum Amplitude Damping (T1)**:
  ampDamping(t) = exp(-t / T1)
- **Quantum Phase Damping (T2)**:
  phaseDamping(t) = exp(-t / T2)
- **Hellinger Fidelity**:
  fidelity = (sum(sqrt(|ideal[i]| * |noisy[i]|)) / N)^2
  where N is the number of samples.
- **THD**:
  THD (%) = 100 * sqrt(sum(harmonic[i]^2) / fundamental^2)
  THD (dB) = 20 * log10(THD / 100)
- **Phase Difference**:
  delta_phase = atan2(rightFFT[fundamental].imag, rightFFT[fundamental].real) -
                atan2(leftFFT[fundamental].imag, leftFFT[fundamental].real)
- **Q-Learning Update**:
  Q(s, a) = Q(s, a) + alpha * (reward + gamma * max(Q(nextState, a')) - Q(s, a))
- **Mean Squared Error (MSE)**:
  MSE = sum((generatedDB[i] - targetDB[i])^2) / N
  where generatedDB = 20 * log10(magnitude[i] + 1e-10).

---

| ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(1).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(2).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(3).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(4).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(5).png) |
|----|----|----|----|----|
| ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(6).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(7).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(8).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(9).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(10).png) |
| ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(11).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(12).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(13).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(14).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(15).png) |
| ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(16).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(17).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(18).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(19).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(20).png) |
| ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(21).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(22).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(23).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(24).png) | ![](https://raw.githubusercontent.com/KMORaza/Sweep_Generation_Software_2/main/Sweep%20Generation%20Software/src/screenshots/screen%20(25).png) |
