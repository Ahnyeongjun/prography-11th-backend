package com.prography11thbackend.controller

import com.prography11thbackend.common.ApiResponse
import com.prography11thbackend.dto.request.*
import com.prography11thbackend.dto.response.*
import com.prography11thbackend.entity.SessionStatus
import com.prography11thbackend.service.SessionService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1")
class SessionController(private val sessionService: SessionService) {

    @GetMapping("/sessions")
    fun getSessions(): ApiResponse<List<SessionResponse>> {
        return ApiResponse.success(sessionService.getSessions())
    }

    @GetMapping("/admin/sessions")
    fun getAdminSessions(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateFrom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateTo: LocalDate?,
        @RequestParam(required = false) status: SessionStatus?
    ): ApiResponse<List<AdminSessionResponse>> {
        return ApiResponse.success(sessionService.getAdminSessions(dateFrom, dateTo, status))
    }

    @PostMapping("/admin/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    fun createSession(@RequestBody request: CreateSessionRequest): ApiResponse<AdminSessionResponse> {
        return ApiResponse.success(sessionService.createSession(request))
    }

    @PutMapping("/admin/sessions/{id}")
    fun updateSession(@PathVariable id: Long, @RequestBody request: UpdateSessionRequest): ApiResponse<AdminSessionResponse> {
        return ApiResponse.success(sessionService.updateSession(id, request))
    }

    @DeleteMapping("/admin/sessions/{id}")
    fun deleteSession(@PathVariable id: Long): ApiResponse<AdminSessionResponse> {
        return ApiResponse.success(sessionService.deleteSession(id))
    }

    @PostMapping("/admin/sessions/{sessionId}/qrcodes")
    @ResponseStatus(HttpStatus.CREATED)
    fun createQrCode(@PathVariable sessionId: Long): ApiResponse<QrCodeResponse> {
        return ApiResponse.success(sessionService.createQrCode(sessionId))
    }

    @PutMapping("/admin/qrcodes/{qrCodeId}")
    fun renewQrCode(@PathVariable qrCodeId: Long): ApiResponse<QrCodeResponse> {
        return ApiResponse.success(sessionService.renewQrCode(qrCodeId))
    }
}
