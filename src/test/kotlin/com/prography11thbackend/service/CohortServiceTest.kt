package com.prography11thbackend.service

import com.prography11thbackend.common.BusinessException
import com.prography11thbackend.common.ErrorCode
import com.prography11thbackend.entity.Cohort
import com.prography11thbackend.entity.Part
import com.prography11thbackend.entity.Team
import com.prography11thbackend.repository.CohortRepository
import com.prography11thbackend.repository.PartRepository
import com.prography11thbackend.repository.TeamRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.util.*

@ExtendWith(MockitoExtension::class)
class CohortServiceTest {

    @Mock lateinit var cohortRepository: CohortRepository
    @Mock lateinit var partRepository: PartRepository
    @Mock lateinit var teamRepository: TeamRepository

    private fun service() = CohortService(cohortRepository, partRepository, teamRepository)

    private val cohort10 = Cohort(id = 1, generation = 10, name = "10기")
    private val cohort11 = Cohort(id = 2, generation = 11, name = "11기")

    @Test
    fun `getCohorts returns all cohorts`() {
        whenever(cohortRepository.findAll()).thenReturn(listOf(cohort10, cohort11))

        val result = service().getCohorts()
        assertEquals(2, result.size)
        assertEquals(10, result[0].generation)
        assertEquals(11, result[1].generation)
    }

    @Test
    fun `getCohorts returns empty list when no cohorts`() {
        whenever(cohortRepository.findAll()).thenReturn(emptyList())

        val result = service().getCohorts()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getCohortDetail returns cohort with parts and teams`() {
        val parts = listOf(
            Part(id = 1, name = "SERVER", cohort = cohort11),
            Part(id = 2, name = "WEB", cohort = cohort11)
        )
        val teams = listOf(
            Team(id = 1, name = "Team A", cohort = cohort11)
        )
        whenever(cohortRepository.findById(2L)).thenReturn(Optional.of(cohort11))
        whenever(partRepository.findByCohortId(2L)).thenReturn(parts)
        whenever(teamRepository.findByCohortId(2L)).thenReturn(teams)

        val result = service().getCohortDetail(2L)
        assertEquals(11, result.generation)
        assertEquals(2, result.parts.size)
        assertEquals(1, result.teams.size)
        assertEquals("SERVER", result.parts[0].name)
        assertEquals("Team A", result.teams[0].name)
    }

    @Test
    fun `getCohortDetail throws when cohort not found`() {
        whenever(cohortRepository.findById(99L)).thenReturn(Optional.empty())

        val ex = assertThrows(BusinessException::class.java) {
            service().getCohortDetail(99L)
        }
        assertEquals(ErrorCode.COHORT_NOT_FOUND, ex.errorCode)
    }
}
