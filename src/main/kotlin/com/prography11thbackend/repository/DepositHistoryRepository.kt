package com.prography11thbackend.repository

import com.prography11thbackend.entity.DepositHistory
import org.springframework.data.jpa.repository.JpaRepository

interface DepositHistoryRepository : JpaRepository<DepositHistory, Long> {
    fun findByCohortMemberIdOrderByCreatedAtAsc(cohortMemberId: Long): List<DepositHistory>
}
