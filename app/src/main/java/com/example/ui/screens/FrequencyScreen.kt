package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FrequencyStat

@Composable
fun FrequencyScreen(
    frequencyStats: Map<Int, FrequencyStat>,
    maxPool: Int,
    modifier: Modifier = Modifier
) {
    val statsList = (1..maxPool).mapNotNull { frequencyStats[it] }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header Legend
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "全量冷热与遗漏统计 (1 ~ $maxPool)",
                    color = Color(0xFF1D1B20),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendTag(color = Color(0xFFB3261E), label = "热号 (30期≥5次)")
                    LegendTag(color = Color(0xFF6750A4), label = "冷态/深遗漏 (遗漏≥12期)")
                    LegendTag(color = Color(0xFF79747E), label = "温号")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grid of Numbers
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(statsList) { stat ->
                val (bgColor, borderColor, textColor) = when {
                    stat.isHot -> Triple(Color(0xFFF2B8B5), Color(0xFFB3261E), Color(0xFF601410))
                    stat.isCold -> Triple(Color(0xFFEADDFF), Color(0xFF6750A4), Color(0xFF21005D))
                    else -> Triple(Color.White, Color(0xFFCAC4D0), Color(0xFF1D1B20))
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(bgColor, RoundedCornerShape(12.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%02d", stat.number),
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "遗漏:${stat.currentOmission}期",
                            color = if (stat.isHot || stat.isCold) textColor.copy(alpha = 0.8f) else Color(0xFF79747E),
                            fontSize = 9.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = "频:${stat.frequency30}次",
                            color = if (stat.isHot || stat.isCold) textColor.copy(alpha = 0.8f) else Color(0xFF49454F),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendTag(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = " $label",
            color = Color(0xFF49454F),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

