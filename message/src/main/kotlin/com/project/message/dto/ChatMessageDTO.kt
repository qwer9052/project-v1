package com.project.message.dto

import java.time.Instant


data class ChatMessageDTO(
    val roomId: String,
    val senderId: String,
    val message: String,
    val timestamp: Instant = Instant.now()
)
