package com.craysafe.reports

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.craysafe.api.ApiClient
import com.craysafe.api.models.ReportData
import com.craysafe.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ReportsViewModel(app: Application) : AndroidViewModel(app) {

    private val _reportData = MutableLiveData<ReportData?>()
    val reportData: LiveData<ReportData?> = _reportData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _downloadProgress = MutableLiveData<Int?>(null)
    val downloadProgress: LiveData<Int?> = _downloadProgress

    private val _downloadIndeterminate = MutableLiveData<Boolean>(false)
    val downloadIndeterminate: LiveData<Boolean> = _downloadIndeterminate

    // Now emits the saved filename (not a File handle)
    private val _downloadComplete = MutableLiveData<String?>()
    val downloadComplete: LiveData<String?> = _downloadComplete

    private var currentRange = "7d"
    private var tankId = -1

    fun loadReport(range: String, sessionManager: SessionManager) {
        currentRange = range
        tankId = sessionManager.getSelectedTankId()
        if (tankId == -1) {
            _error.value = "Select a tank first"
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val response = ApiClient.apiService.getTankReports("Bearer $token", tankId, range)
                _isLoading.value = false
                if (response.success) {
                    _reportData.value = response.data
                } else {
                    _error.value = response.message ?: "Failed to load reports"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    fun downloadPdf(sessionManager: SessionManager) {
        if (tankId == -1) {
            _error.value = "Select a tank first"
            return
        }

        _downloadProgress.value = 0
        _downloadComplete.value = null

        viewModelScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    _error.value = "Please login again"
                    _downloadProgress.value = null
                    return@launch
                }

                val body = ApiClient.apiService.downloadReportPdf("Bearer $token", tankId, currentRange)
                val total = body.contentLength()
                _downloadIndeterminate.postValue(total <= 0)

                val app = getApplication<Application>()
                val fileName = "craysafe_report_${tankId}_${currentRange}.pdf"

                val downloadStart = System.currentTimeMillis()

                val savedName = withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        saveViaMediaStore(app, fileName, body, total)
                    } else {
                        saveToLegacyDownloads(app, fileName, body, total)
                    }
                }

                // Ensures the overlay is visible for at least a moment (UX)
                val elapsed = System.currentTimeMillis() - downloadStart
                val minDuration = 1200L
                if (elapsed < minDuration) {
                    kotlinx.coroutines.delay(minDuration - elapsed)
                }

                _downloadProgress.postValue(100)
                _downloadComplete.postValue(savedName)
            } catch (e: Exception) {
                _error.value = "Download failed: ${e.message}"
                _downloadProgress.value = null
            }
        }
    }

    /**
     * Save via MediaStore (Android 10+).
     * No permission required — scoped storage handles it.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(
        context: Context,
        fileName: String,
        body: ResponseBody,
        total: Long
    ): String {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val uri: Uri = resolver.insert(collection, values)
            ?: throw IOException("Failed to create MediaStore entry")

        try {
            resolver.openOutputStream(uri)?.use { output ->
                body.byteStream().use { input ->
                    copyWithProgress(input, output, total)
                }
            } ?: throw IOException("Failed to open output stream")

            // Clear IS_PENDING so the file becomes visible to other apps
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            return fileName
        } catch (e: Exception) {
            // Clean up the pending entry if the write failed midway
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }

    /**
     * Legacy save (Android 7–9).
     * Writes to the public Downloads directory; MediaScanner then makes it visible.
     * Requires WRITE_EXTERNAL_STORAGE (granted at runtime by the Fragment).
     */
    @Suppress("DEPRECATION")
    private fun saveToLegacyDownloads(
        context: Context,
        fileName: String,
        body: ResponseBody,
        total: Long
    ): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val file = File(downloadsDir, fileName)
        file.outputStream().use { output ->
            body.byteStream().use { input ->
                copyWithProgress(input, output, total)
            }
        }

        // Makes the file visible in the Files app / Downloads app
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            null,
            null
        )

        return fileName
    }

    /**
     * Stream copy with throttled progress callbacks.
     * Posts to `_downloadProgress` only when the integer percentage changes.
     */
    private fun copyWithProgress(input: InputStream, output: OutputStream, total: Long) {
        val buffer = ByteArray(4 * 1024)
        var bytesRead: Long = 0
        var read: Int
        var lastPostedProgress = -1

        while (input.read(buffer).also { read = it } != -1) {
            output.write(buffer, 0, read)
            bytesRead += read

            if (total > 0) {
                val progress = ((bytesRead * 100) / total).toInt().coerceIn(0, 100)
                if (progress != lastPostedProgress) {
                    _downloadProgress.postValue(progress)
                    lastPostedProgress = progress
                }
            }
        }
        output.flush()
    }

    fun resetDownloadState() {
        _downloadProgress.value = null
        _downloadComplete.value = null
    }

    fun getCurrentRange() = currentRange
    fun getTankId() = tankId
}