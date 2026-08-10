package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.data.model.BufferStatus
import com.example.data.model.BufferSummary
import com.example.data.model.BufferedPredictionRecord

@Composable
fun BufferedPredictionsCard(
    summary: BufferSummary,
    bufferList: List<BufferedPredictionRecord>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFE8DEF8), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Buffer Tracker",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "10期推测号码缓冲池与实时对碰",
                            color = Color(0xFF1D1B20),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "每期开奖自动对碰分析：到底命中了没有？",
                            color = Color(0xFF49454F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint = Color(0xFF6750A4)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Stats Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF7FF), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE8DEF8), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BufferMetric(
                    label = "缓冲容量",
                    value = "${summary.totalBuffered} 期"
                )
                BufferMetric(
                    label = "已开奖对碰",
                    value = "${summary.evaluatedCount} 期"
                )
                BufferMetric(
                    label = "0命中(完全避开)",
                    value = "${summary.successCount} 期"
                )
                BufferMetric(
                    label = "对碰胜率",
                    value = String.format("%.1f%%", summary.successRatePercentage),
                    valueColor = Color(0xFF6750A4)
                )
            }

            // Expanded List
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    bufferList.forEach { record ->
                        BufferedItemRow(record = record)
                    }
                }
            }
        }
    }
}

@Composable
private fun BufferMetric(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF1D1B20)
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color(0xFF79747E),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun BufferedItemRow(
    record: BufferedPredictionRecord
) {
    val (statusBg, statusBorder, statusText, statusIcon) = when (record.status) {
        BufferStatus.PENDING -> Quadruple(
            Color(0xFFEADDFF),
            Color(0xFFD0BCFF),
            Color(0xFF21005D),
            Icons.Default.HourglassTop
        )
        BufferStatus.HIT_SUCCESS -> Quadruple(
            Color(0xFFDCFCE7),
            Color(0xFF86EFAC),
            Color(0xFF166534),
            Icons.Default.CheckCircle
        )
        BufferStatus.HIT_WARNING -> Quadruple(
            Color(0xFFFFF0F0),
            Color(0xFFF2B8B5),
            Color(0xFFB3261E),
            Icons.Default.Warning
        )
    }

    val statusTitle = when (record.status) {
        BufferStatus.PENDING -> "⏳ 缓冲中 · 待开奖对碰"
        BufferStatus.HIT_SUCCESS -> "✅ 0命中 (100%成功排除)"
        BufferStatus.HIT_WARNING -> "⚠️ 命中 ${record.hitCount} 个杀号"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Period + Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "第 ${record.period} 期",
                        color = Color(0xFF1D1B20),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (record.isPending) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF6750A4), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "下期缓冲号",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .background(statusBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = "Status",
                            tint = statusText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusTitle,
                            color = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Display Predictions vs Actual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Column 1: Predicted Excluded 6 Numbers
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "缓冲推测6杀号:",
                        color = Color(0xFF49454F),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        record.predictedExcludedNumbers.forEach { num ->
                            val isHit = record.hitExcludedNumbers.contains(num)
                            DrawBall(
                                number = num,
                                type = if (isHit) BallType.EXCLUDED_KILL else BallType.NORMAL,
                                size = 26.dp
                            )
                        }
                    }
                }

                // Column 2: Actual Numbers (or Pending)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "实际开奖号码:",
                        color = Color(0xFF49454F),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (record.actualNumbers != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            record.actualNumbers.forEach { num ->
                                val isPredictedHit = record.hitExcludedNumbers.contains(num)
                                DrawBall(
                                    number = num,
                                    type = if (isPredictedHit) BallType.EXCLUDED_KILL else BallType.SANDWICH_Y,
                                    size = 26.dp
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .height(26.dp)
                                .background(Color(0xFFE8DEF8), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⏳ 等待下期开奖结果...",
                                color = Color(0xFF21005D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Evaluation Note
            Spacer(modifier = Modifier.height(8.dp))
            val summaryText = when {
                record.isPending -> "📌 到底命中了没有？本期推测6号已进入缓冲锁定区，开奖后将自动计算。"
                record.hitCount == 0 -> "🎯 对碰结果：6个推测号全部成功避开开奖号码，100%排除成功！"
                else -> "⚠️ 对碰结果：其中 ${record.hitExcludedNumbers.joinToString(", ")} 号出现在开奖结果中，命中杀号偏离 ${record.hitCount} 个。"
            }

            Text(
                text = summaryText,
                color = if (record.hitCount == 0 && !record.isPending) Color(0xFF166534) else Color(0xFF49454F),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
