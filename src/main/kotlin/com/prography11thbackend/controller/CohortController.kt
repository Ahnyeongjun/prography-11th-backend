package com.prography11thbackend.controller

import com.prography11thbackend.common.ApiResponse
import com.prography11thbackend.dto.response.CohortDetailResponse
import com.prography11thbackend.dto.response.CohortResponse
import com.prography11thbackend.service.CohortService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/cohorts")
class CohortController(private val cohortService: CohortService) {

    @GetMapping
    fun getCohorts(): ApiResponse<List<CohortResponse>> {
        return ApiResponse.success(cohortService.getCohorts())
    }

    @GetMapping("/{id}")
    fun getCohortDetail(@PathVariable id: Long): ApiResponse<CohortDetailResponse> {
        return ApiResponse.success(cohortService.getCohortDetail(id))
    }
}
