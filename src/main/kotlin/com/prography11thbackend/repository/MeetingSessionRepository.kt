package com.prography11thbackend.repository

import com.prography11thbackend.entity.MeetingSession
import com.prography11thbackend.entity.SessionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface MeetingSessionRepository : JpaRepository<MeetingSession, Long> {
    fun findByCohortGenerationAndStatusNot(generation: Int, status: SessionStatus): List<MeetingSession>
    fun findByCohortGeneration(generation: Int): List<MeetingSession>

    @Query("""
        SELECT s FROM MeetingSession s
        JOIN FETCH s.cohort
        WHERE s.cohort.generation = :generation
        AND (:dateFrom IS NULL OR s.date >= :dateFrom)
        AND (:dateTo IS NULL OR s.date <= :dateTo)
        AND (:status IS NULL OR s.status = :status)
    """)
    fun findByGenerationWithFilters(
        @Param("generation") generation: Int,
        @Param("dateFrom") dateFrom: LocalDate?,
        @Param("dateTo") dateTo: LocalDate?,
        @Param("status") status: SessionStatus?
    ): List<MeetingSession>
}
