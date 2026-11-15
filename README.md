
# RansomGuard – Android Ransomware Early Warning System

**Team Kenshin — A3-PS001-TC228**

RansomGuard is a Kotlin-based Android laboratory prototype that monitors file activity in a designated app-private test directory and raises an alert when activity matches predefined ransomware-like patterns.

> **Scope:** RansomGuard is a controlled research/demo prototype. It monitors only its own app-private lab directory; it is not device-wide Android protection and does not detect or stop real ransomware on a device.

## 1. Project Overview

Ransomware commonly performs repeated file operations, such as modifying, renaming, replacing, or deleting files. RansomGuard demonstrates how event-driven file monitoring and rule-based analysis can identify suspicious patterns during controlled simulations.

The application creates a lab directory in its private internal storage (`filesDir/ranslab`), populates it with 100 dummy files, monitors file-system events using Android `FileObserver`, runs two benign and three ransomware-like workload simulations, displays alerts, and saves experiment results locally.

## 2. Project Objectives

- Build a controlled Android ransomware-simulation environment.
- Generate 100 dummy files for safe testing.
- Simulate normal file activity and ransomware-like behavior.
- Monitor file creation, modification, deletion, and rename-related events.
- Detect suspicious activity using configurable rule-based thresholds.
- Display an alert when detection conditions are met.
- Cooperatively stop the controlled simulation after an alert.
- Record detection delay, files changed before alert, and evaluation metrics.

## 3. System Architecture

### Android Application and Lab Directory

The app uses its private internal storage. The lab directory is `filesDir/ranslab/` and contains 100 dummy files. Simulations operate on these test files rather than personal documents.

### Workload Simulations

The app includes five controlled workloads:

1. Benign Backup
2. Benign Editor
3. Fast Attack
4. Copy-Delete Attack
5. Slow Attack

### File Monitoring

Android `FileObserver` monitors the designated lab directory for:

- `CREATE`
- `MODIFY`
- `DELETE`
- `MOVED_FROM`
- `MOVED_TO`
- `CLOSE_WRITE`

Rename-related activity is inferred from move events and the application's file tracking.

### Detection Engine

The detection engine tracks cumulative unique-file activity for detection. A separate rolling 3-second event window is used for recent activity analytics and chart display.

### Alert and Response

When a suspicious rule is satisfied, the app displays an alert, records detection metrics, and sets a shared stop flag. The controlled workload checks this flag and stops cooperatively.

**Important:** The app does not identify and terminate an independent attacker process or use PID-based process killing.

### Results and Storage

Experiment results are saved locally using Android `SharedPreferences` in JSON format. The Results screen calculates and displays confusion-matrix metrics and average detection measurements.

## 4. Technologies Used

| Technology | Purpose |
|---|---|
| Kotlin | Android application and workload implementation |
| Android Studio | Development and debugging |
| Android SDK / Android Emulator | Build and controlled test environment |
| Android `FileObserver` | Event-driven monitoring of the lab directory |
| SharedPreferences | Local experiment-history storage |
| JSON | Serialization of experiment results |
| Git and GitHub | Version control and collaboration |

## 5. Workload Descriptions

### 5.1 Benign Backup

Simulates legitimate backup-like activity by creating multiple files in the lab directory.

**Expected outcome:** No ransomware alert.

### 5.2 Benign Editor

Simulates normal editing by repeatedly modifying a file.

**Expected outcome:** No ransomware alert.

### 5.3 Fast Attack

Simulates rapid ransomware-like behavior by modifying files and renaming them with a `.locked` extension.

**Expected outcome:** Suspicious activity alert.

### 5.4 Copy-Delete Attack

Simulates replacement-style activity by creating `.encrypted` copies and deleting original files.

**Expected outcome:** Suspicious activity alert.

### 5.5 Slow Attack

Simulates slower file modifications and renaming. It selects a randomized workload of approximately 35–85 files, shuffles the selected files, and introduces randomized delays of approximately 120–350 ms between operations.

**Expected outcome:** Detection if cumulative activity reaches the configured rules.

**Slow Simulation Limitation:** Randomized delays make this workload slower, but it remains a controlled, rule-based simulation. It does not establish that the prototype can detect every real-world slow ransomware technique. The detector relies on cumulative unique-file activity and fixed thresholds; it does not currently use machine learning or entropy analysis.

All workloads operate only on dummy files in the app-private lab directory.

## 6. Detection Methodology

The current suspicious-activity conditions are:

```kotlin
val suspicious =
    (analytics.modified >= 15 && analytics.renamed >= 10) ||
    (analytics.created >= 12 && analytics.deleted >= 12)
```

An alert is triggered when either:

- At least 15 unique files have been modified AND at least 10 unique files have been renamed; OR
- At least 12 unique files have been created AND at least 12 unique files have been deleted.

### Risk Scoring

| Condition | Risk Score |
|---|---:|
| Suspicious detection rule satisfied | 10/10 |
| At least 8 modified files or 8 renamed files | 7/10 |
| At least 4 modified files or 8 created files | 4/10 |
| Otherwise | 0/10 |

These are experimental thresholds selected for the controlled lab workloads, not universal ransomware signatures.

