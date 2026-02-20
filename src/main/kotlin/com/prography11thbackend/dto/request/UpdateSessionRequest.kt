package com.prography11thbackend.dto.request

import com.prography11thbackend.entity.SessionStatus
import java.time.LocalDate
import java.time.LocalTime

data class UpdateSessionRequest(
    val title: String? = null,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val location: String? = null,
    val status: SessionStatus? = null
)
