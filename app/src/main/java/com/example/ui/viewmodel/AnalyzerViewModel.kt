package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BacktestRecord
import com.example.data.model.BufferStatus
import com.example.data.model.BufferSummary
import com.example.data.model.BufferedPredictionRecord
import com.example.data.model.DrawRecord
import com.example.data.model.FrequencyStat
import com.example.data.model.PredictionResult
import com.example.data.repository.DrawRepository
import com.example.engine.AnalyzerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BacktestSummary(
    val totalTested: Int = 0,
    val perfectHits: Int = 0,
    val partialHits: Int = 0,
    val failHits: Int = 0,
    val winRatePercentage: Float = 0f,
    val averageAccuracyPercentage: Float = 0f
)

class AnalyzerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = DrawRepository(db.drawDao())

    val allDraws: StateFlow<List<DrawRecord>> = repository.allDraws.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _targetPoolSize = MutableStateFlow(49)
    val targetPoolSize: StateFlow<Int> = _targetPoolSize.asStateFlow()

    private val _remoteUrl = MutableStateFlow("https://macaujc.ddcdn.cloudns.org/")
    val remoteUrl: StateFlow<String> = _remoteUrl.asStateFlow()

    val predictionResult: StateFlow<PredictionResult?> = combine(allDraws, targetPoolSize) { draws, poolSize ->
        if (draws.isEmpty()) null
        else AnalyzerEngine.predict6ExcludedNumbers(draws, maxNumberPool = poolSize)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val backtestRecords: StateFlow<List<BacktestRecord>> = combine(allDraws, targetPoolSize) { draws, poolSize ->
        if (draws.size < 10) emptyList()
        else AnalyzerEngine.runAutoBacktest(draws, backtestCount = 50, maxPool = poolSize)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val backtestSummary: StateFlow<BacktestSummary> = backtestRecords.combine(allDraws) { records, _ ->
        if (records.isEmpty()) {
            BacktestSummary()
        } else {
            val total = records.size
            val perfect = records.count { it.isPerfectSuccess }
            val avgAcc = records.map { it.accuracyPercentage }.average().toFloat()
            val winRate = (perfect.toFloat() / total.toFloat()) * 100.0f

            BacktestSummary(
                totalTested = total,
                perfectHits = perfect,
                partialHits = records.count { it.hitExcludedNumbersInDraw.size == 1 },
                failHits = records.count { it.hitExcludedNumbersInDraw.size >= 2 },
                winRatePercentage = winRate,
                averageAccuracyPercentage = avgAcc
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BacktestSummary()
    )

    val bufferedPredictions: StateFlow<List<BufferedPredictionRecord>> = combine(allDraws, targetPoolSize) { draws, poolSize ->
        if (draws.isEmpty()) emptyList()
        else AnalyzerEngine.generate10PeriodPredictionBuffer(draws, bufferSize = 10, maxPool = poolSize)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val bufferSummary: StateFlow<BufferSummary> = bufferedPredictions.combine(allDraws) { bufferList, _ ->
        if (bufferList.isEmpty()) {
            BufferSummary()
        } else {
            val total = bufferList.size
            val pending = bufferList.count { it.isPending }
            val evaluated = bufferList.filter { !it.isPending }
            val success = evaluated.count { it.status == BufferStatus.HIT_SUCCESS }
            val warning = evaluated.count { it.status == BufferStatus.HIT_WARNING }
            val rate = if (evaluated.isNotEmpty()) (success.toFloat() / evaluated.size.toFloat()) * 100.0f else 0f

            BufferSummary(
                totalBuffered = total,
                evaluatedCount = evaluated.size,
                pendingCount = pending,
                successCount = success,
                hitWarningCount = warning,
                successRatePercentage = rate
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BufferSummary()
    )

    val frequencyStats: StateFlow<Map<Int, FrequencyStat>> = combine(allDraws, targetPoolSize) { draws, poolSize ->
        if (draws.isEmpty()) emptyMap()
        else AnalyzerEngine.computeFrequencyStats(draws, maxPool = poolSize)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    fun refreshRemoteData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.refreshFromRemote(_remoteUrl.value)
            _isRefreshing.value = false
            result.onSuccess { count ->
                _toastMessage.value = "成功同步 $count 期最新数据"
            }.onFailure { err ->
                _toastMessage.value = "远程连接提示: ${err.message ?: "网络超时，已载入本地高精度历史数据"}"
            }
        }
    }

    fun addDrawRecord(period: String, numbersStr: String) {
        viewModelScope.launch {
            val nums = numbersStr.split(",", " ", "，")
                .mapNotNull { it.trim().toIntOrNull() }
                .take(6)

            if (period.isBlank() || nums.size < 6) {
                _toastMessage.value = "请输入有效的期数与6个开奖号码"
                return@launch
            }

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            val dateStr = sdf.format(java.util.Date())

            val record = DrawRecord(
                period = period.trim(),
                dateStr = dateStr,
                numbers = nums
            )
            repository.addDrawRecord(record)
            _toastMessage.value = "已手动录入第 $period 期开奖结果"
        }
    }

    fun deleteDraw(period: String) {
        viewModelScope.launch {
            repository.deleteDraw(period)
            _toastMessage.value = "已删除第 $period 期记录"
        }
    }

    fun resetToDefaultHistory() {
        viewModelScope.launch {
            repository.resetToDefaultHistory()
            _toastMessage.value = "已恢复100期标准基准开奖记录"
        }
    }

    fun updatePoolSize(size: Int) {
        _targetPoolSize.value = size
    }

    fun updateRemoteUrl(url: String) {
        if (url.isNotBlank()) {
            _remoteUrl.value = url.trim()
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
