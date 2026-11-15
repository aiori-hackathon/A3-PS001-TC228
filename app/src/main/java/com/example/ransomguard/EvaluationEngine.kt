
package com.example.ransomguard

data class EvaluationSummary(
    val total: Int,
    val truePositives: Int,
    val falsePositives: Int,
    val trueNegatives: Int,
    val falseNegatives: Int,
    val precision: Double,
    val recall: Double,
    val accuracy: Double,
    val averageDetectionDelayMs: Double,
    val averageFilesChangedBeforeAlert: Double
)

object EvaluationEngine {

    fun calculate(
        results: List<ExperimentResult>
    ): EvaluationSummary {

        var tp = 0
        var fp = 0
        var tn = 0
        var fn = 0

        for (result in results) {

            val isAttack = result.expected == "attack"
            val isDetected = result.detected

            when {
                isAttack && isDetected -> tp++
                !isAttack && isDetected -> fp++
                !isAttack && !isDetected -> tn++
                isAttack && !isDetected -> fn++
            }
        }

        val precision =
            if (tp + fp > 0) {
                tp.toDouble() / (tp + fp) * 100
            } else {
                0.0
            }

        val recall =
            if (tp + fn > 0) {
                tp.toDouble() / (tp + fn) * 100
            } else {
                0.0
            }

        val accuracy =
            if (results.isNotEmpty()) {
                (tp + tn).toDouble() / results.size * 100
            } else {
                0.0
            }

        val detectedResults = results.filter {
            it.expected == "attack" &&
                    it.detected &&
                    it.detectionDelayMs != null
        }

        val averageDelay =
            if (detectedResults.isNotEmpty()) {
                detectedResults.mapNotNull {
                    it.detectionDelayMs
                }.average()
            } else {
                0.0
            }

        val averageChanged =
            if (detectedResults.isNotEmpty()) {
                detectedResults.map {
                    it.filesChangedBeforeAlert.toDouble()
                }.average()
            } else {
                0.0
            }

        return EvaluationSummary(
            total = results.size,
            truePositives = tp,
            falsePositives = fp,
            trueNegatives = tn,
            falseNegatives = fn,
            precision = precision,
            recall = recall,
            accuracy = accuracy,
            averageDetectionDelayMs = averageDelay,
            averageFilesChangedBeforeAlert = averageChanged
        )
    }
}
