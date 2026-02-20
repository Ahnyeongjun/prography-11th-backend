package com.prography11thbackend.repository

import com.prography11thbackend.entity.Cohort
import org.springframework.data.jpa.repository.JpaRepository

interface CohortRepository : JpaRepository<Cohort, Long> {
    fun findByGeneration(generation: Int): Cohort?
}
