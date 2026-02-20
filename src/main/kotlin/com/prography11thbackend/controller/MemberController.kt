package com.prography11thbackend.controller

import com.prography11thbackend.common.ApiResponse
import com.prography11thbackend.common.PageResponse
import com.prography11thbackend.dto.request.*
import com.prography11thbackend.dto.response.*
import com.prography11thbackend.entity.MemberStatus
import com.prography11thbackend.service.MemberService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class MemberController(private val memberService: MemberService) {

    @GetMapping("/members/{id}")
    fun getMember(@PathVariable id: Long): ApiResponse<MemberResponse> {
        return ApiResponse.success(memberService.getMember(id))
    }

    @PostMapping("/admin/members")
    @ResponseStatus(HttpStatus.CREATED)
    fun createMember(@RequestBody request: CreateMemberRequest): ApiResponse<MemberDetailResponse> {
        return ApiResponse.success(memberService.createMember(request))
    }

    @GetMapping("/admin/members")
    fun getMembersDashboard(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) searchType: String?,
        @RequestParam(required = false) searchValue: String?,
        @RequestParam(required = false) generation: Int?,
        @RequestParam(required = false) partName: String?,
        @RequestParam(required = false) teamName: String?,
        @RequestParam(required = false) status: MemberStatus?
    ): ApiResponse<PageResponse<MemberDashboardItem>> {
        return ApiResponse.success(
            memberService.getMembersDashboard(page, size, searchType, searchValue, generation, partName, teamName, status)
        )
    }

    @GetMapping("/admin/members/{id}")
    fun getMemberDetail(@PathVariable id: Long): ApiResponse<MemberDetailResponse> {
        return ApiResponse.success(memberService.getMemberDetail(id))
    }

    @PutMapping("/admin/members/{id}")
    fun updateMember(@PathVariable id: Long, @RequestBody request: UpdateMemberRequest): ApiResponse<MemberDetailResponse> {
        return ApiResponse.success(memberService.updateMember(id, request))
    }

    @DeleteMapping("/admin/members/{id}")
    fun deleteMember(@PathVariable id: Long): ApiResponse<MemberDeleteResponse> {
        return ApiResponse.success(memberService.deleteMember(id))
    }
}
