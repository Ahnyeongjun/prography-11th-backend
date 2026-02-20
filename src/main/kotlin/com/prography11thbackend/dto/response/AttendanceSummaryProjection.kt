package com.prography11thbackend.dto.response

interface AttendanceSummaryProjection {
    fun getPresent(): Long
    fun getAbsent(): Long
    fun getLate(): Long
    fun getExcused(): Long
    fun getTotal(): Long
}
