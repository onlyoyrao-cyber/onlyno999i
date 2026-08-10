package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class BallType {
    NORMAL,
    TARGET_X,      // Purple Accent
    SANDWICH_Y,    // Dark Purple Secondary
    EXCLUDED_KILL, // Rose Red Kill Number
    PATH_SAFE,     // Emerald Green Safe
    HIGHLIGHT_P    // Deep Violet Highlight
}

@Composable
fun DrawBall(
    number: Int,
    modifier: Modifier = Modifier,
    type: BallType = BallType.NORMAL,
    size: Dp = 40.dp,
    label: String? = null
) {
    val (background, textColor, borderColor) = when (type) {
        BallType.NORMAL -> Triple(
            Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF4EFF4))),
            Color(0xFF1D1B20),
            Color(0xFFCAC4D0)
        )
        BallType.TARGET_X -> Triple(
            Brush.verticalGradient(listOf(Color(0xFF6750A4), Color(0xFF4F378B))),
            Color.White,
            Color(0xFF21005D)
        )
        BallType.SANDWICH_Y -> Triple(
            Brush.verticalGradient(listOf(Color(0xFFE8DEF8), Color(0xFFD0BCFF))),
            Color(0xFF21005D),
            Color(0xFF6750A4)
        )
        BallType.EXCLUDED_KILL -> Triple(
            Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFF0F0))),
            Color(0xFFB3261E),
            Color(0xFFF2B8B5)
        )
        BallType.PATH_SAFE -> Triple(
            Brush.verticalGradient(listOf(Color(0xFFDCFCE7), Color(0xFF86EFAC))),
            Color(0xFF166534),
            Color(0xFF22C55E)
        )
        BallType.HIGHLIGHT_P -> Triple(
            Brush.verticalGradient(listOf(Color(0xFF21005D), Color(0xFF140038))),
            Color.White,
            Color(0xFF6750A4)
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(background)
                .border(2.dp, borderColor, CircleShape)
        ) {
            Text(
                text = String.format("%02d", number),
                color = textColor,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (!label.isNullOrEmpty()) {
            Text(
                text = label,
                color = Color(0xFF49454F),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

