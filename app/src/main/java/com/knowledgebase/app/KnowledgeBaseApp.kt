package com.knowledgebase.app

import android.app.Application
import android.content.Context
import com.knowledgebase.app.data.repository.FileRepository
import com.knowledgebase.app.data.repository.PreferencesRepository
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class KnowledgeBaseApp : Application() {
    lateinit var fileRepository: FileRepository
        private set
    lateinit var preferences: PreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        fileRepository = FileRepository(this)
        preferences = PreferencesRepository(this)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val trace = stackTraceOf(throwable)
                crashLogFile(this).writeText(trace)
                // Also write to Download so Termux can read it for debugging
                java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    ),
                    "crash.txt"
                ).writeText(trace)
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Restore the persisted KB folder if one was selected previously
        preferences.getKbUri()?.let { uri ->
            fileRepository.setRoot(uri)
        }
    }

    companion object {
        private fun crashLogFile(context: Context): File = File(context.filesDir, "last_crash.txt")

        private fun stackTraceOf(t: Throwable): String {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            return sw.toString()
        }

        fun readCrashLog(context: Context): String? =
            crashLogFile(context).takeIf { it.exists() }?.readText()

        fun clearCrashLog(context: Context) {
            crashLogFile(context).delete()
        }
    }
}