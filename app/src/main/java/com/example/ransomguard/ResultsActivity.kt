
package com.example.ransomguard

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class ResultsActivity : AppCompatActivity() {

    private lateinit var totalText: TextView
    private lateinit var precisionText: TextView
    private lateinit var recallText: TextView

    private lateinit var tpText: TextView
    private lateinit var fpText: TextView
    private lateinit var tnText: TextView
    private lateinit var fnText: TextView

    private lateinit var accuracyText: TextView
    private lateinit var averageDelayText: TextView
    private lateinit var averageFilesText: TextView
    private lateinit var historyText: TextView

    private lateinit var refreshButton: Button
    private lateinit var clearHistoryButton: Button
    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_results)

        initializeViews()

        backButton.setOnClickListener {
            finish()
        }

        refreshButton.setOnClickListener {
            loadResults()
        }

        clearHistoryButton.setOnClickListener {
            confirmClearHistory()
        }

        loadResults()
    }

    private fun initializeViews() {

        totalText = findViewById(R.id.totalText)
        precisionText = findViewById(R.id.precisionText)
        recallText = findViewById(R.id.recallText)

        tpText = findViewById(R.id.tpText)
        fpText = findViewById(R.id.fpText)
        tnText = findViewById(R.id.tnText)
        fnText = findViewById(R.id.fnText)

        accuracyText = findViewById(R.id.accuracyText)
        averageDelayText = findViewById(R.id.averageDelayText)
        averageFilesText = findViewById(R.id.averageFilesText)
        historyText = findViewById(R.id.historyText)

        refreshButton =
            findViewById(R.id.refreshResultsButton)

        clearHistoryButton =
            findViewById(R.id.clearHistoryButton)

        backButton =
            findViewById(R.id.backButton)
    }

    private fun confirmClearHistory() {

        AlertDialog.Builder(this)
            .setTitle("Clear experiment history?")
            .setMessage(
                "All saved experiments and evaluation metrics will be reset."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear") { _, _ ->

                ResultsStorage.clearResults(this)

                loadResults()

                Toast.makeText(
                    this,
                    "Experiment history cleared.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun loadResults() {

        refreshButton.isEnabled = false

        try {

            val results = ResultsStorage.getResults(this)
            val newestFirstResults = results.reversed()

            val summary =
                EvaluationEngine.calculate(results)

            val latestDetectedAttack =
                newestFirstResults.firstOrNull {
                    it.expected == "attack" && it.detected
                }

            if (latestDetectedAttack != null) {

                averageDelayText.text =
                    latestDetectedAttack.detectionDelayMs
                        ?.let { "$it ms" } ?: "N/A"

                averageFilesText.text =
                    "${latestDetectedAttack.filesChangedBeforeAlert} files"

            } else {

                averageDelayText.text = "N/A"
                averageFilesText.text = "N/A"
            }

            totalText.text =
                "Total experiments: ${summary.total}"

            precisionText.text =
                String.format(
                    Locale.getDefault(),
                    "Precision: %.2f%%",
                    summary.precision
                )

            recallText.text =
                String.format(
                    Locale.getDefault(),
                    "Recall: %.2f%%",
                    summary.recall
                )

            tpText.text =
                "True Positives: ${summary.truePositives}"

            fpText.text =
                "False Positives: ${summary.falsePositives}"

            tnText.text =
                "True Negatives: ${summary.trueNegatives}"

            fnText.text =
                "False Negatives: ${summary.falseNegatives}"

            accuracyText.text =
                String.format(
                    Locale.getDefault(),
                    "%.2f%%",
                    summary.accuracy
                )

            displayHistory(newestFirstResults)

        } catch (e: Exception) {

            totalText.text = "Unable to load results"
            historyText.text = "Error: ${e.message}"

            android.util.Log.e(
                "RansomGuard",
                "Local results loading failed",
                e
            )

        } finally {
            refreshButton.isEnabled = true
        }
    }

    private fun displayHistory(
        results: List<ExperimentResult>
    ) {

        if (results.isEmpty()) {
            historyText.text =
                "No experiments recorded yet."
            return
        }

        val history = StringBuilder()

        results.forEachIndexed { index, result ->

            val outcome = when {

                result.expected == "attack" && result.detected ->
                    "DETECTED"

                result.expected == "attack" && !result.detected ->
                    "MISSED"

                result.expected == "benign" && result.detected ->
                    "FALSE ALERT"

                else -> "NORMAL"
            }

            history.append("${index + 1}. ")
            history.append(result.workload)
            history.append("\n")

            history.append("Type: ${result.expected.uppercase()}\n")
            history.append("Outcome: $outcome\n")

            history.append(
                "Files changed: ${result.filesChangedBeforeAlert}\n"
            )

            val delay = result.detectionDelayMs

            history.append(
                if (delay != null) {
                    "Detection delay: $delay ms"
                } else {
                    "Detection delay: N/A"
                }
            )

            history.append("\n\n")
        }

        historyText.text = history.toString().trim()
    }
}
