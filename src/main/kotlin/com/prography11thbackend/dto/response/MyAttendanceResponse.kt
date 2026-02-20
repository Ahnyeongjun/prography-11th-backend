package com.prography11thbackend.dto.response

import com.prography11thbackend.entity.AttendanceStatus
import java.time.Instant

data class MyAttendanceResponse(
    val id: Long, val sessionId: Long, val sessionTitle: String,
    val status: AttendanceStatus,
    val lateMinutes: Int?, val penaltyAmount: Int,
    val reason: String?, val checkedInAt: Instant?,
    val createdAt: Instant
)
