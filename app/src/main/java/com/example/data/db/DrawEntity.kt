package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "draw_records")
data class DrawEntity(
    @PrimaryKey val period: String,
    val dateStr: String,
    val numbersCsv: String, // e.g. "05,12,23,31,38,45"
    val timestamp: Long = System.currentTimeMillis()
)
