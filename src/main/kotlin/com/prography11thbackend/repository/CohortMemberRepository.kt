package com.prography11thbackend.repository

import com.prography11thbackend.entity.CohortMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CohortMemberRepository : JpaRepository<CohortMember, Long> {
    fun findByMemberIdAndCohortId(memberId: Long, cohortId: Long): CohortMember?
    fun findByMemberId(memberId: Long): List<CohortMember>
    fun findByCohortId(cohortId: Long): List<CohortMember>
    fun findByMemberIdAndCohortGeneration(memberId: Long, generation: Int): CohortMember?

    @Query("""
        SELECT cm FROM CohortMember cm
        JOIN FETCH cm.cohort
        LEFT JOIN FETCH cm.part
        LEFT JOIN FETCH cm.team
        WHERE cm.member.id IN :memberIds
    """)
    fun findByMemberIdInWithDetails(@Param("memberIds") memberIds: List<Long>): List<CohortMember>
}
