package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BacktestRecord
import com.example.data.model.BufferSummary
import com.example.data.model.BufferedPredictionRecord
import com.example.ui.components.BacktestItemCard
import com.example.ui.components.BacktestSummaryGauge
import com.example.ui.components.BufferedPredictionsCard
import com.example.ui.viewmodel.BacktestSummary

enum class BacktestFilter {
    ALL,
    PERFECT,
    PARTIAL
}

@Composable
fun BacktestScreen(
    summary: BacktestSummary,
    records: List<BacktestRecord>,
    bufferSummary: BufferSummary,
    bufferedPredictions: List<BufferedPredictionRecord>,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(BacktestFilter.ALL) }

    val filteredRecords = when (selectedFilter) {
        BacktestFilter.ALL -> records
        BacktestFilter.PERFECT -> records.filter { it.isPerfectSuccess }
        BacktestFilter.PARTIAL -> records.filter { !it.isPerfectSuccess }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Top Gauge Metric
        item {
            BacktestSummaryGauge(summary = summary)
        }

        // 10-Period Prediction Buffer & Live Hit Verification Card
        item {
            BufferedPredictionsCard(
                summary = bufferSummary,
                bufferList = bufferedPredictions
            )
        }

        // Filter Chips Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    text = "全部回测 (${records.size})",
                    isSelected = selectedFilter == BacktestFilter.ALL,
                    onClick = { selectedFilter = BacktestFilter.ALL }
                )
                FilterChip(
                    text = "100%完全排除 (${records.count { it.isPerfectSuccess }})",
                    isSelected = selectedFilter == BacktestFilter.PERFECT,
                    onClick = { selectedFilter = BacktestFilter.PERFECT }
                )
                FilterChip(
                    text = "含命中 (${records.count { !it.isPerfectSuccess }})",
                    isSelected = selectedFilter == BacktestFilter.PARTIAL,
                    onClick = { selectedFilter = BacktestFilter.PARTIAL }
                )
            }
        }

        // Records List
        items(filteredRecords) { record ->
            BacktestItemCard(record = record)
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(
                if (isSelected) Color(0xFF6750A4) else Color(0xFFE8DEF8),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color(0xFF49454F),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

