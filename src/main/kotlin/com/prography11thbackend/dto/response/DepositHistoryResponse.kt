package com.prography11thbackend.dto.response

import com.prography11thbackend.entity.DepositType
import java.time.Instant

data class DepositHistoryResponse(
    val id: Long, val cohortMemberId: Long,
    val type: DepositType,
    val amount: Int, val balanceAfter: Int,
    val attendanceId: Long?,
    val description: String?,
    val createdAt: Instant
)
