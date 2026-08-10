package com.example

import com.example.data.remote.MacauDataFetcher
import com.example.engine.AnalyzerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzerEngineTest {

    @Test
    fun `test circular target position calculation`() {
        // P = 1 => 1, 2, 6
        val pos1 = AnalyzerEngine.computeCircularTargetPositions(1, 6)
        assertEquals(listOf(1, 2, 6), pos1)

        // P = 6 => 5, 6, 1
        val pos6 = AnalyzerEngine.computeCircularTargetPositions(6, 6)
        assertEquals(listOf(5, 6, 1), pos6)

        // P = 3 => 2, 3, 4
        val pos3 = AnalyzerEngine.computeCircularTargetPositions(3, 6)
        assertEquals(listOf(2, 3, 4), pos3)
    }

    @Test
    fun `test prediction generates exactly 6 excluded numbers`() {
        val mockData = MacauDataFetcher.generateInitialHistoricalData(30)
        val result = AnalyzerEngine.predict6ExcludedNumbers(mockData, 49)

        assertNotNull(result)
        assertEquals(6, result.predictedExcludedNumbers.size)
        assertTrue(result.predictedExcludedNumbers.all { it in 1..49 })
    }

    @Test
    fun `test auto backtest execution`() {
        val mockData = MacauDataFetcher.generateInitialHistoricalData(50)
        val backtestRecords = AnalyzerEngine.runAutoBacktest(mockData, backtestCount = 20, maxPool = 49)

        assertTrue(backtestRecords.isNotEmpty())
        for (record in backtestRecords) {
            assertEquals(6, record.predictedExcludedNumbers.size)
        }
    }
}
