
package com.example.ransomguard

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Toast
import android.os.FileObserver
import java.io.File
import java.util.ArrayDeque
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    // DIRECTORY AND MONITORING

    private lateinit var labDir: File
    private var fileObserver: FileObserver? = null

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var monitoring = false

    @Volatile
    private var attackStopped = false

    @Volatile
    private var alertShown = false

    // UI ELEMENTS

    private lateinit var statusText: TextView
    private lateinit var fileCountText: TextView
    private lateinit var riskText: TextView
    private lateinit var logText: TextView

    private lateinit var eventChart: EventChartView
    private lateinit var delayText: TextView
    private lateinit var changedFilesText: TextView

    private lateinit var monitorButton: Button
    private lateinit var resetButton: Button
    private lateinit var backupButton: Button
    private lateinit var editorButton: Button
    private lateinit var fastAttackButton: Button
    private lateinit var copyDeleteButton: Button
    private lateinit var slowAttackButton: Button
    private lateinit var resultsButton: Button

    // EVENT COUNTERS

    @Volatile
    private var createCount = 0

    @Volatile
    private var modifyCount = 0

    @Volatile
    private var renameCount = 0

    @Volatile
    private var deleteCount = 0

    private val eventLock = Any()

    private val changedFiles = mutableSetOf<String>()
    private val renamedFiles = mutableSetOf<String>()

    // Cumulative unique file tracking for detection
    private val createdFiles = mutableSetOf<String>()
    private val modifiedFiles = mutableSetOf<String>()
    private val deletedFiles = mutableSetOf<String>()

    private val alertLock = Any()

    private data class FileEvent(
        val time: Long,
        val type: String,
        val filename: String
    )

    // Shared analytics model for chart and alert.

    private data class FileAnalytics(
        val created: Int,
        val modified: Int,
        val renamed: Int,
        val deleted: Int
    )

    private val events = ArrayDeque<FileEvent>()
    private val eventWindowMs = 3000L

    // EXPERIMENT METRICS

    @Volatile
    private var workloadStartTime = 0L

    @Volatile
    private var detectionDelayMs = 0L

    @Volatile
    private var filesChangedBeforeAlert = 0

    @Volatile
    private var currentWorkload = ""

    @Volatile
    private var currentExpected = "benign"

    private val experimentFinished = AtomicBoolean(false)

    // ACTIVITY CREATION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        labDir = File(filesDir, "ranslab")

        initializeViews()
        createLabFiles()
        updateFileCount()
        updateStatus("SYSTEM NORMAL")

        log("RansomGuard initialized.")
        log("Laboratory directory: ${labDir.absolutePath}")

        monitorButton.setOnClickListener {
            if (monitoring) {
                stopMonitoring()
            } else {
                startMonitoring()
            }
        }

        resetButton.setOnClickListener {
            resetLab()
        }

        backupButton.setOnClickListener {
            runBackup()
        }

        editorButton.setOnClickListener {
            runEditor()
        }

        fastAttackButton.setOnClickListener {
            runFastAttack()
        }

        copyDeleteButton.setOnClickListener {
            runCopyDeleteAttack()
        }

        slowAttackButton.setOnClickListener {
            runSlowAttack()
        }

        resultsButton.setOnClickListener {
            startActivity(
                Intent(this, ResultsActivity::class.java)
            )
        }

        log("Ready for testing.")
    }

    // INITIALIZE UI

    private fun initializeViews() {

        statusText = findViewById(R.id.statusText)
            ?: error("Missing view: statusText")

        fileCountText = findViewById(R.id.fileCountText)
            ?: error("Missing view: fileCountText")

        riskText = findViewById(R.id.riskText)
            ?: error("Missing view: riskText")

        logText = findViewById(R.id.logText)
            ?: error("Missing view: logText")

        eventChart = findViewById(R.id.eventChart)
            ?: error("Missing view: eventChart")

        delayText = findViewById(R.id.delayText)
            ?: error("Missing view: delayText")

        changedFilesText = findViewById(R.id.changedFilesText)
            ?: error("Missing view: changedFilesText")

        monitorButton = findViewById(R.id.monitorButton)
            ?: error("Missing view: monitorButton")

        resetButton = findViewById(R.id.resetButton)
            ?: error("Missing view: resetButton")

        backupButton = findViewById(R.id.backupButton)
            ?: error("Missing view: backupButton")

        editorButton = findViewById(R.id.editorButton)
            ?: error("Missing view: editorButton")

        fastAttackButton = findViewById(R.id.fastAttackButton)
            ?: error("Missing view: fastAttackButton")

        copyDeleteButton = findViewById(R.id.copyDeleteButton)
            ?: error("Missing view: copyDeleteButton")

        slowAttackButton = findViewById(R.id.slowAttackButton)
            ?: error("Missing view: slowAttackButton")

        resultsButton = findViewById(R.id.resultsButton)
            ?: error("Missing view: resultsButton")
    }

    // LAB FILE CREATION

    private fun createLabFiles() {

        if (!labDir.exists()) {
            labDir.mkdirs()
        }

        val existingFiles =
            labDir.listFiles()?.count {
                it.name.startsWith("file_") &&
                        it.extension == "txt"
            } ?: 0

        if (existingFiles < 100) {

            for (i in 1..100) {

                val file = File(
                    labDir,
                    "file_$i.txt"
                )

                if (!file.exists()) {
                    file.writeText(
                        "RansomGuard dummy test file $i"
                    )
                }
            }
        }

        updateFileCount()
    }

    private fun updateFileCount() {

        val count = labDir.listFiles()?.size ?: 0

        handler.post {
            fileCountText.text = "Files monitored: $count"
        }
    }

    // MONITORING

    private fun startMonitoring() {

        if (monitoring) {
            log("Monitoring is already active.")
            return
        }

        if (!labDir.exists()) {
            labDir.mkdirs()
        }

        monitoring = true
        attackStopped = false
        alertShown = false

        fileObserver?.stopWatching()

        fileObserver = object : FileObserver(
            labDir.absolutePath,
            CREATE or
                    MODIFY or
                    DELETE or
                    MOVED_FROM or
                    MOVED_TO or
                    CLOSE_WRITE
        ) {

            override fun onEvent(event: Int, path: String?) {

                if (path == null) return

                val name = path

                when {
                    event and MOVED_TO != 0 -> {
                        recordEvent("RENAME", name)
                    }

                    event and MOVED_FROM != 0 -> {
                        recordEvent("RENAME", name)
                    }

                    event and CREATE != 0 -> {
                        recordEvent("CREATE", name)
                    }

                    event and DELETE != 0 -> {
                        recordEvent("DELETE", name)
                    }

                    event and MODIFY != 0 -> {
                        recordEvent("MODIFY", name)
                    }

                    event and CLOSE_WRITE != 0 -> {
                        // Not counted separately.
                    }
                }
            }
        }

        fileObserver?.startWatching()

        monitorButton.text = "STOP MONITORING"
        updateStatus("MONITORING ACTIVE")
        log("File monitoring started.")

        updateFileCount()
    }

    private fun stopMonitoring() {

        fileObserver?.stopWatching()
        fileObserver = null

        monitoring = false

        monitorButton.text = "START MONITORING"

        updateStatus("MONITORING STOPPED")
        log("Monitoring stopped.")
    }

    // EVENT RECORDING

    private fun canonicalFileKey(filename: String): String {
        return filename
            .removeSuffix(".locked")
            .removeSuffix(".encrypted")
    }

    private fun recordEvent(
        type: String,
        filename: String
    ) {

        if (!monitoring || alertShown) return

        val now = System.currentTimeMillis()
        val key = canonicalFileKey(filename)

        synchronized(eventLock) {

            when (type) {
                "CREATE" -> {
                    createCount++
                    createdFiles.add(key)
                }

                "MODIFY" -> {
                    modifyCount++
                    modifiedFiles.add(key)
                }

                "RENAME" -> {
                    if (renamedFiles.add(key)) {
                        renameCount++
                    }
                }

                "DELETE" -> {
                    deleteCount++
                    deletedFiles.add(key)
                }
            }

            changedFiles.add(key)

            events.addLast(
                FileEvent(
                    now,
                    type,
                    key
                )
            )

            while (
                events.isNotEmpty() &&
                now - events.first.time > eventWindowMs
            ) {
                events.removeFirst()
            }
        }

        handler.post {
            updateChart()
            updateFileCount()
            log("$type: $filename")
        }

        evaluateDetection(now)
    }

    // FILE ANALYTICS
    // Counts distinct filenames in the rolling detection window.

    private fun getFileAnalytics(): FileAnalytics {

        synchronized(eventLock) {

            val creates = mutableSetOf<String>()
            val modifies = mutableSetOf<String>()
            val renames = mutableSetOf<String>()
            val deletes = mutableSetOf<String>()

            for (event in events) {

                when (event.type) {
                    "CREATE" -> creates.add(event.filename)
                    "MODIFY" -> modifies.add(event.filename)
                    "RENAME" -> renames.add(event.filename)
                    "DELETE" -> deletes.add(event.filename)
                }
            }

            return FileAnalytics(
                created = creates.size,
                modified = modifies.size,
                renamed = renames.size,
                deleted = deletes.size
            )
        }
    }
    private fun getCumulativeFileAnalytics(): FileAnalytics {
        synchronized(eventLock) {
            return FileAnalytics(
                created = createdFiles.size,
                modified = modifiedFiles.size,
                renamed = renamedFiles.size,
                deleted = deletedFiles.size
            )
        }
    }

    // CHART UPDATE

    private fun updateChart() {

        val analytics = getFileAnalytics()

        eventChart.updateCounts(
            analytics.created,
            analytics.modified,
            analytics.renamed,
            analytics.deleted
        )
    }

    // DETECTION ENGINE

    private fun evaluateDetection(now: Long) {

        if (attackStopped || alertShown) return

        // Use cumulative unique file counts for detection.
        val analytics = getCumulativeFileAnalytics()

        val suspicious =
            (analytics.modified >= 15 &&
                    analytics.renamed >= 10) ||
                    (analytics.created >= 12 &&
                            analytics.deleted >= 12)

        val risk = when {
            suspicious -> 10
            analytics.modified >= 8 ||
                    analytics.renamed >= 8 -> 7
            analytics.modified >= 4 ||
                    analytics.created >= 8 -> 4
            else -> 0
        }

        handler.post {
            riskText.text = "Risk score: $risk/10"
        }

        if (suspicious) {
            triggerAlert(analytics)
        }
    }

    // RANSOMWARE ALERT

    private fun triggerAlert(analytics: FileAnalytics) {

        val shouldAlert = synchronized(alertLock) {

            if (alertShown) {
                false
            } else {
                alertShown = true
                attackStopped = true
                true
            }
        }

        if (!shouldAlert) return

        detectionDelayMs =
            (System.currentTimeMillis() - workloadStartTime)
                .coerceAtLeast(0L)

        synchronized(eventLock) {
            filesChangedBeforeAlert = changedFiles.size
        }

        handler.post {

            updateStatus("RANSOMWARE ALERT")

            delayText.text = "$detectionDelayMs ms"

            changedFilesText.text =
                filesChangedBeforeAlert.toString()

            riskText.text = "Risk score: 10/10"

            // Freeze chart at the exact snapshot shown in the alert.

            eventChart.updateCounts(
                analytics.created,
                analytics.modified,
                analytics.renamed,
                analytics.deleted
            )

            log("RANSOMWARE ALERT DETECTED!")

            showRansomwareAlert(analytics)
        }
    }

    private fun showRansomwareAlert(
        analytics: FileAnalytics
    ) {

        AlertDialog.Builder(this)
            .setTitle("Ransomware-like activity detected!")
            .setMessage(
                """
                Suspicious activity detected.

                FILE ANALYTICS

                Created files: ${analytics.created}
                Modified files: ${analytics.modified}
                Renamed files: ${analytics.renamed}
                Deleted files: ${analytics.deleted}

                Detection delay: ${detectionDelayMs} ms

                Files changed before alert:
                $filesChangedBeforeAlert

                Controlled simulation stopped.
                """.trimIndent()
            )
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("OK", null)
            .setCancelable(false)
            .show()
    }

    // BENIGN BACKUP SIMULATION

    private fun runBackup() {

        if (!prepareExperiment("benign_backup", "benign")) {
            return
        }

        log("Starting benign backup simulation.")

        executor.execute {

            for (i in 1..50) {

                if (attackStopped) break

                val file = File(
                    labDir,
                    "backup_$i.txt"
                )

                file.writeText("Backup test data $i")

                Thread.sleep(100)
            }

            finishExperiment()

            handler.post {
                log("Backup simulation completed.")
            }
        }
    }

    // BENIGN EDITOR SIMULATION

    private fun runEditor() {

        if (!prepareExperiment("benign_editor", "benign")) {
            return
        }

        log("Starting benign editor simulation.")

        executor.execute {

            val file = File(
                labDir,
                "editor_test.txt"
            )

            for (i in 1..20) {

                if (attackStopped) break

                file.writeText(
                    "Normal editor modification $i"
                )

                Thread.sleep(300)
            }

            finishExperiment()

            handler.post {
                log("Editor simulation completed.")
            }
        }
    }

    // FAST ATTACK SIMULATION

    private fun runFastAttack() {

        if (!prepareExperiment("attack_fast", "attack")) {
            return
        }

        log("Starting FAST attack simulation.")

        executor.execute {

            val files =
                labDir.listFiles()
                    ?.filter {
                        it.name.matches(
                            Regex("file_\\d+\\.txt")
                        )
                    } ?: emptyList()

            for (file in files) {

                if (attackStopped) break

                try {

                    file.writeText(
                        "SIMULATED ENCRYPTED DATA"
                    )

                    val renamed = File(
                        labDir,
                        file.name + ".locked"
                    )

                    file.renameTo(renamed)

                    Thread.sleep(80)

                } catch (e: Exception) {

                    android.util.Log.e(
                        "RansomGuard",
                        "Fast simulation error",
                        e
                    )
                }
            }

            finishExperiment()

            handler.post {
                log("Fast attack simulation completed.")
            }
        }
    }

    // COPY-DELETE ATTACK SIMULATION

    private fun runCopyDeleteAttack() {

        if (!prepareExperiment(
                "attack_copy_delete",
                "attack"
            )
        ) {
            return
        }

        log("Copy-delete attack started")

        executor.execute {

            val files =
                labDir.listFiles()
                    ?.filter {
                        it.name.matches(
                            Regex("file_\\d+\\.txt")
                        )
                    } ?: emptyList()

            log("Total files selected: ${files.size}")
            for (file in files) {

                if (attackStopped) break

                try {

                    val encrypted = File(
                        labDir,
                        file.name + ".encrypted"
                    )

                    encrypted.writeText(
                        "SIMULATED ENCRYPTED DATA"
                    )

                    file.delete()

                    Thread.sleep(100)

                } catch (e: Exception) {

                    android.util.Log.e(
                        "RansomGuard",
                        "Copy-delete error",
                        e
                    )
                }
            }

            finishExperiment()

            handler.post {
                log("Copy-delete simulation completed.")
            }
        }
    }

    // SLOW ATTACK SIMULATION

    private fun runSlowAttack() {

        if (!prepareExperiment("attack_slow", "attack")) {
            return
        }

        log("Starting SLOW attack simulation.")

        executor.execute {

            // Select the original lab files.
            val allFiles = labDir.listFiles()
                ?.filter {
                    it.name.matches(Regex("file_\\d+\\.txt"))
                }
                ?.shuffled()
                ?: emptyList()

            // Vary the workload size for this run.
            val workloadSize = if (allFiles.isEmpty()) {
                0
            } else {
                kotlin.random.Random.nextInt(
                    35,
                    minOf(86, allFiles.size + 1)
                )
            }

            // Use only the selected files.
            val files = allFiles.take(workloadSize)

            // Vary the delay between file operations.
            val delayPerFile = kotlin.random.Random.nextLong(
                120L,
                351L
            )

            log("Slow attack configuration: $workloadSize files, ${delayPerFile}ms delay")

            var processedFiles = 0

            for (file in files) {

                if (attackStopped) break

                try {

                    file.writeText(
                        "SIMULATED SLOW ENCRYPTED DATA"
                    )

                    val renamed = File(
                        labDir,
                        file.name + ".locked"
                    )

                    if (file.renameTo(renamed)) {
                        processedFiles++
                    }

                    Thread.sleep(delayPerFile)

                } catch (e: Exception) {

                    android.util.Log.e(
                        "RansomGuard",
                        "Slow simulation error",
                        e
                    )
                }
            }

            finishExperiment()

            handler.post {
                log(
                    "Slow attack completed. " +
                            "Files successfully renamed: $processedFiles " +
                            "of $workloadSize."
                )
            }
        }
    }

    // EXPERIMENT INITIALIZATION

    private fun prepareExperiment(
        workload: String,
        expected: String
    ): Boolean {

        if (!monitoring) {

            Toast.makeText(
                this,
                "Start monitoring first.",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        if (!experimentFinished.get() &&
            currentWorkload.isNotEmpty()
        ) {

            Toast.makeText(
                this,
                "An experiment is already running.",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        synchronized(eventLock) {

            createCount = 0
            modifyCount = 0
            renameCount = 0
            deleteCount = 0

            changedFiles.clear()
            renamedFiles.clear()
            createdFiles.clear()
            modifiedFiles.clear()
            deletedFiles.clear()
            events.clear()
        }

        synchronized(alertLock) {
            alertShown = false
            attackStopped = false
        }

        detectionDelayMs = 0L
        filesChangedBeforeAlert = 0

        workloadStartTime = System.currentTimeMillis()

        currentWorkload = workload
        currentExpected = expected

        experimentFinished.set(false)

        handler.post {

            delayText.text = "-- ms"
            changedFilesText.text = "0"
            riskText.text = "Risk score: 0/10"

            updateChart()
        }

        log("Experiment started: $workload")

        return true
    }

    // EXPERIMENT FINISH

    private fun finishExperiment() {

        if (!experimentFinished.compareAndSet(
                false,
                true
            )
        ) {
            return
        }

        val wasDetected = alertShown

        synchronized(eventLock) {

            if (!wasDetected) {
                filesChangedBeforeAlert = changedFiles.size
            }
        }

        if (!wasDetected) {

            detectionDelayMs = 0L

            handler.post {

                delayText.text = "Not detected"

                changedFilesText.text =
                    filesChangedBeforeAlert.toString()
            }
        }

        saveExperimentResult(wasDetected)

        handler.post {

            if (!wasDetected) {
                log("Experiment finished without alert.")
            }

            updateFileCount()
        }

        currentWorkload = ""
    }

    private fun saveExperimentResult(detected: Boolean) {

        val delay = if (detected) {
            detectionDelayMs
        } else {
            null
        }

        val result = ExperimentResult(
            workload = currentWorkload,
            expected = currentExpected,
            detected = detected,
            detectionDelayMs = delay,
            filesChangedBeforeAlert = filesChangedBeforeAlert
        )

        ResultsStorage.saveResult(
            applicationContext,
            result
        )

        log("Experiment saved locally on Android.")
    }

    // RESET LAB

    private fun resetLab() {

        stopMonitoring()

        attackStopped = true
        experimentFinished.set(true)

        currentWorkload = ""

        labDir.listFiles()?.forEach {
            it.deleteRecursively()
        }

        synchronized(eventLock) {

            events.clear()

            changedFiles.clear()
            renamedFiles.clear()

            createCount = 0
            modifyCount = 0
            renameCount = 0
            deleteCount = 0
        }

        detectionDelayMs = 0L
        filesChangedBeforeAlert = 0

        alertShown = false

        createLabFiles()
        updateChart()

        delayText.text = "-- ms"
        changedFilesText.text = "--"

        riskText.text = "Risk score: 0/10"

        updateStatus("SYSTEM NORMAL")

        log("Laboratory reset completed.")

        startMonitoring()
    }

    // STATUS AND LOG

    private fun updateStatus(message: String) {

        handler.post {
            statusText.text = message
        }
    }

    private fun log(message: String) {

        handler.post {

            val previous = logText.text.toString()

            logText.text = if (previous.isBlank()) {
                message
            } else {
                "$message\n$previous"
            }
        }
    }

    // ACTIVITY CLEANUP

    override fun onDestroy() {

        fileObserver?.stopWatching()
        fileObserver = null

        monitoring = false

        executor.shutdownNow()

        super.onDestroy()
    }
}
