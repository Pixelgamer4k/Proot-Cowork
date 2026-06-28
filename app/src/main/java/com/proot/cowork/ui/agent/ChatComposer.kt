package com.proot.cowork.ui.agent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.proot.cowork.R
import com.proot.cowork.domain.agent.ExecutionMode
import com.proot.cowork.ui.kimi.KimiTokens
import com.proot.cowork.ui.theme.Motion

@Composable
fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isExecuting: Boolean,
    isApiConfigured: Boolean,
    awaitingApproval: Boolean = false,
    executionMode: ExecutionMode,
    onModeChange: (ExecutionMode) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    artifactFileNames: List<String> = emptyList(),
    onPickFile: () -> Unit,
    onAttachArtifact: (String) -> Unit,
    onAddContextBlock: () -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var attachMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused) { onFocusChange(isFocused) }

    val showSend = value.isNotBlank() && !isExecuting && !awaitingApproval
    val canSend = showSend && isApiConfigured
    val borderColor = if (isFocused) KimiTokens.Accent.copy(alpha = 0.45f) else KimiTokens.Border

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .heightIn(min = 108.dp)
            .clip(KimiTokens.ShapeComposer)
            .background(KimiTokens.Card)
            .border(1.dp, borderColor, KimiTokens.ShapeComposer)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = when {
                        awaitingApproval -> stringResource(R.string.swarm_awaiting_execute)
                        !isApiConfigured -> stringResource(R.string.agent_api_required)
                        isExecuting -> stringResource(R.string.kimi_composer_working)
                        else -> stringResource(R.string.kimi_composer_hint)
                    },
                    color = KimiTokens.TextMuted,
                )
            },
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = KimiTokens.Accent,
                focusedTextColor = KimiTokens.TextPrimary,
                unfocusedTextColor = KimiTokens.TextPrimary,
            ),
            interactionSource = interactionSource,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
            enabled = !isExecuting,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                IconButton(
                    onClick = { attachMenuOpen = true },
                    enabled = !isExecuting,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(KimiTokens.CardElevated),
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.composer_attach_menu), tint = KimiTokens.TextSecondary)
                }
                DropdownMenu(expanded = attachMenuOpen, onDismissRequest = { attachMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.composer_attach_file)) },
                        leadingIcon = { Icon(Icons.Default.AttachFile, null) },
                        onClick = { attachMenuOpen = false; onPickFile() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.composer_add_context)) },
                        leadingIcon = { Icon(Icons.Default.Notes, null) },
                        onClick = { attachMenuOpen = false; onAddContextBlock() },
                    )
                    if (artifactFileNames.isNotEmpty()) {
                        artifactFileNames.takeLast(8).forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name, maxLines = 1) },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, null) },
                                onClick = {
                                    attachMenuOpen = false
                                    onAttachArtifact(name)
                                },
                            )
                        }
                    }
                }
            }

            if (isExecuting) {
                Spacer(Modifier.size(8.dp))
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = KimiTokens.Accent.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedContent(
                targetState = isExecuting,
                transitionSpec = {
                    (fadeIn(Motion.tweenQuick) + scaleIn(Motion.springBouncy))
                        .togetherWith(fadeOut(Motion.tweenQuick) + scaleOut())
                },
                label = "composerAction",
            ) { sending ->
                if (sending) {
                    Surface(
                        onClick = onStop,
                        shape = KimiTokens.ShapePill,
                        color = KimiTokens.Error.copy(alpha = 0.15f),
                        modifier = Modifier.border(1.dp, KimiTokens.Error.copy(alpha = 0.35f), KimiTokens.ShapePill),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Stop, stringResource(R.string.stop_agent), tint = KimiTokens.Error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(R.string.stop_agent), color = KimiTokens.Error, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else if (showSend) {
                    Surface(
                        onClick = { if (canSend) onSend() },
                        shape = KimiTokens.ShapePill,
                        color = KimiTokens.Accent,
                        modifier = Modifier.alpha(if (canSend) 1f else 0.45f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = KimiTokens.Bg, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(R.string.send), color = KimiTokens.Bg, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Surface(
                        onClick = onSpeak,
                        shape = KimiTokens.ShapePill,
                        color = KimiTokens.CardElevated,
                        modifier = Modifier.border(1.dp, KimiTokens.Border, KimiTokens.ShapePill),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Mic, null, tint = KimiTokens.TextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(R.string.speak), color = KimiTokens.TextPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
