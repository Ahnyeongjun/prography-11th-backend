package com.prography11thbackend.dto.response

import java.time.Instant

data class CohortResponse(
    val id: Long, val generation: Int, val name: String, val createdAt: Instant
)
