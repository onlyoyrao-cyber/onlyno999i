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

    val bufferedPredictions: StateFlow<List<BufferedPredictionRecord>> = combine(allDraws, targetPoolSize, predictionResult) { draws, poolSize, predRes ->
        if (draws.isEmpty()) emptyList()
        else {
            val all10 = AnalyzerEngine.generate10PeriodPredictionBuffer(
                draws = draws,
                bufferSize = 10,
                maxPool = poolSize,
                overridePendingNumbers = predRes?.predictedExcludedNumbers
            )
            
            val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
            var installPeriod = prefs.getString("install_period", null)
            
            if (installPeriod == null) {
                val nextPeriod = draws.maxByOrNull { it.period }?.let { AnalyzerEngine.deriveNextPeriod(it.period) } ?: "2026001"
                installPeriod = nextPeriod
                prefs.edit().putString("install_period", installPeriod).apply()
            }
            
            all10.filter { it.period >= installPeriod }
        }
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

            // Automatically fetch and sync latest remote data on every startup
            _isRefreshing.value = true
            val syncResult = repository.refreshFromRemote(_remoteUrl.value)
            _isRefreshing.value = false

            syncResult.onSuccess { count ->
                if (count > 0) {
                    _toastMessage.value = "⚡ 【开机自动抓取】同步成功！获取到 $count 期最新数据"
                }
            }

            checkAndPerformV12FirstLaunchBacktest()
        }
    }

    private suspend fun checkAndPerformV12FirstLaunchBacktest() {
        val prefs = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        val hasRunFirstBacktest = prefs.getBoolean("v1_2_forced_first_backtest_done", false)

        if (!hasRunFirstBacktest) {
            val draws = allDraws.value
            if (draws.size >= 2) {
                val sorted = draws.sortedByDescending { it.period }
                val latestDraw = sorted.first()
                val priorDraws = sorted.drop(1)

                val pred = AnalyzerEngine.predict6ExcludedNumbers(priorDraws, maxNumberPool = _targetPoolSize.value)
                val actual = latestDraw.numbers
                val hitExcluded = pred.predictedExcludedNumbers.filter { actual.contains(it) }

                val statusText = if (hitExcluded.isEmpty()) "100%成功避开(0命中)" else "包含命中: ${hitExcluded.joinToString(",")}"
                _toastMessage.value = "【v1.2 首次安装】强行回测第 ${latestDraw.period} 期完成: $statusText"
                prefs.edit().putBoolean("v1_2_forced_first_backtest_done", true).apply()
            }
        }
    }

    fun runForcedSinglePeriodBacktest() {
        viewModelScope.launch {
            val draws = allDraws.value
            if (draws.size < 2) {
                _toastMessage.value = "历史开奖数据不足，无法执行单期强行回测"
                return@launch
            }
            val sorted = draws.sortedByDescending { it.period }
            val latestDraw = sorted.first()
            val priorDraws = sorted.drop(1)

            val pred = AnalyzerEngine.predict6ExcludedNumbers(priorDraws, maxNumberPool = _targetPoolSize.value)
            val actual = latestDraw.numbers
            val hitExcluded = pred.predictedExcludedNumbers.filter { actual.contains(it) }

            val statusText = if (hitExcluded.isEmpty()) "【完美排除】6杀号完全避开开奖号" else "【部分命中】杀号命中 ${hitExcluded.size} 个: ${hitExcluded.joinToString(",")}"
            _toastMessage.value = "【强行单期回测】第 ${latestDraw.period} 期推算6杀号[${pred.predictedExcludedNumbers.joinToString(",")}] vs 实际开奖[${actual.joinToString(",")}] 结果: $statusText"
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
                .take(7)

            if (period.isBlank() || nums.size < 7) {
                _toastMessage.value = "请输入有效的期数与7个开奖号码"
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
