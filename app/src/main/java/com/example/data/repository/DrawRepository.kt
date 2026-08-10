package com.example.data.repository

import com.example.data.db.DrawDao
import com.example.data.db.DrawEntity
import com.example.data.model.DrawRecord
import com.example.data.remote.MacauDataFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DrawRepository(private val drawDao: DrawDao) {

    val allDraws: Flow<List<DrawRecord>> = drawDao.getAllDraws().map { entities ->
        entities.map { entityToRecord(it) }
    }

    suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        val count = drawDao.getCount()
        if (count == 0) {
            val initialData = MacauDataFetcher.generateInitialHistoricalData(100)
            val entities = initialData.map { recordToEntity(it) }
            drawDao.insertDraws(entities)
        }
    }

    suspend fun refreshFromRemote(url: String = "https://macaujc.ddcdn.cloudns.org/"): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val remoteDraws = MacauDataFetcher.fetchFromRemoteUrl(url)
            if (remoteDraws.isNotEmpty()) {
                val entities = remoteDraws.map { recordToEntity(it) }
                drawDao.insertDraws(entities)
                Result.success(remoteDraws.size)
            } else {
                Result.failure(Exception("未提取到新数据"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addDrawRecord(record: DrawRecord) = withContext(Dispatchers.IO) {
        drawDao.insertDraw(recordToEntity(record))
    }

    suspend fun deleteDraw(period: String) = withContext(Dispatchers.IO) {
        drawDao.deleteDrawByPeriod(period)
    }

    suspend fun resetToDefaultHistory() = withContext(Dispatchers.IO) {
        drawDao.clearAll()
        val defaultData = MacauDataFetcher.generateInitialHistoricalData(100)
        drawDao.insertDraws(defaultData.map { recordToEntity(it) })
    }

    private fun entityToRecord(entity: DrawEntity): DrawRecord {
        val nums = entity.numbersCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
        return DrawRecord(
            period = entity.period,
            dateStr = entity.dateStr,
            numbers = nums,
            timestamp = entity.timestamp
        )
    }

    private fun recordToEntity(record: DrawRecord): DrawEntity {
        val csv = record.numbers.joinToString(",") { String.format("%02d", it) }
        return DrawEntity(
            period = record.period,
            dateStr = record.dateStr,
            numbersCsv = csv,
            timestamp = record.timestamp
        )
    }
}
