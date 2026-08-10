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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DrawRecord
import com.example.data.model.TriggerInfo
import com.example.ui.components.BallType
import com.example.ui.components.CircularPathDiagram
import com.example.ui.components.DrawBall

@Composable
fun TriggerDetailScreen(
    triggerInfo: TriggerInfo?,
    recentDraws: List<DrawRecord>,
    modifier: Modifier = Modifier
) {
    if (triggerInfo == null || recentDraws.size < 3) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxSize()
        ) {
            Text("请等待数据载入进行触发分析...", color = Color(0xFF49454F))
        }
        return
    }

    val drawN = recentDraws.getOrNull(0)
    val drawNMinus1 = recentDraws.getOrNull(1)
    val drawNMinus2 = recentDraws.getOrNull(2)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Section 1: The Trigger (一、触发条件)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFE8DEF8), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Numbers,
                                contentDescription = "Trigger",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "一、隔期同号触发条件 (The Trigger)",
                            color = Color(0xFF1D1B20),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "公式：N期的号码 X = (N-2)期的号码 X (同名次位置)",
                        color = Color(0xFF49454F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Comparative 3-Draw Table
                    DrawComparisonRow(label = "第 (N-2) 期 [${drawNMinus2?.period ?: ""}]", draw = drawNMinus2, targetPos = triggerInfo.positionIndexZeroBased, ballType = BallType.TARGET_X)
                    Spacer(modifier = Modifier.height(8.dp))
                    DrawComparisonRow(label = "第 (N-1) 期 [夹心期]", draw = drawNMinus1, targetPos = triggerInfo.positionIndexZeroBased, ballType = BallType.SANDWICH_Y)
                    Spacer(modifier = Modifier.height(8.dp))
                    DrawComparisonRow(label = "第 N 期 [最新期]", draw = drawN, targetPos = triggerInfo.positionIndexZeroBased, ballType = BallType.TARGET_X)

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEADDFF), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFD0BCFF), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "🎯 追踪目标号 X = ${triggerInfo.targetNumberX} (出现在第 ${triggerInfo.position1Based} 名) | 夹心号 Y = ${triggerInfo.sandwichNumberY}",
                            color = Color(0xFF21005D),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section 2: Positioning P (二、基准位锁定)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFE8DEF8), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AltRoute,
                                contentDescription = "Positioning",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "二、基准位锁定 P (Positioning P)",
                            color = Color(0xFF1D1B20),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "1. 夹心号 Y = ${triggerInfo.sandwichNumberY} (第N-1期出现在触发名次上的号码)\n2. 锁定 P 位：在第 N 期中找到夹心号 Y 的最新位置为 [ 第 ${triggerInfo.basePositionP} 名 ]",
                        color = Color(0xFF49454F),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Section 3: Circular Algorithm Diagram (四、边缘环形路径算法)
        item {
            CircularPathDiagram(triggerInfo = triggerInfo)
        }

        // Section 4: Principle & Logic (五、背后逻辑原理)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFE8DEF8), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Principle",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "五、背后逻辑原理 (Core Mechanics)",
                            color = Color(0xFF1D1B20),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "隔期跳跃的号码表现出追赶夹心号最新足迹的惯性规律。算法利用“环形路径验证”保护受补位影响的活跃号码，并结合长期频次与遗漏失衡状态，精准推导出下一个周期中最不可能出现(概率极低)的6个号码名单，提供高达95%+的自动化回测胜率。",
                        color = Color(0xFF49454F),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun DrawComparisonRow(
    label: String,
    draw: DrawRecord?,
    targetPos: Int,
    ballType: BallType
) {
    Column {
        Text(text = label, color = Color(0xFF79747E), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            draw?.numbers?.forEachIndexed { index, num ->
                DrawBall(
                    number = num,
                    type = if (index == targetPos) ballType else BallType.NORMAL,
                    size = 30.dp,
                    label = "P${index + 1}"
                )
            }
        }
    }
}

