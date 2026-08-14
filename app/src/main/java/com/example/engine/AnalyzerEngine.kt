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
                    circularPathNumbersInN = circularPathNumbers,
                    totalPositions = m
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
                        circularPathNumbersInN = targetPos.mapNotNull { dN.numbers.getOrNull(it - 1) },
                        totalPositions = m
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
            circularPathNumbersInN = drawN.numbers.take(3),
            totalPositions = m
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

        // 蒙特卡洛暴力对撞引擎 (Monte Carlo Simulation)
        val past7Draws = sortedDraws.take(7)
        val historicalNumbers = past7Draws.flatMap { it.numbers }.toSet()
        
        var bestCandidates = mutableListOf<List<Int>>()
        var minHits = Int.MAX_VALUE

        val iterations = 50000
        // 固定随机种子：使用下一期和当前期的期号结合作为 Seed。
        // 这样保证同一个历史状态下，刷新多少次算出来的 50000 组随机结果都是绝对固定不变的。
        val seedString = "${nextPeriod}_${latestDraw.period}"
        val random = java.util.Random(seedString.hashCode().toLong())

        // 暴力对撞 50,000 次
        for (i in 0 until iterations) {
            val candidate = mutableSetOf<Int>()
            while (candidate.size < 6) {
                candidate.add(random.nextInt(maxNumberPool) + 1)
            }

            // 计算对撞“命中数”（命中历史记录的号码越少越好，0表示完美踩空）
            var hits = 0
            for (num in candidate) {
                if (historicalNumbers.contains(num)) {
                    hits++
                }
            }

            if (hits < minHits) {
                minHits = hits
                bestCandidates.clear()
                bestCandidates.add(candidate.toList().sorted())
            } else if (hits == minHits) {
                // 如果命中数一样低，也加入候选池，增加随机性
                // 为防止池子过大，限制只保存最多 100 组最优解
                if (bestCandidates.size < 100) {
                    bestCandidates.add(candidate.toList().sorted())
                }
            }
        }

        // 随机抽取其中一组最优解
        val excluded6 = if (bestCandidates.isNotEmpty()) {
            bestCandidates[random.nextInt(bestCandidates.size)]
        } else {
            (1..6).toList()
        }

        val triggerInfo = detectTrigger(sortedDraws) // 可以保留原有的特征分析供UI展示

        // 如果 Monte Carlo 实在选不出完美踩空（命中数大于0），则降级使用传统公式引擎
        if (minHits > 0) {
            val frequencyStats = computeFrequencyStats(sortedDraws, maxNumberPool)
            val numberScores = mutableMapOf<Int, Double>()

            for (num in 1..maxNumberPool) {
                var score = 50.0 // Base score

                val stat = frequencyStats[num]
                val omission = stat?.currentOmission ?: 0
                val freq30 = stat?.frequency30 ?: 0

                // 1. Circular Path Protection
                if (triggerInfo.circularPathNumbersInN.contains(num)) score += 100.0

                // 2. Target X and Sandwich Y protection
                if (num == triggerInfo.targetNumberX || num == triggerInfo.sandwichNumberY) score += 80.0

                // 3. Draw N numbers have rebound probability
                if (latestDraw.numbers.contains(num)) score += 40.0

                // 4. Extreme Deep Cold Omission Penalty
                if (omission > 15) score -= (omission - 15) * 4.0

                // 5. Over-saturated Cooling Phase Penalty
                val freqLast5 = sortedDraws.take(5).count { it.numbers.contains(num) }
                if (freqLast5 >= 3) score -= 35.0

                // 6. Non-neighbor position divergence penalty
                val minDistanceToPath = triggerInfo.circularPathNumbersInN.minOfOrNull { kotlin.math.abs(it - num) } ?: 10
                if (minDistanceToPath > 8) score -= 15.0

                // 7. Cross-period Dynamic Rotation Adjustment
                if (previousPeriodExcluded != null && previousPeriodExcluded.contains(num)) score += 12.0

                numberScores[num] = score
            }

            val traditionalExcluded6 = numberScores.entries
                .sortedBy { it.value }
                .map { it.key }
                .take(6)
                .sorted()

            return PredictionResult(
                nextPeriod = nextPeriod,
                predictedExcludedNumbers = traditionalExcluded6,
                confidenceScore = 75,
                triggerInfo = triggerInfo,
                coldHotSummary = "由于 AI 对撞引擎未能找到完美踩空的号码组合（最低碰撞 ${minHits} 次），已自动降级为【传统公式推演引擎】选号。基于冷热偏离度锁定 6 大杀号: ${traditionalExcluded6.joinToString("、")}",
                algorithmNotes = listOf(
                    "降级原因: Monte Carlo 对撞 ${iterations} 次均发生历史碰撞 (最低 ${minHits})，说明近期号码极度分散，无法暴力踩空。",
                    "触发机制: 目标号 X=${triggerInfo.targetNumberX}, 夹心号 Y=${triggerInfo.sandwichNumberY}",
                    "推演引擎: 传统固定公式引擎 (智能降级接管)",
                    "最终锁定杀号: ${traditionalExcluded6.joinToString(", ")}"
                )
            )
        }

        val confidence = if (minHits == 0) 99 else if (minHits == 1) 85 else 70
        
        val hitMsg = if (minHits == 0) "【完美踩空7期】" else "【极低历史碰撞】"

        val notes = listOf(
            "推演引擎: 蒙特卡洛暴力对撞 (Monte Carlo Simulation)",
            "计算量: 在 ${iterations} 次随机生成中暴力寻优",
            "对撞池: 回溯最近 ${past7Draws.size} 期历史开奖数据",
            "评估结果: 命中历史轨迹 ${minHits} 次 $hitMsg",
            "最终锁定杀号: ${excluded6.joinToString(", ")}"
        )

        val coldHotSummary = "系统通过 Monte Carlo 暴力引擎，从 ${iterations} 组模拟组合中提取出表现最冷、踩空率最高的6大杀号: ${excluded6.joinToString("、")}"

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
        maxPool: Int = 49,
        overridePendingNumbers: List<Int>? = null
    ): List<BufferedPredictionRecord> {
        if (draws.isEmpty()) return emptyList()

        val sortedDraws = draws.sortedByDescending { it.period } // Newest to oldest
        val records = mutableListOf<BufferedPredictionRecord>()

        // 1. Compute Recent Historical Periods (Period N down to Period N-(bufferSize-2))
        val maxPastCount = (bufferSize - 1).coerceAtMost(sortedDraws.size - 2)
        val pastRecords = mutableListOf<BufferedPredictionRecord>()

        for (i in 0 until maxPastCount) {
            val targetDraw = sortedDraws[i]
            val priorHistory = sortedDraws.subList(i + 1, sortedDraws.size)
            if (priorHistory.size < 2) break

            val pred = predict6ExcludedNumbers(priorHistory, maxPool)
            val actualNums = targetDraw.numbers
            val hitNums = pred.predictedExcludedNumbers.filter { actualNums.contains(it) }
            val status = if (hitNums.isEmpty()) BufferStatus.HIT_SUCCESS else BufferStatus.HIT_WARNING

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

        // 2. Pending Next Period (N+1): Strictly single calculation, matching top recommendation 100%
        val latestDraw = sortedDraws.first()
        val nextPeriod = deriveNextPeriod(latestDraw.period)
        val nextExcludedNums = overridePendingNumbers ?: predict6ExcludedNumbers(
            draws = sortedDraws,
            maxNumberPool = maxPool
        ).predictedExcludedNumbers

        records.add(
            BufferedPredictionRecord(
                period = nextPeriod,
                predictedExcludedNumbers = nextExcludedNums,
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

    fun deriveNextPeriod(currentPeriod: String): String {
        return try {
            val num = currentPeriod.toLong()
            (num + 1).toString()
        } catch (e: Exception) {
            "2026${(System.currentTimeMillis() % 1000)}"
        }
    }

}
