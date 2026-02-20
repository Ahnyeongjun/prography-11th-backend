package com.prography11thbackend.controller

import com.prography11thbackend.common.ApiResponse
import com.prography11thbackend.dto.request.LoginRequest
import com.prography11thbackend.dto.response.MemberResponse
import com.prography11thbackend.service.AuthService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ApiResponse<MemberResponse> {
        return ApiResponse.success(authService.login(request))
    }
}
