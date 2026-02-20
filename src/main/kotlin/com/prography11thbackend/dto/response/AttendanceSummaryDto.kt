package com.prography11thbackend.dto.response

data class AttendanceSummaryDto(
    val present: Int, val absent: Int, val late: Int,
    val excused: Int, val total: Int
)
