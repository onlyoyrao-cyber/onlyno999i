package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawDao {
    @Query("SELECT * FROM draw_records ORDER BY period DESC")
    fun getAllDraws(): Flow<List<DrawEntity>>

    @Query("SELECT * FROM draw_records ORDER BY period DESC LIMIT :limit")
    suspend fun getRecentDraws(limit: Int): List<DrawEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraws(draws: List<DrawEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraw(draw: DrawEntity)

    @Query("DELETE FROM draw_records WHERE period = :period")
    suspend fun deleteDrawByPeriod(period: String)

    @Query("DELETE FROM draw_records")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM draw_records")
    suspend fun getCount(): Int
}
