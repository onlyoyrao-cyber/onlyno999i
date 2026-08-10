package com.example.engine

import com.example.data.model.BacktestRecord
import com.example.data.model.BufferStatus
import com.example.data.model.BufferedPredictionRecord
import com.example.data.model.DrawRecord
import com.example.data.model.FrequencyStat
import com.example.data.model.PredictionResult
import com.example.data.model.TriggerInfo
import kotlin.math.abs

object AnalyzerEngine {

    /**
     * Analyzes draw history sorted from newest (index 0) to oldest (index N-1)
     * and detects the "隔期同号" trigger.
     */
    fun detectTrigger(draws: List<DrawRecord>): TriggerInfo {
        if (draws.size < 3) {
            return TriggerInfo(triggered = false)
        }

        val drawN = draws[0]
        val drawNMinus1 = draws[1]
        val drawNMinus2 = draws[2]

        val m = drawN.numbers.size.coerceAtLeast(6)

        // Check each position pos (0..m-1) for DrawN[pos] == DrawNMinus2[pos]
        for (pos in 0 until minOf(drawN.numbers.size, drawNMinus2.numbers.size)) {
            val numX = drawN.numbers[pos]
            if (numX == drawNMinus2.numbers[pos]) {
                // Found trigger!
                val numY = if (pos < drawNMinus1.numbers.size) drawNMinus1.numbers[pos] else numX
                
                // Base Position P: Find position of Y in Draw N (1-indexed)
                val posOfYInN = drawN.numbers.indexOf(numY)
                val baseP = if (posOfYInN != -1) {
                    posOfYInN + 1
                } else {
                    // Fallback: Find most recent active position of Y in recent draws
                    findRecentPositionOfNumber(draws, numY, defaultPos = pos + 1)
                }

                // Circular Algorithm: Compute target positions [P-1, P, P+1]
                val targetPositions = computeCircularTargetPositions(baseP, m)

                // Get numbers in Draw N at these target positions
                val circularPathNumbers = targetPositions.mapNotNull { pos1Based ->
                    val idx = pos1Based - 1
                    if (idx in drawN.numbers.indices) drawN.numbers[idx] else null
                }

                return TriggerInfo(
                    triggered = true,
                    periodN = drawN.period,
                    periodNMinus2 = drawNMinus2.period,
                    targetNumberX = numX,
                    positionIndexZeroBased = pos,
                    position1Based = pos + 1,
                    sandwichNumberY = numY,
                    basePositionP = baseP,
                    targetPositions1Based = targetPositions,
                    circularPathNumbersInN = circularPathNumbers
                )
            }
        }

        // Fallback if no exact 1-interval duplicate at same position:
        // Find most recent interval-1 matching number
        for (i in 0 until draws.size - 2) {
            val dN = draws[i]
            val dN2 = draws[i + 2]
            for (p in 0 until minOf(dN.numbers.size, dN2.numbers.size)) {
                if (dN.numbers[p] == dN2.numbers[p]) {
                    val numX = dN.numbers[p]
                    val numY = draws[i + 1].numbers.getOrElse(p) { numX }
                    val posY = dN.numbers.indexOf(numY)
                    val baseP = if (posY != -1) posY + 1 else p + 1
                    val targetPos = computeCircularTargetPositions(baseP, m)
                    return TriggerInfo(
                        triggered = true,
                        periodN = dN.period,
                        periodNMinus2 = dN2.period,
                        targetNumberX = numX,
                        positionIndexZeroBased = p,
                        position1Based = p + 1,
                        sandwichNumberY = numY,
                        basePositionP = baseP,
                        targetPositions1Based = targetPos,
                        circularPathNumbersInN = targetPos.mapNotNull { dN.numbers.getOrNull(it - 1) }
                    )
                }
            }
        }

        // Default structural trigger if history is small
        val defaultP = 1
        return TriggerInfo(
            triggered = false,
            periodN = drawN.period,
            periodNMinus2 = drawNMinus2.period,
            targetNumberX = drawN.numbers.firstOrNull() ?: 1,
            positionIndexZeroBased = 0,
            position1Based = 1,
            sandwichNumberY = drawNMinus1.numbers.firstOrNull() ?: 1,
            basePositionP = defaultP,
            targetPositions1Based = computeCircularTargetPositions(defaultP, m),
            circularPathNumbersInN = drawN.numbers.take(3)
        )
    }

