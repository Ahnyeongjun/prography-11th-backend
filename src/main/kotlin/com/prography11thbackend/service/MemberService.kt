package com.prography11thbackend.service

import com.prography11thbackend.common.BusinessException
import com.prography11thbackend.common.ErrorCode
import com.prography11thbackend.common.PageResponse
import com.prography11thbackend.dto.request.*
import com.prography11thbackend.dto.response.*
import com.prography11thbackend.entity.*
import com.prography11thbackend.repository.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val cohortRepository: CohortRepository,
    private val partRepository: PartRepository,
    private val teamRepository: TeamRepository,
    private val cohortMemberRepository: CohortMemberRepository,
    private val depositHistoryRepository: DepositHistoryRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    @Value("\${current-cohort.generation}") private val currentGeneration: Int
) {

    fun getMember(id: Long): MemberResponse {
        val member = findMember(id)
        return toMemberResponse(member)
    }

    @Transactional
    fun createMember(request: CreateMemberRequest): MemberDetailResponse {
        if (memberRepository.existsByLoginId(request.loginId)) {
            throw BusinessException(ErrorCode.DUPLICATE_LOGIN_ID)
        }

        val cohort = cohortRepository.findById(request.cohortId)
            .orElseThrow { BusinessException(ErrorCode.COHORT_NOT_FOUND) }
        val part = request.partId?.let {
            partRepository.findById(it).orElseThrow { BusinessException(ErrorCode.PART_NOT_FOUND) }
        }
        val team = request.teamId?.let {
            teamRepository.findById(it).orElseThrow { BusinessException(ErrorCode.TEAM_NOT_FOUND) }
        }

        val member = memberRepository.save(Member(
            loginId = request.loginId,
            password = passwordEncoder.encode(request.password)!!,
            name = request.name,
            phone = request.phone
        ))

        val cm = cohortMemberRepository.save(CohortMember(
            member = member, cohort = cohort, part = part, team = team
        ))

        depositHistoryRepository.save(DepositHistory(
            cohortMember = cm, type = DepositType.INITIAL,
            amount = 100000, balanceAfter = 100000, description = "초기 보증금"
        ))

        return MemberDetailResponse(
            member.id, member.loginId, member.name, member.phone,
            member.status, member.role,
            cohort.generation, part?.name, team?.name,
            member.createdAt, member.updatedAt
        )
    }

    fun getMembersDashboard(
        page: Int, size: Int,
        searchType: String?, searchValue: String?,
        generation: Int?, partName: String?, teamName: String?,
        status: MemberStatus?
    ): PageResponse<MemberDashboardItem> {
        val effectiveSize = if (size > 0) size else 10
        val pageable = PageRequest.of(page, effectiveSize)
        val spec = buildDashboardSpec(searchType, searchValue, status, generation, partName, teamName)

        val memberPage = memberRepository.findAll(spec, pageable)

        val memberIds = memberPage.content.map { it.id }
        val cohortMemberMap = if (memberIds.isEmpty()) {
            emptyMap()
        } else {
            val cohortMembers = cohortMemberRepository.findByMemberIdInWithDetails(memberIds)
            if (generation != null) {
                cohortMembers.filter { it.cohort.generation == generation }
                    .associateBy { it.member.id }
            } else {
                cohortMembers.groupBy { it.member.id }
                    .mapValues { (_, cms) -> cms.minByOrNull { it.id }!! }
            }
        }

        val content = memberPage.content.map { member ->
            val cm = cohortMemberMap[member.id]
            MemberDashboardItem(
                member.id, member.loginId, member.name, member.phone,
                member.status, member.role,
                cm?.cohort?.generation, cm?.part?.name, cm?.team?.name, cm?.deposit,
                member.createdAt, member.updatedAt
            )
        }

        return PageResponse(content, page, effectiveSize, memberPage.totalElements, memberPage.totalPages)
    }

    fun getMemberDetail(id: Long): MemberDetailResponse {
        val member = findMember(id)
        val cm = cohortMemberRepository.findByMemberId(member.id).firstOrNull()
        return MemberDetailResponse(
            member.id, member.loginId, member.name, member.phone,
            member.status, member.role,
            cm?.cohort?.generation, cm?.part?.name, cm?.team?.name,
            member.createdAt, member.updatedAt
        )
    }

    @Transactional
    fun updateMember(id: Long, request: UpdateMemberRequest): MemberDetailResponse {
        val member = findMember(id)
        request.name?.let { member.name = it }
        request.phone?.let { member.phone = it }
        member.updatedAt = Instant.now()

        val cohortId = request.cohortId
        if (cohortId != null) {
            val cohort = cohortRepository.findById(cohortId)
                .orElseThrow { BusinessException(ErrorCode.COHORT_NOT_FOUND) }
            val part = request.partId?.let {
                partRepository.findById(it).orElseThrow { BusinessException(ErrorCode.PART_NOT_FOUND) }
            }
            val team = request.teamId?.let {
                teamRepository.findById(it).orElseThrow { BusinessException(ErrorCode.TEAM_NOT_FOUND) }
            }
            val existing = cohortMemberRepository.findByMemberIdAndCohortId(member.id, cohort.id)
            if (existing != null) {
                existing.part = part
                existing.team = team
            } else {
                val cm = cohortMemberRepository.save(CohortMember(
                    member = member, cohort = cohort, part = part, team = team
                ))
                depositHistoryRepository.save(DepositHistory(
                    cohortMember = cm, type = DepositType.INITIAL,
                    amount = 100000, balanceAfter = 100000, description = "초기 보증금"
                ))
            }
        }

        memberRepository.save(member)
        val cm = cohortMemberRepository.findByMemberId(member.id).firstOrNull()
        return MemberDetailResponse(
            member.id, member.loginId, member.name, member.phone,
            member.status, member.role,
            cm?.cohort?.generation, cm?.part?.name, cm?.team?.name,
            member.createdAt, member.updatedAt
        )
    }

    @Transactional
    fun deleteMember(id: Long): MemberDeleteResponse {
        val member = findMember(id)
        if (member.status == MemberStatus.WITHDRAWN) {
            throw BusinessException(ErrorCode.MEMBER_ALREADY_WITHDRAWN)
        }
        member.status = MemberStatus.WITHDRAWN
        member.updatedAt = Instant.now()
        memberRepository.save(member)
        return MemberDeleteResponse(member.id, member.loginId, member.name, member.status, member.updatedAt)
    }

    private fun buildDashboardSpec(
        searchType: String?, searchValue: String?,
        status: MemberStatus?,
        generation: Int?, partName: String?, teamName: String?
    ): Specification<Member> {
        return Specification { root, query, cb ->
            val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()

            status?.let { predicates.add(cb.equal(root.get<MemberStatus>("status"), it)) }

            if (searchType != null && !searchValue.isNullOrBlank()) {
                when (searchType) {
                    "name" -> predicates.add(cb.like(root.get("name"), "%$searchValue%"))
                    "loginId" -> predicates.add(cb.like(root.get("loginId"), "%$searchValue%"))
                    "phone" -> predicates.add(cb.like(root.get("phone"), "%$searchValue%"))
                }
            }

            if (generation != null || partName != null || teamName != null) {
                val subquery = query!!.subquery(Long::class.java)
                val cm = subquery.from(CohortMember::class.java)
                val subPredicates = mutableListOf<jakarta.persistence.criteria.Predicate>()

                subPredicates.add(cb.equal(cm.get<Member>("member"), root))
                generation?.let {
                    subPredicates.add(cb.equal(cm.get<Cohort>("cohort").get<Int>("generation"), it))
                }
                partName?.let {
                    subPredicates.add(cb.equal(cm.get<Part>("part").get<String>("name"), it))
                }
                teamName?.let {
                    subPredicates.add(cb.equal(cm.get<Team>("team").get<String>("name"), it))
                }

                subquery.select(cm.get("id"))
                subquery.where(*subPredicates.toTypedArray())
                predicates.add(cb.exists(subquery))
            }

            if (predicates.isEmpty()) null else cb.and(*predicates.toTypedArray())
        }
    }

    private fun findMember(id: Long): Member =
        memberRepository.findById(id).orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }

    private fun toMemberResponse(m: Member) = MemberResponse(
        m.id, m.loginId, m.name, m.phone, m.status, m.role, m.createdAt, m.updatedAt
    )
}
