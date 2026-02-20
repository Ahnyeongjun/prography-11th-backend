package com.prography11thbackend.service

import com.prography11thbackend.common.BusinessException
import com.prography11thbackend.common.ErrorCode
import com.prography11thbackend.dto.request.LoginRequest
import com.prography11thbackend.entity.Member
import com.prography11thbackend.entity.MemberRole
import com.prography11thbackend.entity.MemberStatus
import com.prography11thbackend.repository.MemberRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock lateinit var memberRepository: MemberRepository
    @Mock lateinit var passwordEncoder: BCryptPasswordEncoder
    @InjectMocks lateinit var authService: AuthService

    private fun createMember(
        status: MemberStatus = MemberStatus.ACTIVE,
        role: MemberRole = MemberRole.ADMIN
    ) = Member(id = 1, loginId = "admin", password = "hashed", name = "관리자", phone = "010-0000-0000", status = status, role = role)

    @Test
    fun `login succeeds with valid credentials`() {
        val member = createMember()
        whenever(memberRepository.findByLoginId("admin")).thenReturn(member)
        whenever(passwordEncoder.matches("admin1234", "hashed")).thenReturn(true)

        val result = authService.login(LoginRequest("admin", "admin1234"))
        assertEquals("admin", result.loginId)
        assertEquals(MemberStatus.ACTIVE, result.status)
    }

    @Test
    fun `login fails with wrong loginId`() {
        whenever(memberRepository.findByLoginId("wrong")).thenReturn(null)
        val ex = assertThrows(BusinessException::class.java) {
            authService.login(LoginRequest("wrong", "admin1234"))
        }
        assertEquals(ErrorCode.LOGIN_FAILED, ex.errorCode)
    }

    @Test
    fun `login fails with wrong password`() {
        val member = createMember()
        whenever(memberRepository.findByLoginId("admin")).thenReturn(member)
        whenever(passwordEncoder.matches("wrong", "hashed")).thenReturn(false)

        val ex = assertThrows(BusinessException::class.java) {
            authService.login(LoginRequest("admin", "wrong"))
        }
        assertEquals(ErrorCode.LOGIN_FAILED, ex.errorCode)
    }

    @Test
    fun `login fails for withdrawn member`() {
        val member = createMember(status = MemberStatus.WITHDRAWN)
        whenever(memberRepository.findByLoginId("admin")).thenReturn(member)
        whenever(passwordEncoder.matches("admin1234", "hashed")).thenReturn(true)

        val ex = assertThrows(BusinessException::class.java) {
            authService.login(LoginRequest("admin", "admin1234"))
        }
        assertEquals(ErrorCode.MEMBER_WITHDRAWN, ex.errorCode)
    }
}
