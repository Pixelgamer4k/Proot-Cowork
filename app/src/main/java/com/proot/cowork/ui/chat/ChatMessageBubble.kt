package com.proot.cowork.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.proot.cowork.R
import com.proot.cowork.domain.agent.AgentMessage
import com.proot.cowork.domain.agent.MessageRole
import com.proot.cowork.ui.design.CoworkTokens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
    message: AgentMessage,
    onCopy: () -> Unit,
    onEdit: ((String) -> Unit)?,
    onRegenerate: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (message.role == MessageRole.ASSISTANT && message.content.isBlank()) return

    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var editOpen by remember { mutableStateOf(false) }
    var editText by remember(message.content) { mutableStateOf(message.content) }

    val isUser = message.role == MessageRole.USER
    val canEdit = isUser && onEdit != null
    val canRegenerate = onRegenerate != null &&
        (message.role == MessageRole.USER || message.role == MessageRole.ASSISTANT)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = { menuOpen = true },
                    ),
                shape = RoundedCornerShape(18.dp, 18.dp, if (isUser) 18.dp else 6.dp, if (isUser) 6.dp else 18.dp),
                color = when (message.role) {
                    MessageRole.USER -> CoworkTokens.Mint.copy(alpha = 0.16f)
                    MessageRole.SYSTEM -> CoworkTokens.SurfaceElevated
                    else -> CoworkTokens.Surface
                },
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(14.dp, 10.dp),
                    color = CoworkTokens.TextPrimary,
                )
            }

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_action_copy)) },
                    onClick = {
                        menuOpen = false
                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clip.setPrimaryClip(ClipData.newPlainText("message", message.content))
                        onCopy()
                    },
                )
                if (canEdit) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_action_edit)) },
                        onClick = {
                            menuOpen = false
                            editText = message.content
                            editOpen = true
                        },
                    )
                }
                if (canRegenerate) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_action_regenerate)) },
                        onClick = {
                            menuOpen = false
                            onRegenerate?.invoke()
                        },
                    )
                }
            }
        }
    }

    if (editOpen && canEdit) {
        AlertDialog(
            onDismissRequest = { editOpen = false },
            title = { Text(stringResource(R.string.chat_edit_title)) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 6,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        editOpen = false
                        onEdit?.invoke(editText.trim())
                    },
                ) { Text(stringResource(R.string.chat_edit_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
