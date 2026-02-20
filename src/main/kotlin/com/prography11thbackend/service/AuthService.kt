package com.prography11thbackend.service

import com.prography11thbackend.common.BusinessException
import com.prography11thbackend.common.ErrorCode
import com.prography11thbackend.dto.request.LoginRequest
import com.prography11thbackend.dto.response.MemberResponse
import com.prography11thbackend.entity.MemberStatus
import com.prography11thbackend.repository.MemberRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: BCryptPasswordEncoder
) {
    fun login(request: LoginRequest): MemberResponse {
        val member = memberRepository.findByLoginId(request.loginId)
            ?: throw BusinessException(ErrorCode.LOGIN_FAILED)

        if (!passwordEncoder.matches(request.password, member.password)) {
            throw BusinessException(ErrorCode.LOGIN_FAILED)
        }

        if (member.status == MemberStatus.WITHDRAWN) {
            throw BusinessException(ErrorCode.MEMBER_WITHDRAWN)
        }

        return MemberResponse(
            member.id, member.loginId, member.name, member.phone,
            member.status, member.role, member.createdAt, member.updatedAt
        )
    }
}
