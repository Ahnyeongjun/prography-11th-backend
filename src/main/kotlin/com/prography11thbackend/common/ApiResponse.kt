package com.prography11thbackend.common

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ErrorDetail?
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> = ApiResponse(true, data, null)
        fun <T> error(code: String, message: String): ApiResponse<T> = ApiResponse(false, null, ErrorDetail(code, message))
    }
}

data class ErrorDetail(
    val code: String,
    val message: String
)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
