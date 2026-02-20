package com.prography11thbackend.dto.response

data class MemberAttendanceDetailResponse(
    val memberId: Long, val memberName: String,
    val generation: Int?, val partName: String?, val teamName: String?,
    val deposit: Int?, val excuseCount: Int?,
    val attendances: List<AttendanceResponse>
)
