package com.prography11thbackend.dto.request

import com.prography11thbackend.entity.AttendanceStatus

data class CreateAttendanceRequest(
    val sessionId: Long,
    val memberId: Long,
    val status: AttendanceStatus,
    val lateMinutes: Int? = null,
    val reason: String? = null
)
