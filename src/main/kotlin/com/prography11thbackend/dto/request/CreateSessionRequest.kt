package com.prography11thbackend.dto.request

import java.time.LocalDate
import java.time.LocalTime

data class CreateSessionRequest(
    val title: String,
    val date: LocalDate,
    val time: LocalTime,
    val location: String
)
