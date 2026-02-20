package com.prography11thbackend.repository

import com.prography11thbackend.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface MemberRepository : JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {
    fun findByLoginId(loginId: String): Member?
    fun existsByLoginId(loginId: String): Boolean
}
