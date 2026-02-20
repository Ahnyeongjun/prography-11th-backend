package com.prography11thbackend.dto.response

import com.prography11thbackend.entity.AttendanceStatus
import java.time.Instant

data class AttendanceResponse(
    val id: Long, val sessionId: Long, val memberId: Long,
    val status: AttendanceStatus,
    val lateMinutes: Int?, val penaltyAmount: Int,
    val reason: String?, val checkedInAt: Instant?,
    val createdAt: Instant, val updatedAt: Instant
)
