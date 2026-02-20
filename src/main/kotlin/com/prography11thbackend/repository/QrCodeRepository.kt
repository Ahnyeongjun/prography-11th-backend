package com.prography11thbackend.repository

import com.prography11thbackend.entity.QrCode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface QrCodeRepository : JpaRepository<QrCode, Long> {
    fun findByHashValue(hashValue: String): QrCode?
    fun findBySessionIdAndExpiresAtAfter(sessionId: Long, now: Instant): QrCode?

    @Query("""
        SELECT q.session.id
        FROM QrCode q
        WHERE q.session.id IN :sessionIds
        AND q.expiresAt > :now
    """)
    fun findActiveSessionIds(
        @Param("sessionIds") sessionIds: List<Long>,
        @Param("now") now: Instant
    ): List<Long>
}
