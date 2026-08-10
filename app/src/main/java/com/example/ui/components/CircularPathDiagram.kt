package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TriggerInfo
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularPathDiagram(
    triggerInfo: TriggerInfo,
    modifier: Modifier = Modifier
) {
    val totalPositions = 6
    val baseP = triggerInfo.basePositionP.coerceIn(1, totalPositions)
    val targetPositions = triggerInfo.targetPositions1Based

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "边缘环形路径推演 (Circular Algorithm)",
                color = Color(0xFF1D1B20),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "当基准位 P = 第 $baseP 名时，验证环形邻位范围: [ ${targetPositions.joinToString("名, ")}名 ]",
                color = Color(0xFF49454F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Circular Visual Canvas
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    val radius = size.width / 2.3f
                    val center = Offset(size.width / 2, size.height / 2)

                    // Draw connecting circular ring line
                    drawCircle(
                        color = Color(0xFFCAC4D0),
                        radius = radius,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )

                    // Draw active target path arc highlights
                    for (i in 1..totalPositions) {
                        val angle = Math.toRadians((i - 1) * (360.0 / totalPositions) - 90.0)
                        val x = center.x + radius * cos(angle).toFloat()
                        val y = center.y + radius * sin(angle).toFloat()

                        val isBaseP = i == baseP
                        val isTarget = targetPositions.contains(i)

                        if (isTarget) {
                            drawCircle(
                                color = if (isBaseP) Color(0xFF21005D) else Color(0xFF6750A4),
                                radius = 18.dp.toPx(),
                                center = Offset(x, y)
                            )
                        } else {
                            drawCircle(
                                color = Color(0xFFF4EFF4),
                                radius = 14.dp.toPx(),
                                center = Offset(x, y)
                            )
                            drawCircle(
                                color = Color(0xFFCAC4D0),
                                radius = 14.dp.toPx(),
                                center = Offset(x, y),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                }

                // Overlay Text Badges for positions 1..6
                for (i in 1..totalPositions) {
                    val angleDeg = (i - 1) * (360.0 / totalPositions) - 90.0
                    val angleRad = Math.toRadians(angleDeg)
                    val radiusDp = 78.dp.value
                    val xOffset = (radiusDp * cos(angleRad)).dp
                    val yOffset = (radiusDp * sin(angleRad)).dp

                    val isBaseP = i == baseP
                    val isTarget = targetPositions.contains(i)

                    val numInN = triggerInfo.circularPathNumbersInN.getOrNull(targetPositions.indexOf(i))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(start = if (xOffset.value > 0) xOffset else 0.dp,
                                     top = if (yOffset.value > 0) yOffset else 0.dp)
                    ) {
                        Text(
                            text = if (numInN != null && isTarget) "第${i}位\n($numInN)" else "第${i}位",
                            color = when {
                                isTarget -> Color.White
                                else -> Color(0xFF49454F)
                            },
                            fontSize = 10.sp,
                            fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF21005D), RoundedCornerShape(2.dp))
                )
                Text(
                    text = " 正位 P (第${baseP}名)",
                    color = Color(0xFF1D1B20),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF6750A4), RoundedCornerShape(2.dp))
                )
                Text(
                    text = " 环形邻位 (P-1, P+1)",
                    color = Color(0xFF1D1B20),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

