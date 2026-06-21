package com.proot.cowork.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.proot.cowork.R
import com.proot.cowork.domain.agent.AgentMessage
import com.proot.cowork.domain.agent.ExecutionMode
import com.proot.cowork.domain.agent.MessageRole
import com.proot.cowork.domain.agent.SwarmTask

@Composable
fun AgentPanel(
    messages: List<AgentMessage>,
    swarmTasks: List<SwarmTask>,
    executionMode: ExecutionMode,
    inputText: String,
    isExecuting: Boolean,
    onModeChange: (ExecutionMode) -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenBrowser: () -> Unit,
    onOpenSkills: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxSize(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = executionMode == ExecutionMode.PLAN,
                    onClick = { onModeChange(ExecutionMode.PLAN) },
                    label = { Text(stringResource(R.string.plan_mode)) },
                )
                FilterChip(
                    selected = executionMode == ExecutionMode.DIRECT,
                    onClick = { onModeChange(ExecutionMode.DIRECT) },
                    label = { Text(stringResource(R.string.direct_mode)) },
                )
                FilterChip(
                    selected = executionMode == ExecutionMode.SCHEDULE,
                    onClick = { onModeChange(ExecutionMode.SCHEDULE) },
                    label = { Text(stringResource(R.string.schedule_mode)) },
                )
            }
            Row {
                IconButton(onClick = onOpenSkills) {
                    Icon(Icons.Default.Psychology, contentDescription = stringResource(R.string.skills))
                }
                IconButton(onClick = onOpenBrowser) {
                    Icon(Icons.Default.Folder, contentDescription = stringResource(R.string.browser))
                }
                IconButton(onClick = onOpenTerminal) {
                    Icon(Icons.Default.Terminal, contentDescription = stringResource(R.string.terminal))
                }
            }
        }

        if (swarmTasks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            SwarmTaskTree(tasks = swarmTasks)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (messages.isEmpty()) {
                item {
                    Text(
                        text = "Agent swarm ready. Configure API in Settings, then describe a task.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
            items(messages, key = { it.id }) { message ->
                MessageBubble(message)
            }
            if (isExecuting) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Agents working…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.agent_hint)) },
            trailingIcon = {
                IconButton(onClick = onSend, enabled = inputText.isNotBlank() && !isExecuting) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send))
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            maxLines = 4,
            shape = RoundedCornerShape(12.dp),
        )
    }
}

@Composable
private fun MessageBubble(message: AgentMessage) {
    val isUser = message.role == MessageRole.USER
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = when (message.role) {
        MessageRole.USER -> MaterialTheme.colorScheme.primaryContainer
        MessageRole.SYSTEM -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Text(
            text = message.content,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(bgColor, RoundedCornerShape(12.dp))
                .padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SwarmTaskTree(tasks: List<SwarmTask>, indent: Int = 0) {
    Column(modifier = Modifier.padding(start = (indent * 12).dp)) {
        tasks.forEach { task ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp),
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.height(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (task.children.isNotEmpty()) {
                SwarmTaskTree(tasks = task.children, indent = indent + 1)
            }
        }
    }
}
