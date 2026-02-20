package com.prography11thbackend.dto.request

data class CreateMemberRequest(
    val loginId: String,
    val password: String,
    val name: String,
    val phone: String,
    val cohortId: Long,
    val partId: Long? = null,
    val teamId: Long? = null
)
