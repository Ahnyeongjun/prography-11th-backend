package com.prography11thbackend.common

import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .status(e.errorCode.status)
            .body(ApiResponse.error(e.errorCode.name, e.errorCode.message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error("INVALID_INPUT", "입력값이 올바르지 않습니다"))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(e: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error("INVALID_INPUT", "입력값이 올바르지 않습니다"))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .internalServerError()
            .body(ApiResponse.error("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다"))
    }
}
