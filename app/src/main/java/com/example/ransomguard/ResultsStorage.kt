package com.example.ransomguard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ExperimentResult(
    val workload: String,
    val expected: String,
    val detected: Boolean,
    val detectionDelayMs: Long?,
    val filesChangedBeforeAlert: Int
)

object ResultsStorage {

    private const val PREFS_NAME = "ransomguard_results"
    private const val RESULTS_KEY = "experiments"

    @Synchronized
    fun saveResult(
        context: Context,
        result: ExperimentResult
    ) {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val results = JSONArray(
            prefs.getString(RESULTS_KEY, "[]")
        )

        val item = JSONObject().apply {
            put("workload", result.workload)
            put("expected", result.expected)
            put("detected", result.detected)
            put(
                "detectionDelayMs",
                result.detectionDelayMs ?: JSONObject.NULL
            )
            put(
                "filesChangedBeforeAlert",
                result.filesChangedBeforeAlert
            )
        }

        results.put(item)

        prefs.edit()
            .putString(RESULTS_KEY, results.toString())
            .apply()
    }

    @Synchronized
    fun getResults(context: Context): List<ExperimentResult> {

        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val json = JSONArray(
            prefs.getString(RESULTS_KEY, "[]")
        )

        val results = mutableListOf<ExperimentResult>()

        for (i in 0 until json.length()) {

            val item = json.getJSONObject(i)

            val delayValue =
                item.opt("detectionDelayMs")

            val delay =
                if (delayValue == null ||
                    delayValue == JSONObject.NULL
                ) {
                    null
                } else {
                    (delayValue as Number).toLong()
                }

            results.add(
                ExperimentResult(
                    workload = item.getString("workload"),
                    expected = item.getString("expected"),
                    detected = item.getBoolean("detected"),
                    detectionDelayMs = delay,
                    filesChangedBeforeAlert =
                        item.optInt("filesChangedBeforeAlert", 0)
                )
            )
        }

        return results
    }

    fun clearResults(context: Context) {

        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .remove(RESULTS_KEY)
            .apply()
    }
}