### Monitoring Window vs Detection Tracking

- **Rolling Analytics:** A 3-second event window is used to display recent file activity in the chart.
- **Cumulative Detection Analytics:** Cumulative unique-file tracking is used by the detection engine to evaluate the workload.

The chart and detector therefore do not necessarily show identical counts at every moment.

## 7. Running the Project

1. Open the RansomGuard project in Android Studio.
2. Allow Gradle sync to complete.
3. Start an Android emulator.
4. Select the emulator as the run target.
5. Click **Run** in Android Studio, or install the generated APK.
6. Open RansomGuard.
7. Start monitoring and run the workloads from the app.
8. Open the Results screen to review saved experiment history and evaluation metrics.

Use **Reset Lab Files** to restore the lab files between demonstrations when needed.

Use **Clear Experiment History** to clear saved evaluation records.

## 8. Evaluation Methodology

Each experiment records:

- Workload name and expected classification.
- Whether an alert was detected.
- Detection delay, when detected.
- Number of files changed before the alert.

### Evaluation Metrics

**True Positive (TP):** An attack workload correctly detected.

**False Positive (FP):** A benign workload incorrectly detected as an attack.

**True Negative (TN):** A benign workload correctly completed without an alert.

**False Negative (FN):** An attack workload that was not detected.

**Precision:**

`TP / (TP + FP) × 100`

Measures the proportion of detected attack classifications that were correct.

**Recall:**

`TP / (TP + FN) × 100`

Measures the proportion of attack workloads that were detected.

**Accuracy:**

`(TP + TN) / Total Experiments × 100`

Measures the proportion of all experiment classifications that were correct.

**Average Detection Delay:** Average delay across detected attack experiments with a recorded delay.

**Average Files Changed Before Alert:** Average number of files changed before alert across detected attack experiments.

## 9. Initial Demonstration Results

The following results are from the initial demonstration of one run per workload. They are NOT the completed 25-run evaluation.

### 9.1 Workload Results

| Workload | Outcome | Detection Delay | Files Changed Before Alert |
|---|---|---:|---:|
| Benign Backup | No alert | — | — |
| Benign Editor | No alert | — | — |
| Fast Attack | Detected | 1,217 ms | 15 |
| Copy-Delete Attack | Detected | 1,145 ms | 12 |
| Slow Attack | Detected | 2,886 ms | 15 |

### 9.2 Initial Evaluation Summary

| Metric | Initial Result |
|---|---:|
| Total Experiments | 5 |
| True Positives | 3 |
| False Positives | 0 |
| True Negatives | 2 |
| False Negatives | 0 |
| Precision | 100% |
| Recall | 100% |
| Accuracy | 100% |
| Average Detection Delay | 1,749.33 ms |
| Average Files Changed Before Alert | 14.00 |

These results describe a small controlled demonstration only. A 100% score on five experiments does not establish performance against real-world ransomware or guarantee the same result in larger tests.

## 10. Next Milestones

- Complete the 25-run evaluation (five runs per workload).
- Evaluate detection consistency, false positives, and false negatives.
- Refine detection thresholds based on repeated experimental results.
- Test the final APK and improve application reliability.
- Report results from the completed evaluation separately from the initial demonstration.

## 11. Limitations

- Monitoring is limited to the app-private `filesDir/ranslab` directory.
- The prototype does not provide device-wide ransomware protection.
- Detection uses rule-based thresholds and cumulative unique-file tracking.
- The rolling 3-second chart analytics are separate from cumulative detection analytics.
- Slow Attack is a controlled simulation and does not represent every real-world slow ransomware technique.
- The simulations do not use real ransomware or perform real encryption.
- The response cooperatively stops the app's own controlled workload; it does not terminate an independent attacker process.
- Initial evaluation results are based on only five experiments. The planned 25-run evaluation remains outstanding.
- Thresholds may need retuning for different workloads and device performance.

## 12. Future Improvements

- Explore entropy-based analysis as an additional detection signal.
- Improve behavioural analysis and risk scoring.
- Expand controlled workload patterns and evaluation datasets.
- Evaluate detection consistency across repeated trials.
- Improve dashboard visualizations and experiment reporting.
- Investigate broader Android monitoring capabilities within Android's security and permission restrictions.

## 13. Safety and Ethical Considerations

All tests are conducted in a controlled Android application environment using dummy files stored in the app-private lab directory.

The simulations are intended for academic research, cybersecurity education, and controlled testing. They do not target personal documents or production data.

## Conclusion

RansomGuard demonstrates an Android-based, event-driven approach to identifying suspicious file activity in a controlled laboratory directory.

By combining `FileObserver`, rule-based detection, controlled workloads, alerts, and local experiment evaluation, the prototype provides a foundation for further study of Android ransomware early warning.

The initial five-experiment demonstration detected all three simulated attack workloads without alerting on the two benign workloads. Further repeated testing and development are required before drawing conclusions about broader detection reliability.

## Demo Video

Add the demo video to the repository and link it here. For example:

```markdown
[Watch the RansomGuard demo](./ransomguard.mp4)
```

Update the filename above to match the actual video filename committed to the repository.
