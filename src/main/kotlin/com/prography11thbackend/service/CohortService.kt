package com.prography11thbackend.service

import com.prography11thbackend.common.BusinessException
import com.prography11thbackend.common.ErrorCode
import com.prography11thbackend.dto.response.*
import com.prography11thbackend.repository.CohortRepository
import com.prography11thbackend.repository.PartRepository
import com.prography11thbackend.repository.TeamRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CohortService(
    private val cohortRepository: CohortRepository,
    private val partRepository: PartRepository,
    private val teamRepository: TeamRepository
) {
    fun getCohorts(): List<CohortResponse> {
        return cohortRepository.findAll().map {
            CohortResponse(it.id, it.generation, it.name, it.createdAt)
        }
    }

    fun getCohortDetail(id: Long): CohortDetailResponse {
        val cohort = cohortRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.COHORT_NOT_FOUND) }

        val parts = partRepository.findByCohortId(cohort.id).map { PartResponse(it.id, it.name) }
        val teams = teamRepository.findByCohortId(cohort.id).map { TeamResponse(it.id, it.name) }

        return CohortDetailResponse(
            cohort.id, cohort.generation, cohort.name, parts, teams, cohort.createdAt
        )
    }
}
