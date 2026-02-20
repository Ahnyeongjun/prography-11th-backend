package com.prography11thbackend.dto.request

data class UpdateMemberRequest(
    val name: String? = null,
    val phone: String? = null,
    val cohortId: Long? = null,
    val partId: Long? = null,
    val teamId: Long? = null
)