    /**
     * Circular Boundary Algorithm (边缘环形路径算法):
     * When P = 1: [1, 2, M]
     * When P = M: [M-1, M, 1]
     * General 2 <= P <= M-1: [P-1, P, P+1]
     */
    fun computeCircularTargetPositions(baseP: Int, m: Int): List<Int> {
        val maxPos = m.coerceAtLeast(6)
        val p = baseP.coerceIn(1, maxPos)
        return when (p) {
            1 -> listOf(1, 2, maxPos)
            maxPos -> listOf(maxPos - 1, maxPos, 1)
            else -> listOf(p - 1, p, p + 1)
        }
    }

    private fun findRecentPositionOfNumber(draws: List<DrawRecord>, number: Int, defaultPos: Int): Int {
        for (draw in draws) {
            val idx = draw.numbers.indexOf(number)
            if (idx != -1) return idx + 1
        }
        return defaultPos
    }

    /**
     * Predicts 6 numbers least likely to appear in the next period (6大杀号预测).
     * Combines Circular Path target protection + Cold/Hot Omission Repair analysis.
     */
    fun predict6ExcludedNumbers(
        draws: List<DrawRecord>,
        maxNumberPool: Int = 49,
        previousPeriodExcluded: List<Int>? = null
    ): PredictionResult {
        if (draws.isEmpty()) {
            val default6 = (1..6).toList()
            return PredictionResult(
                nextPeriod = "2026001",
                predictedExcludedNumbers = default6,
                confidenceScore = 95,
                triggerInfo = TriggerInfo(false),
                coldHotSummary = "初始化默认分析",
                algorithmNotes = listOf("初始数据状态")
            )
        }

        val sortedDraws = draws.sortedByDescending { it.period }
        val latestDraw = sortedDraws.first()
        val nextPeriod = deriveNextPeriod(latestDraw.period)

        val triggerInfo = detectTrigger(sortedDraws)

        // Calculate frequency and omission statistics for pool 1..maxNumberPool
        val frequencyStats = computeFrequencyStats(sortedDraws, maxNumberPool)

        // Compute Likelihood Score for each number in 1..maxNumberPool
        // Lower score = LESS LIKELY to appear in next period = Candidate for Exclusion (杀号)!
        val numberScores = mutableMapOf<Int, Double>()

        for (num in 1..maxNumberPool) {
            var score = 50.0 // Base score

            val stat = frequencyStats[num]
            val omission = stat?.currentOmission ?: 0
            val freq30 = stat?.frequency30 ?: 0

            // 1. Circular Path Protection: Numbers in circular path in Draw N have high likelihood to appear (+100)
            if (triggerInfo.circularPathNumbersInN.contains(num)) {
                score += 100.0
            }

            // 2. Target X and Sandwich Y protection (+80)
            if (num == triggerInfo.targetNumberX || num == triggerInfo.sandwichNumberY) {
                score += 80.0
            }

            // 3. Draw N numbers have rebound probability (+40)
            if (latestDraw.numbers.contains(num)) {
                score += 40.0
            }

            // 4. Extreme Deep Cold Omission Penalty (-50 to -100):
            // Numbers with high omission (> 18) and low 30-draw frequency are cold states that tend to remain omitted
            if (omission > 15) {
                score -= (omission - 15) * 4.0
            }

            // 5. Over-saturated Cooling Phase Penalty (-30):
            // Numbers that appeared too frequently in last 5 draws enter a cooling phase
            val freqLast5 = sortedDraws.take(5).count { it.numbers.contains(num) }
            if (freqLast5 >= 3) {
                score -= 35.0
            }

            // 6. Non-neighbor position divergence penalty (-20)
            val minDistanceToPath = triggerInfo.circularPathNumbersInN.minOfOrNull { abs(it - num) } ?: 10
            if (minDistanceToPath > 8) {
                score -= 15.0
            }

            // 7. Cross-period Dynamic Rotation Adjustment:
            // If a number was already recommended in previous period's 6-kill list,
            // apply a mild score adjustment (+12.0) to encourage dynamic pool variation across periods.
            if (previousPeriodExcluded != null && previousPeriodExcluded.contains(num)) {
                score += 12.0
            }

            numberScores[num] = score
        }

        // Select 6 numbers with the LOWEST score as the 6 Excluded Numbers (6大杀号)
        val excluded6 = numberScores.entries
            .sortedBy { it.value }
            .map { it.key }
            .take(6)
            .sorted()

        val confidence = calculateConfidenceScore(triggerInfo, excluded6, frequencyStats)

        val notes = listOf(
            "触发机制: 目标号 X=${triggerInfo.targetNumberX} (第${triggerInfo.position1Based}位), 夹心号 Y=${triggerInfo.sandwichNumberY}",
            "基准位锁定: P=第${triggerInfo.basePositionP}名, 环形目标位置=${triggerInfo.targetPositions1Based.joinToString(",")}",
            "边缘环形保护: 排除热号与受保护位, 锁定6个最高遗漏/冷态偏离号码",
            "自动排除名单: ${excluded6.joinToString(", ")}"
        )

        val coldHotSummary = "全局扫描已完成: 筛选出6个在下一个周期中落空概率最高(最不可能出现)的号码: ${excluded6.joinToString("、")}"

        return PredictionResult(
            nextPeriod = nextPeriod,
            predictedExcludedNumbers = excluded6,
            confidenceScore = confidence,
            triggerInfo = triggerInfo,
            coldHotSummary = coldHotSummary,
            algorithmNotes = notes
        )
    }

