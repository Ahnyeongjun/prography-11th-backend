package com.prography11thbackend.dto.response

import com.prography11thbackend.entity.SessionStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class AdminSessionResponse(
    val id: Long, val cohortId: Long, val title: String,
    val date: LocalDate, val time: LocalTime,
    val location: String, val status: SessionStatus,
    val attendanceSummary: AttendanceSummaryDto,
    val qrActive: Boolean,
    val createdAt: Instant, val updatedAt: Instant
)
