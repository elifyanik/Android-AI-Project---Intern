package com.example.demo1.data

data class ChatSessionPreview(
    val id: Long,
    val title: String,
    val updatedAt: Long,
    val messageCount: Int,
    val isPinned: Boolean
)