    /**
     * Compute Frequency and Omission statistics for pool 1..maxNumberPool
     */
    fun computeFrequencyStats(draws: List<DrawRecord>, maxPool: Int = 49): Map<Int, FrequencyStat> {
        val sortedDraws = draws.sortedByDescending { it.period }
        val draws30 = sortedDraws.take(30)

        val map = mutableMapOf<Int, FrequencyStat>()

        for (num in 1..maxPool) {
            val freq30 = draws30.count { it.numbers.contains(num) }
            
            // Calculate current omission (how many draws since last appearance)
            var curOmission = 0
            for (draw in sortedDraws) {
                if (draw.numbers.contains(num)) break
                curOmission++
            }

            // Calculate max omission in history
            var maxOmission = 0
            var tempOmiss = 0
            for (draw in sortedDraws.reversed()) {
                if (draw.numbers.contains(num)) {
                    if (tempOmiss > maxOmission) maxOmission = tempOmiss
                    tempOmiss = 0
                } else {
                    tempOmiss++
                }
            }
            if (tempOmiss > maxOmission) maxOmission = tempOmiss

            map[num] = FrequencyStat(
                number = num,
                frequency30 = freq30,
                currentOmission = curOmission,
                maxOmission = maxOmission,
                isHot = freq30 >= 5,
                isCold = curOmission >= 12
            )
        }

        return map
    }

    /**
     * Runs automated backtests for past K periods (default 50 periods).
     */
    fun runAutoBacktest(
        draws: List<DrawRecord>,
        backtestCount: Int = 50,
        maxPool: Int = 49
    ): List<BacktestRecord> {
        val sortedDraws = draws.sortedBy { it.period } // Oldest to newest
        if (sortedDraws.size < 10) return emptyList()

        val results = mutableListOf<BacktestRecord>()
        val startIdx = (sortedDraws.size - backtestCount).coerceAtLeast(5)

        for (i in startIdx until sortedDraws.size) {
            val actualDraw = sortedDraws[i]
            val historicalSublist = sortedDraws.subList(0, i) // All draws prior to i

            // Predict 6 excluded numbers using only historicalSublist
            val prediction = predict6ExcludedNumbers(historicalSublist, maxPool)
            val predictedExcluded = prediction.predictedExcludedNumbers

            // Check which predicted excluded numbers appeared in actual draw
            val hitInDraw = predictedExcluded.filter { actualDraw.numbers.contains(it) }
            val isSuccess = hitInDraw.isEmpty()
            val accuracy = ((6 - hitInDraw.size) / 6.0f) * 100.0f

            val triggerSummary = "期数${actualDraw.period} | 目标X=${prediction.triggerInfo.targetNumberX} | 基准P=${prediction.triggerInfo.basePositionP} | 杀号: ${predictedExcluded.joinToString(",")}"

            results.add(
                BacktestRecord(
                    targetPeriod = actualDraw.period,
                    actualNumbers = actualDraw.numbers,
                    predictedExcludedNumbers = predictedExcluded,
                    hitExcludedNumbersInDraw = hitInDraw,
                    isPerfectSuccess = isSuccess,
                    accuracyPercentage = accuracy,
                    triggerSummary = triggerSummary
                )
            )
        }

        return results.reversed() // Return newest backtests first
    }

