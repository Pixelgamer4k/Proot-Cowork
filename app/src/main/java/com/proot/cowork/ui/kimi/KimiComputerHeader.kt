package com.proot.cowork.ui.kimi

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proot.cowork.R

@Composable
fun KimiComputerHeader(
    isActive: Boolean,
    toolCallCount: Int,
    maxToolCalls: Int,
    toolLimitReached: Boolean,
    modifier: Modifier = Modifier,
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    val progress = (toolCallCount.toFloat() / maxToolCalls.coerceAtLeast(1)).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(KimiTokens.ShapeCard)
            .background(KimiTokens.CardElevated)
            .border(1.dp, KimiTokens.Border, KimiTokens.ShapeCard)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(KimiTokens.ShapeCard)
                    .background(KimiTokens.ThinkBg)
                    .border(1.dp, KimiTokens.Border, KimiTokens.ShapeCard),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Computer, null, tint = KimiTokens.Accent, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Text(
                    text = stringResource(R.string.kimi_computer_title),
                    color = KimiTokens.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Text(
                    text = when {
                        toolLimitReached -> stringResource(R.string.tool_limit_reached, maxToolCalls)
                        isActive -> stringResource(R.string.kimi_computer_working)
                        else -> stringResource(R.string.kimi_computer_done)
                    },
                    color = KimiTokens.TextSecondary,
                    fontSize = 12.sp,
                )
            }
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(pulse)
                        .clip(CircleShape)
                        .background(KimiTokens.Success),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tool_calls_progress, toolCallCount, maxToolCalls),
                color = if (toolLimitReached) KimiTokens.Error else KimiTokens.TextMuted,
                fontSize = 11.sp,
            )
            Text(
                text = stringResource(R.string.kimi_task_progress, toolCallCount.coerceAtMost(maxToolCalls), maxToolCalls),
                color = KimiTokens.TextMuted,
                fontSize = 11.sp,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .clip(KimiTokens.ShapePill),
            color = if (toolLimitReached) KimiTokens.Error else KimiTokens.Accent,
            trackColor = KimiTokens.Border,
        )
    }
}
