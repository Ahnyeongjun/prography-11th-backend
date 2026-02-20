package com.prography11thbackend.dto.response

import java.time.Instant

data class CohortDetailResponse(
    val id: Long, val generation: Int, val name: String,
    val parts: List<PartResponse>, val teams: List<TeamResponse>,
    val createdAt: Instant
)