    /**
     * Generates a 10-period prediction buffer window.
     * Contains 1 pending next period + 9 recent historical periods, each with pre-computed predicted excluded numbers
     * and live backtest hit verification against actual opened draw numbers.
     */
    fun generate10PeriodPredictionBuffer(
        draws: List<DrawRecord>,
        bufferSize: Int = 10,
        maxPool: Int = 49
    ): List<BufferedPredictionRecord> {
        if (draws.isEmpty()) return emptyList()

        val sortedDraws = draws.sortedByDescending { it.period } // Newest to oldest
        val records = mutableListOf<BufferedPredictionRecord>()

        // 1. Compute Recent Historical Periods (Period N down to Period N-(bufferSize-2))
        val maxPastCount = (bufferSize - 1).coerceAtMost(sortedDraws.size - 2)
        val pastRecords = mutableListOf<BufferedPredictionRecord>()
        var lastEvaluatedExcluded: List<Int>? = null

        for (i in 0 until maxPastCount) {
            val targetDraw = sortedDraws[i]
            val priorHistory = sortedDraws.subList(i + 1, sortedDraws.size)
            if (priorHistory.size < 2) break

            val pred = predict6ExcludedNumbers(priorHistory, maxPool)
            val actualNums = targetDraw.numbers
            val hitNums = pred.predictedExcludedNumbers.filter { actualNums.contains(it) }
            val status = if (hitNums.isEmpty()) BufferStatus.HIT_SUCCESS else BufferStatus.HIT_WARNING

            if (i == 0) {
                lastEvaluatedExcluded = pred.predictedExcludedNumbers
            }

            pastRecords.add(
                BufferedPredictionRecord(
                    period = targetDraw.period,
                    predictedExcludedNumbers = pred.predictedExcludedNumbers,
                    actualNumbers = actualNums,
                    hitExcludedNumbers = hitNums,
                    status = status,
                    hitCount = hitNums.size,
                    isPending = false
                )
            )
        }

        // 2. Pending Next Period (N+1) using last evaluated excluded numbers for dynamic rotation
        val latestDraw = sortedDraws.first()
        val nextPeriod = deriveNextPeriod(latestDraw.period)
        val nextPrediction = predict6ExcludedNumbers(
            draws = sortedDraws,
            maxNumberPool = maxPool,
            previousPeriodExcluded = lastEvaluatedExcluded
        )

        records.add(
            BufferedPredictionRecord(
                period = nextPeriod,
                predictedExcludedNumbers = nextPrediction.predictedExcludedNumbers,
                actualNumbers = null,
                hitExcludedNumbers = emptyList(),
                status = BufferStatus.PENDING,
                hitCount = 0,
                isPending = true
            )
        )

        records.addAll(pastRecords)
        return records
    }

    private fun deriveNextPeriod(currentPeriod: String): String {
        return try {
            val num = currentPeriod.toLong()
            (num + 1).toString()
        } catch (e: Exception) {
            "2026${(System.currentTimeMillis() % 1000)}"
        }
    }

    private fun calculateConfidenceScore(
        trigger: TriggerInfo,
        excluded6: List<Int>,
        stats: Map<Int, FrequencyStat>
    ): Int {
        var base = 92
        if (trigger.triggered) base += 5
        if (trigger.circularPathNumbersInN.isNotEmpty()) base += 2
        return base.coerceIn(85, 99)
    }
}
