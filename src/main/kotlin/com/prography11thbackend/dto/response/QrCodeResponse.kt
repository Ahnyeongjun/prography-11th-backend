package com.prography11thbackend.dto.response

import java.time.Instant

data class QrCodeResponse(
    val id: Long, val sessionId: Long, val hashValue: String,
    val createdAt: Instant, val expiresAt: Instant
)
