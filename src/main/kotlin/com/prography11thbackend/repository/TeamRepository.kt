package com.prography11thbackend.repository

import com.prography11thbackend.entity.Team
import org.springframework.data.jpa.repository.JpaRepository

interface TeamRepository : JpaRepository<Team, Long> {
    fun findByCohortId(cohortId: Long): List<Team>
}
