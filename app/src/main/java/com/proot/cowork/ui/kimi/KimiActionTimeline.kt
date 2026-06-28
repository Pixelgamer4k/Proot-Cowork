package com.proot.cowork.ui.kimi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proot.cowork.R
import com.proot.cowork.domain.agent.AgentMessage
import com.proot.cowork.domain.agent.ToolCallStatus

@Composable
fun KimiThinkRow(
    lines: List<String>,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preview = lines.lastOrNull().orEmpty()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(KimiTokens.ShapeCard)
            .background(KimiTokens.ThinkBg)
            .border(1.dp, KimiTokens.Border, KimiTokens.ShapeCard)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.kimi_think_label),
            color = KimiTokens.TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(52.dp),
        )
        Text(
            text = if (expanded) lines.joinToString("\n") else preview,
            color = KimiTokens.TextSecondary,
            fontSize = 13.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = KimiTokens.TextMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun KimiActionTimeline(
    tools: List<AgentMessage>,
    modifier: Modifier = Modifier,
) {
    if (tools.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(180)),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        tools.forEachIndexed { index, tool ->
            KimiTimelineRow(
                tool = tool,
                isLast = index == tools.lastIndex,
            )
        }
    }
}

@Composable
private fun KimiTimelineRow(
    tool: AgentMessage,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(tool.id) { mutableStateOf(false) }
    val status = tool.toolStatus ?: ToolCallStatus.COMPLETED
    val dotColor = when (status) {
        ToolCallStatus.RUNNING -> KimiTokens.Accent
        ToolCallStatus.FAILED -> KimiTokens.Error
        ToolCallStatus.COMPLETED -> KimiTokens.Success
        ToolCallStatus.REQUESTED -> KimiTokens.TextMuted
    }
    val title = buildString {
        tool.agentName?.let { append("$it · ") }
        append(tool.toolName ?: "tool")
    }
    val detail = tool.content.trim()

    Row(modifier = modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor),
                contentAlignment = Alignment.Center,
            ) {
                if (status == ToolCallStatus.RUNNING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(8.dp),
                        strokeWidth = 1.dp,
                        color = KimiTokens.Bg,
                    )
                }
            }
            if (!isLast) {
                DottedTimelineLine(modifier = Modifier.height(36.dp).width(2.dp))
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 8.dp)
                .clip(KimiTokens.ShapeCard)
                .background(KimiTokens.Card)
                .border(1.dp, KimiTokens.Border, KimiTokens.ShapeCard)
                .clickable(enabled = detail.isNotBlank()) { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KimiStatusIcon(status)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    color = KimiTokens.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (detail.isNotBlank()) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = KimiTokens.TextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded && detail.isNotBlank(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Text(
                    text = detail.take(4000),
                    color = KimiTokens.TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun KimiStatusIcon(status: ToolCallStatus) {
    val (icon, tint) = when (status) {
        ToolCallStatus.RUNNING -> Icons.Default.PlayArrow to KimiTokens.Accent
        ToolCallStatus.COMPLETED -> Icons.Default.Check to KimiTokens.Success
        ToolCallStatus.FAILED -> Icons.Default.Close to KimiTokens.Error
        ToolCallStatus.REQUESTED -> Icons.Default.PlayArrow to KimiTokens.TextMuted
    }
    Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
}

@Composable
private fun DottedTimelineLine(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawLine(
            color = KimiTokens.DotInactive,
            start = Offset(size.width / 2, 0f),
            end = Offset(size.width / 2, size.height),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
        )
    }
}
