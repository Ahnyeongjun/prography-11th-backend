package com.prography11thbackend.service

import com.prography11thbackend.common.BusinessException
import com.prography11thbackend.common.ErrorCode
import com.prography11thbackend.dto.request.CreateMemberRequest
import com.prography11thbackend.dto.request.UpdateMemberRequest
import com.prography11thbackend.entity.*
import com.prography11thbackend.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.*

@ExtendWith(MockitoExtension::class)
class MemberServiceTest {

    @Mock lateinit var memberRepository: MemberRepository
    @Mock lateinit var cohortRepository: CohortRepository
    @Mock lateinit var partRepository: PartRepository
    @Mock lateinit var teamRepository: TeamRepository
    @Mock lateinit var cohortMemberRepository: CohortMemberRepository
    @Mock lateinit var depositHistoryRepository: DepositHistoryRepository
    @Mock lateinit var passwordEncoder: BCryptPasswordEncoder

    private fun service() = MemberService(
        memberRepository, cohortRepository, partRepository, teamRepository,
        cohortMemberRepository, depositHistoryRepository, passwordEncoder, 11
    )

    private val cohort = Cohort(id = 2, generation = 11, name = "11기")
    private val member = Member(id = 1, loginId = "user1", password = "hashed", name = "홍길동", phone = "010-1234-5678")

    @Test
    fun `getMember returns member info`() {
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        val result = service().getMember(1L)
        assertEquals("user1", result.loginId)
        assertEquals("홍길동", result.name)
    }

    @Test
    fun `getMember throws MEMBER_NOT_FOUND`() {
        whenever(memberRepository.findById(99L)).thenReturn(Optional.empty())
        val ex = assertThrows(BusinessException::class.java) { service().getMember(99L) }
        assertEquals(ErrorCode.MEMBER_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `createMember succeeds`() {
        val request = CreateMemberRequest("newuser", "pass", "이름", "010-0000-0000", 2L)
        whenever(memberRepository.existsByLoginId("newuser")).thenReturn(false)
        whenever(cohortRepository.findById(2L)).thenReturn(Optional.of(cohort))
        whenever(passwordEncoder.encode("pass")).thenReturn("hashed")
        whenever(memberRepository.save(any<Member>())).thenAnswer { it.arguments[0] as Member }
        whenever(cohortMemberRepository.save(any<CohortMember>())).thenAnswer { it.arguments[0] as CohortMember }
        whenever(depositHistoryRepository.save(any<DepositHistory>())).thenAnswer { it.arguments[0] as DepositHistory }

        val result = service().createMember(request)
        assertEquals("newuser", result.loginId)
        assertEquals(MemberStatus.ACTIVE, result.status)
        assertEquals(11, result.generation)
    }

    @Test
    fun `createMember fails with duplicate loginId`() {
        whenever(memberRepository.existsByLoginId("dup")).thenReturn(true)
        val ex = assertThrows(BusinessException::class.java) {
            service().createMember(CreateMemberRequest("dup", "p", "n", "ph", 2L))
        }
        assertEquals(ErrorCode.DUPLICATE_LOGIN_ID, ex.errorCode)
    }

    @Test
    fun `deleteMember changes status to WITHDRAWN`() {
        val m = Member(id = 1, loginId = "u", password = "p", name = "n", phone = "ph")
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(m))
        whenever(memberRepository.save(any<Member>())).thenAnswer { it.arguments[0] as Member }

        val result = service().deleteMember(1L)
        assertEquals(MemberStatus.WITHDRAWN, result.status)
    }

    @Test
    fun `deleteMember fails if already withdrawn`() {
        val m = Member(id = 1, loginId = "u", password = "p", name = "n", phone = "ph", status = MemberStatus.WITHDRAWN)
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(m))
        val ex = assertThrows(BusinessException::class.java) { service().deleteMember(1L) }
        assertEquals(ErrorCode.MEMBER_ALREADY_WITHDRAWN, ex.errorCode)
    }

    @Test
    fun `getMemberDetail returns member with cohort info`() {
        val cm = CohortMember(id = 1, member = member, cohort = cohort,
            part = Part(id = 1, cohort = cohort, name = "SERVER"),
            team = Team(id = 1, cohort = cohort, name = "Team A"))
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        whenever(cohortMemberRepository.findByMemberId(1L)).thenReturn(listOf(cm))

        val result = service().getMemberDetail(1L)
        assertEquals("홍길동", result.name)
        assertEquals(11, result.generation)
        assertEquals("SERVER", result.partName)
        assertEquals("Team A", result.teamName)
    }

    @Test
    fun `getMemberDetail returns null cohort info when not enrolled`() {
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        whenever(cohortMemberRepository.findByMemberId(1L)).thenReturn(emptyList())

        val result = service().getMemberDetail(1L)
        assertNull(result.generation)
        assertNull(result.partName)
    }

    @Test
    fun `updateMember updates name and phone`() {
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        whenever(memberRepository.save(any<Member>())).thenAnswer { it.arguments[0] as Member }
        whenever(cohortMemberRepository.findByMemberId(1L)).thenReturn(emptyList())

        val result = service().updateMember(1L, UpdateMemberRequest(name = "새이름", phone = "010-9999-9999"))
        assertEquals("새이름", result.name)
    }

    @Test
    fun `updateMember with cohortId creates new CohortMember`() {
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        whenever(cohortRepository.findById(2L)).thenReturn(Optional.of(cohort))
        whenever(cohortMemberRepository.findByMemberIdAndCohortId(1L, 2L)).thenReturn(null)
        whenever(cohortMemberRepository.save(any<CohortMember>())).thenAnswer { it.arguments[0] as CohortMember }
        whenever(depositHistoryRepository.save(any<DepositHistory>())).thenAnswer { it.arguments[0] as DepositHistory }
        whenever(memberRepository.save(any<Member>())).thenAnswer { it.arguments[0] as Member }
        whenever(cohortMemberRepository.findByMemberId(1L)).thenReturn(
            listOf(CohortMember(id = 1, member = member, cohort = cohort))
        )

        val result = service().updateMember(1L, UpdateMemberRequest(cohortId = 2L))
        assertEquals(11, result.generation)
    }

    @Test
    fun `getMembersDashboard returns paged results`() {
        val page = PageImpl(listOf(member))
        whenever(memberRepository.findAll(any<Specification<Member>>(), any<Pageable>()))
            .thenReturn(page)

        val cm = CohortMember(id = 1, member = member, cohort = cohort, deposit = 100000)
        whenever(cohortMemberRepository.findByMemberIdInWithDetails(listOf(1L)))
            .thenReturn(listOf(cm))

        val result = service().getMembersDashboard(0, 10, null, null, null, null, null, null)
        assertEquals(1, result.content.size)
        assertEquals("홍길동", result.content[0].name)
        assertEquals(1L, result.totalElements)
    }

    @Test
    fun `getMembersDashboard filters by generation`() {
        val page = PageImpl<Member>(emptyList())
        whenever(memberRepository.findAll(any<Specification<Member>>(), any<Pageable>()))
            .thenReturn(page)

        val result = service().getMembersDashboard(0, 10, null, null, 99, null, null, null)
        assertEquals(0, result.content.size)
    }
}
