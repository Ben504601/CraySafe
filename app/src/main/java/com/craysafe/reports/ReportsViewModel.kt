package com.craysafe.reports

import android.app.Application
import android.os.Environment
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
import java.io.File

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

    private val _downloadComplete = MutableLiveData<File?>()
    val downloadComplete: LiveData<File?> = _downloadComplete

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

                val dir = getApplication<Application>()
                    .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: getApplication<Application>().cacheDir

                val file = File(dir, "craysafe_report_${tankId}_${currentRange}.pdf")

                // ✅ downloadStart declared at the outer scope
                val downloadStart = System.currentTimeMillis()

                withContext(Dispatchers.IO) {
                    body.byteStream().use { input ->
                        file.outputStream().use { output ->
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
                    }

                    // Now downloadStart is accessible here
                    val elapsed = System.currentTimeMillis() - downloadStart
                    val minDuration = 1200L
                    if (elapsed < minDuration) {
                        kotlinx.coroutines.delay(minDuration - elapsed)
                    }
                }

                _downloadProgress.postValue(100)
                _downloadComplete.postValue(file)
            } catch (e: Exception) {
                _error.value = "Download failed: ${e.message}"
                _downloadProgress.value = null
            }
        }
    }

    fun resetDownloadState() {
        _downloadProgress.value = null
        _downloadComplete.value = null
    }

    fun getCurrentRange() = currentRange
    fun getTankId() = tankId
}