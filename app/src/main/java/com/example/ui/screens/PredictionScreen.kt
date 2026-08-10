package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BufferSummary
import com.example.data.model.BufferedPredictionRecord
import com.example.data.model.DrawRecord
import com.example.data.model.PredictionResult
import com.example.ui.components.BallType
import com.example.ui.components.BufferedPredictionsCard
import com.example.ui.components.DrawBall
import com.example.ui.components.PredictionCard

@Composable
fun PredictionScreen(
    predictionResult: PredictionResult?,
    bufferSummary: BufferSummary,
    bufferedPredictions: List<BufferedPredictionRecord>,
    recentDraws: List<DrawRecord>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Hero Update Banner (High Density Theme)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFEADDFF),
                        RoundedCornerShape(20.dp)
                    )
                    .border(1.dp, Color(0xFFD0BCFF), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoMode,
                                contentDescription = "Auto Sync",
                                tint = Color(0xFF21005D),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "每日 21:35 自动同步更新数据",
                                color = Color(0xFF21005D),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "数据源: https://macaujc.ddcdn.cloudns.org/",
                            color = Color(0xFF49454F),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier
                            .background(Color(0xFFE8DEF8), RoundedCornerShape(12.dp))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Data",
                            tint = if (isRefreshing) Color(0xFF79747E) else Color(0xFF6750A4),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Prediction Card Section (6 Excluded Numbers)
        item {
            if (predictionResult != null) {
                PredictionCard(result = predictionResult)
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(20.dp))
                ) {
                    Text(
                        text = "正在计算下期杀号矩阵...",
                        color = Color(0xFF49454F),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // 10-Period Forecast Buffer & Live Hit Verification Card
        item {
            BufferedPredictionsCard(
                summary = bufferSummary,
                bufferList = bufferedPredictions
            )
        }

        // Recent Draws Title
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFFE8DEF8), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "最新开奖记录 (最近历史)",
                    color = Color(0xFF1D1B20),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Recent Draws List Items
        items(recentDraws.take(6)) { draw ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "第 ${draw.period} 期",
                            color = Color(0xFF1D1B20),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = draw.dateStr,
                            color = Color(0xFF79747E),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        draw.numbers.forEachIndexed { index, num ->
                            DrawBall(
                                number = num,
                                type = BallType.NORMAL,
                                size = 32.dp,
                                label = "P${index + 1}"
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

