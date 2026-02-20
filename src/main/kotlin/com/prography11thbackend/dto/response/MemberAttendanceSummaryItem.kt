package com.prography11thbackend.dto.response

data class MemberAttendanceSummaryItem(
    val memberId: Long, val memberName: String,
    val present: Int, val absent: Int, val late: Int, val excused: Int,
    val totalPenalty: Int, val deposit: Int
)
