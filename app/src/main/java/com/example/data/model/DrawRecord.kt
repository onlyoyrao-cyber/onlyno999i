package com.example.data.model

data class DrawRecord(
    val period: String,
    val dateStr: String,
    val numbers: List<Int>,
    val timestamp: Long = System.currentTimeMillis()
)

data class TriggerInfo(
    val triggered: Boolean,
    val periodN: String = "",
    val periodNMinus2: String = "",
    val targetNumberX: Int = 0,
    val positionIndexZeroBased: Int = 0,
    val position1Based: Int = 0,
    val sandwichNumberY: Int = 0,
    val basePositionP: Int = 0,
    val targetPositions1Based: List<Int> = emptyList(),
    val circularPathNumbersInN: List<Int> = emptyList(),
    val totalPositions: Int = 7
)

data class PredictionResult(
    val nextPeriod: String,
    val predictedExcludedNumbers: List<Int>, // 6 numbers least likely to appear
    val confidenceScore: Int,
    val triggerInfo: TriggerInfo,
    val coldHotSummary: String,
    val algorithmNotes: List<String>
)

data class BacktestRecord(
    val targetPeriod: String,
    val actualNumbers: List<Int>,
    val predictedExcludedNumbers: List<Int>,
    val hitExcludedNumbersInDraw: List<Int>, // Numbers among predicted 6 that appeared in actual draw (0 = Perfect Success)
    val isPerfectSuccess: Boolean,
    val accuracyPercentage: Float,
    val triggerSummary: String
)

data class FrequencyStat(
    val number: Int,
    val frequency30: Int,
    val currentOmission: Int,
    val maxOmission: Int,
    val isHot: Boolean,
    val isCold: Boolean
)

enum class BufferStatus {
    PENDING,      // ⏳ 待开奖 (Next upcoming period queued in buffer)
    HIT_SUCCESS,  // ✅ 100% 成功 (0 predicted excluded numbers appeared in draw)
    HIT_WARNING   // ⚠️ 命中/偏离 (1 or more predicted excluded numbers appeared)
}

data class BufferedPredictionRecord(
    val period: String,
    val predictedExcludedNumbers: List<Int>,
    val actualNumbers: List<Int>?,
    val hitExcludedNumbers: List<Int>,
    val status: BufferStatus,
    val hitCount: Int,
    val isPending: Boolean = (actualNumbers == null)
)

data class BufferSummary(
    val totalBuffered: Int = 0,
    val evaluatedCount: Int = 0,
    val pendingCount: Int = 0,
    val successCount: Int = 0,
    val hitWarningCount: Int = 0,
    val successRatePercentage: Float = 0f
)

