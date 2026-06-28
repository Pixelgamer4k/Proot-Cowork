package com.proot.cowork.ui.kimi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proot.cowork.R
import com.proot.cowork.domain.agent.AgentMessage

@Composable
fun KimiUserCard(
    message: AgentMessage,
    onCopy: () -> Unit,
    onEdit: ((String) -> Unit)?,
    onRegenerate: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    KimiMessageCard(
        text = message.content,
        onCopy = onCopy,
        onEdit = onEdit,
        onRegenerate = onRegenerate,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KimiMessageCard(
    text: String,
    onCopy: () -> Unit,
    onEdit: ((String) -> Unit)?,
    onRegenerate: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var editOpen by remember { mutableStateOf(false) }
    var editText by remember(text) { mutableStateOf(text) }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(KimiTokens.ShapeCard)
                .background(KimiTokens.Card)
                .border(1.dp, KimiTokens.Border, KimiTokens.ShapeCard)
                .combinedClickable(onClick = {}, onLongClick = { menuOpen = true })
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = text,
                color = KimiTokens.TextPrimary,
                fontWeight = FontWeight.Normal,
                lineHeight = 22.sp,
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_action_copy)) },
                onClick = {
                    menuOpen = false
                    copyToClipboard(context, text)
                    onCopy()
                },
            )
            if (onEdit != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_action_edit)) },
                    onClick = { menuOpen = false; editOpen = true },
                )
            }
            if (onRegenerate != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_action_regenerate)) },
                    onClick = { menuOpen = false; onRegenerate() },
                )
            }
        }
    }

    if (editOpen && onEdit != null) {
        AlertDialog(
            onDismissRequest = { editOpen = false },
            title = { Text(stringResource(R.string.chat_edit_title)) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KimiTokens.Accent,
                        cursorColor = KimiTokens.Accent,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editOpen = false
                    onEdit(editText.trim())
                }) {
                    Text(stringResource(R.string.chat_edit_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KimiAssistantBlock(
    content: String,
    isStreaming: Boolean,
    onCopy: () -> Unit,
    onRegenerate: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (content.isBlank() && !isStreaming) return
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        KimiMarkdownText(
            text = content,
            showCursor = isStreaming && content.isBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { if (content.isNotBlank()) menuOpen = true },
                ),
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_action_copy)) },
                onClick = {
                    menuOpen = false
                    copyToClipboard(context, content)
                    onCopy()
                },
            )
            if (onRegenerate != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_action_regenerate)) },
                    onClick = { menuOpen = false; onRegenerate() },
                )
            }
        }
    }
}

@Composable
fun KimiSystemNotice(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(width = 3.dp, height = 14.dp)
                .clip(KimiTokens.ShapePill)
                .background(KimiTokens.TextMuted),
        )
        Text(
            text = text,
            color = KimiTokens.TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("message", text))
}
