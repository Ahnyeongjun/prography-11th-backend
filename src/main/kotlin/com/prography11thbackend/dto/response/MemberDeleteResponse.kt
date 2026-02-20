package com.prography11thbackend.dto.response

import com.prography11thbackend.entity.MemberStatus
import java.time.Instant

data class MemberDeleteResponse(
    val id: Long, val loginId: String, val name: String,
    val status: MemberStatus, val updatedAt: Instant
)
