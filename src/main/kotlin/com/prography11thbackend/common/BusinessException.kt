package com.prography11thbackend.common

class BusinessException(val errorCode: ErrorCode) : RuntimeException(errorCode.message)
