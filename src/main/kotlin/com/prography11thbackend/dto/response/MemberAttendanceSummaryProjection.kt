package com.prography11thbackend.dto.response

interface MemberAttendanceSummaryProjection {
    fun getPresent(): Long
    fun getAbsent(): Long
    fun getLate(): Long
    fun getExcused(): Long
    fun getTotalPenalty(): Long
}
