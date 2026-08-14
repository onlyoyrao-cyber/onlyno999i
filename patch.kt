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
                val minDistanceToPath = triggerInfo.circularPathNumbersInN.minOfOrNull { Math.abs(it - num) } ?: 10
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
                coldHotSummary = "由于 AI 对撞引擎未能找到完美踩空的号码组合（最低碰撞数 ${minHits}），已自动降级为【传统公式推演引擎】选号。传统引擎依据冷热遗漏偏离度锁定了这 6 个杀号: ${traditionalExcluded6.joinToString("、")}",
                algorithmNotes = listOf(
                    "降级原因: Monte Carlo 对撞 ${iterations} 次均发生历史碰撞 (最低 ${minHits})，说明近期开出号码极其分散，无法暴力踩空。",
                    "触发机制: 目标号 X=${triggerInfo.targetNumberX}, 夹心号 Y=${triggerInfo.sandwichNumberY}",
                    "基准位锁定: P=第${triggerInfo.basePositionP}名, 环形目标位置=${triggerInfo.targetPositions1Based.joinToString(",")}",
                    "推演引擎: 传统固定公式 (降级备用)",
                    "最终锁定杀号: ${traditionalExcluded6.joinToString(", ")}"
                )
            )
        }
