package com.prography11thbackend.config

import com.prography11thbackend.entity.*
import com.prography11thbackend.repository.*
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val memberRepository: MemberRepository,
    private val cohortRepository: CohortRepository,
    private val partRepository: PartRepository,
    private val teamRepository: TeamRepository,
    private val cohortMemberRepository: CohortMemberRepository,
    private val depositHistoryRepository: DepositHistoryRepository,
    private val passwordEncoder: BCryptPasswordEncoder
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val cohort10 = cohortRepository.save(Cohort(generation = 10, name = "10기"))
        val cohort11 = cohortRepository.save(Cohort(generation = 11, name = "11기"))

        val partNames = listOf("SERVER", "WEB", "iOS", "ANDROID", "DESIGN")
        for (name in partNames) {
            partRepository.save(Part(name = name, cohort = cohort10))
            partRepository.save(Part(name = name, cohort = cohort11))
        }

        teamRepository.save(Team(name = "Team A", cohort = cohort11))
        teamRepository.save(Team(name = "Team B", cohort = cohort11))
        teamRepository.save(Team(name = "Team C", cohort = cohort11))

        val admin = memberRepository.save(Member(
            loginId = "admin",
            password = passwordEncoder.encode("admin1234")!!,
            name = "관리자",
            phone = "010-0000-0000",
            role = MemberRole.ADMIN
        ))

        val cm = cohortMemberRepository.save(CohortMember(
            member = admin, cohort = cohort11
        ))

        depositHistoryRepository.save(DepositHistory(
            cohortMember = cm, type = DepositType.INITIAL,
            amount = 100000, balanceAfter = 100000, description = "초기 보증금"
        ))
    }
}
