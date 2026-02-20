package com.prography11thbackend.dto.request

import com.prography11thbackend.entity.AttendanceStatus

data class UpdateAttendanceRequest(
    val status: AttendanceStatus,
    val lateMinutes: Int? = null,
    val reason: String? = null
)
