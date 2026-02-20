package com.prography11thbackend.repository

import com.prography11thbackend.entity.Part
import org.springframework.data.jpa.repository.JpaRepository

interface PartRepository : JpaRepository<Part, Long> {
    fun findByCohortId(cohortId: Long): List<Part>
}
