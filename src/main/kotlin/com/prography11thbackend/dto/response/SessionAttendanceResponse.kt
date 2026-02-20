package com.prography11thbackend.dto.response

data class SessionAttendanceResponse(
    val sessionId: Long, val sessionTitle: String,
    val attendances: List<AttendanceResponse>
)
