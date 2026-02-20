package com.prography11thbackend.dto.response

data class AttendanceSummaryResponse(
    val memberId: Long, val present: Int, val absent: Int,
    val late: Int, val excused: Int,
    val totalPenalty: Int, val deposit: Int?
)
