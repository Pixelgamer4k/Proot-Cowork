package com.proot.cowork.data.chat

import com.proot.cowork.domain.agent.ExecutionMode

data class ChatThreadMeta(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val executionMode: ExecutionMode = ExecutionMode.SWARM,
)
