package com.prography11thbackend.dto.response

interface CohortMemberAttendanceSummaryProjection {
    fun getMemberId(): Long
    fun getMemberName(): String
    fun getPresent(): Long
    fun getAbsent(): Long
    fun getLate(): Long
    fun getExcused(): Long
    fun getTotalPenalty(): Long
    fun getDeposit(): Int
}
