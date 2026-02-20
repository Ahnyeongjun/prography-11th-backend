package com.prography11thbackend.dto.response

import com.prography11thbackend.entity.MemberRole
import com.prography11thbackend.entity.MemberStatus
import java.time.Instant

data class MemberResponse(
    val id: Long, val loginId: String, val name: String, val phone: String,
    val status: MemberStatus, val role: MemberRole,
    val createdAt: Instant, val updatedAt: Instant
)
