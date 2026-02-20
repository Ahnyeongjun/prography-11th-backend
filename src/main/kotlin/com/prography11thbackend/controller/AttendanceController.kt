package com.prography11thbackend.controller

import com.prography11thbackend.common.ApiResponse
import com.prography11thbackend.dto.request.*
import com.prography11thbackend.dto.response.*
import com.prography11thbackend.service.AttendanceService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class AttendanceController(private val attendanceService: AttendanceService) {

    @PostMapping("/attendances")
    @ResponseStatus(HttpStatus.CREATED)
    fun checkIn(@RequestBody request: CheckInRequest): ApiResponse<AttendanceResponse> {
        return ApiResponse.success(attendanceService.checkIn(request))
    }

    @GetMapping("/attendances")
    fun getMyAttendances(@RequestParam memberId: Long): ApiResponse<List<MyAttendanceResponse>> {
        return ApiResponse.success(attendanceService.getMyAttendances(memberId))
    }

    @GetMapping("/members/{memberId}/attendance-summary")
    fun getAttendanceSummary(@PathVariable memberId: Long): ApiResponse<AttendanceSummaryResponse> {
        return ApiResponse.success(attendanceService.getAttendanceSummary(memberId))
    }

    @PostMapping("/admin/attendances")
    @ResponseStatus(HttpStatus.CREATED)
    fun registerAttendance(@RequestBody request: CreateAttendanceRequest): ApiResponse<AttendanceResponse> {
        return ApiResponse.success(attendanceService.registerAttendance(request))
    }

    @PutMapping("/admin/attendances/{id}")
    fun updateAttendance(@PathVariable id: Long, @RequestBody request: UpdateAttendanceRequest): ApiResponse<AttendanceResponse> {
        return ApiResponse.success(attendanceService.updateAttendance(id, request))
    }

    @GetMapping("/admin/attendances/sessions/{sessionId}/summary")
    fun getSessionAttendanceSummary(@PathVariable sessionId: Long): ApiResponse<List<MemberAttendanceSummaryItem>> {
        return ApiResponse.success(attendanceService.getSessionAttendanceSummary(sessionId))
    }

    @GetMapping("/admin/attendances/members/{memberId}")
    fun getMemberAttendanceDetail(@PathVariable memberId: Long): ApiResponse<MemberAttendanceDetailResponse> {
        return ApiResponse.success(attendanceService.getMemberAttendanceDetail(memberId))
    }

    @GetMapping("/admin/attendances/sessions/{sessionId}")
    fun getSessionAttendances(@PathVariable sessionId: Long): ApiResponse<SessionAttendanceResponse> {
        return ApiResponse.success(attendanceService.getSessionAttendances(sessionId))
    }

    @GetMapping("/admin/cohort-members/{cohortMemberId}/deposits")
    fun getDepositHistory(@PathVariable cohortMemberId: Long): ApiResponse<List<DepositHistoryResponse>> {
        return ApiResponse.success(attendanceService.getDepositHistory(cohortMemberId))
    }
}
