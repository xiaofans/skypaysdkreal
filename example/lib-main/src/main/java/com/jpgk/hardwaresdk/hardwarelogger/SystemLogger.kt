package com.jpgk.hardwaresdk.hardwarelogger

import android.content.Context
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

object SystemLogger {

    private var logThread: Thread? = null
    private var process: Process? = null
    private var isRunning = false

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var logDir: File
    /**
     * 開始記錄系統 logcat
     */
    @Synchronized
    fun start(context: Context, keepDays: Int = 30) {
        if (isRunning) {
            return
        }

        // 获取存储目录，比如 /data/data/<包名>/files/logs
        logDir = File(LocalLogsPathManager.getLogsNewPath(), "logs/system").apply { mkdirs() }
        cleanOldLogs(keepDays)

        try {
            val cmd = arrayOf("logcat", "-v", "time")
            process = Runtime.getRuntime().exec(cmd)

            logThread = thread(start = true, isDaemon = true) {
                var currentDate = getToday()
                var logFile = File(logDir, "$currentDate.log")
                var writer = BufferedWriter(FileWriter(logFile, true))

                process?.inputStream?.bufferedReader()?.use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val msg = line ?: continue

                        // 每次寫入前檢查日期是否變化
                        val today = getToday()
                        if (today != currentDate) {
                            writer.close()
                            cleanOldLogs(keepDays)
                            currentDate = today
                            logFile = File(logDir, "$currentDate.log")
                            writer = BufferedWriter(FileWriter(logFile, true))
                        }

                        writer.appendLine(msg)
                        writer.flush()
                    }
                }
                writer.close()
            }

            isRunning = true

        } catch (e: IOException) {
        }
    }

    /**
     * 停止記錄
     */
    @Synchronized
    fun stop() {
        try {
            process?.destroy()
            logThread?.interrupt()
            process = null
            logThread = null
            isRunning = false
        } catch (e: Exception) {
        }
    }

    private fun cleanOldLogs(keepDays: Int) {
        val cutoff = System.currentTimeMillis() - keepDays * 24 * 60 * 60 * 1000L
        logDir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    private fun getToday(): String {
        return dateFormat.format(Date())
    }
}
