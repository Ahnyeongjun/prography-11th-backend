package com.prography11thbackend.service

import com.prography11thbackend.common.BusinessException
import com.prography11thbackend.common.ErrorCode
import com.prography11thbackend.dto.request.*
import com.prography11thbackend.dto.response.*
import com.prography11thbackend.entity.*
import com.prography11thbackend.repository.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional(readOnly = true)
class SessionService(
    private val sessionRepository: MeetingSessionRepository,
    private val cohortRepository: CohortRepository,
    private val qrCodeRepository: QrCodeRepository,
    private val attendanceRepository: AttendanceRepository,
    @Value("\${current-cohort.generation}") private val currentGeneration: Int
) {

    fun getSessions(): List<SessionResponse> {
        return sessionRepository.findByCohortGenerationAndStatusNot(currentGeneration, SessionStatus.CANCELLED)
            .map { toSessionResponse(it) }
    }

    fun getAdminSessions(dateFrom: LocalDate?, dateTo: LocalDate?, status: SessionStatus?): List<AdminSessionResponse> {
        val sessions = sessionRepository.findByGenerationWithFilters(currentGeneration, dateFrom, dateTo, status)
        if (sessions.isEmpty()) return emptyList()

        val sessionIds = sessions.map { it.id }
        val attendanceCounts = attendanceRepository.countBySessionIds(sessionIds)
            .associateBy { it.getSessionId() }
        val activeQrSessionIds = qrCodeRepository.findActiveSessionIds(sessionIds, Instant.now())
            .toSet()

        return sessions.map { s ->
            val counts = attendanceCounts[s.id]
            AdminSessionResponse(
                s.id, s.cohort.id, s.title, s.date, s.time, s.location, s.status,
                AttendanceSummaryDto(
                    present = counts?.getPresent()?.toInt() ?: 0,
                    absent = counts?.getAbsent()?.toInt() ?: 0,
                    late = counts?.getLate()?.toInt() ?: 0,
                    excused = counts?.getExcused()?.toInt() ?: 0,
                    total = counts?.getTotal()?.toInt() ?: 0
                ),
                s.id in activeQrSessionIds,
                s.createdAt, s.updatedAt
            )
        }
    }

    @Transactional
    fun createSession(request: CreateSessionRequest): AdminSessionResponse {
        val cohort = cohortRepository.findByGeneration(currentGeneration)
            ?: throw BusinessException(ErrorCode.COHORT_NOT_FOUND)

        val session = sessionRepository.save(MeetingSession(
            cohort = cohort, title = request.title,
            date = request.date, time = request.time, location = request.location
        ))

        qrCodeRepository.save(QrCode(
            session = session,
            hashValue = UUID.randomUUID().toString(),
            expiresAt = Instant.now().plus(24, ChronoUnit.HOURS)
        ))

        return toAdminSessionResponse(session)
    }

    @Transactional
    fun updateSession(id: Long, request: UpdateSessionRequest): AdminSessionResponse {
        val session = findSession(id)
        if (session.status == SessionStatus.CANCELLED) {
            throw BusinessException(ErrorCode.SESSION_ALREADY_CANCELLED)
        }

        request.title?.let { session.title = it }
        request.date?.let { session.date = it }
        request.time?.let { session.time = it }
        request.location?.let { session.location = it }
        request.status?.let { session.status = it }
        session.updatedAt = Instant.now()
        sessionRepository.save(session)

        return toAdminSessionResponse(session)
    }

    @Transactional
    fun deleteSession(id: Long): AdminSessionResponse {
        val session = findSession(id)
        if (session.status == SessionStatus.CANCELLED) {
            throw BusinessException(ErrorCode.SESSION_ALREADY_CANCELLED)
        }
        session.status = SessionStatus.CANCELLED
        session.updatedAt = Instant.now()
        sessionRepository.save(session)
        return toAdminSessionResponse(session)
    }

    @Transactional
    fun createQrCode(sessionId: Long): QrCodeResponse {
        val session = findSession(sessionId)
        val existing = qrCodeRepository.findBySessionIdAndExpiresAtAfter(sessionId, Instant.now())
        if (existing != null) {
            throw BusinessException(ErrorCode.QR_ALREADY_ACTIVE)
        }

        val qr = qrCodeRepository.save(QrCode(
            session = session,
            hashValue = UUID.randomUUID().toString(),
            expiresAt = Instant.now().plus(24, ChronoUnit.HOURS)
        ))
        return QrCodeResponse(qr.id, qr.session.id, qr.hashValue, qr.createdAt, qr.expiresAt)
    }

    @Transactional
    fun renewQrCode(qrCodeId: Long): QrCodeResponse {
        val oldQr = qrCodeRepository.findById(qrCodeId)
            .orElseThrow { BusinessException(ErrorCode.QR_NOT_FOUND) }

        oldQr.expiresAt = Instant.now()
        qrCodeRepository.save(oldQr)

        val newQr = qrCodeRepository.save(QrCode(
            session = oldQr.session,
            hashValue = UUID.randomUUID().toString(),
            expiresAt = Instant.now().plus(24, ChronoUnit.HOURS)
        ))
        return QrCodeResponse(newQr.id, newQr.session.id, newQr.hashValue, newQr.createdAt, newQr.expiresAt)
    }

    private fun findSession(id: Long): MeetingSession =
        sessionRepository.findById(id).orElseThrow { BusinessException(ErrorCode.SESSION_NOT_FOUND) }

    private fun toSessionResponse(s: MeetingSession) = SessionResponse(
        s.id, s.title, s.date, s.time, s.location, s.status, s.createdAt, s.updatedAt
    )

    private fun toAdminSessionResponse(s: MeetingSession): AdminSessionResponse {
        val counts = attendanceRepository.countBySessionId(s.id)
        val qrActive = qrCodeRepository.findBySessionIdAndExpiresAtAfter(s.id, Instant.now()) != null
        return AdminSessionResponse(
            s.id, s.cohort.id, s.title, s.date, s.time, s.location, s.status,
            AttendanceSummaryDto(
                present = counts.getPresent().toInt(),
                absent = counts.getAbsent().toInt(),
                late = counts.getLate().toInt(),
                excused = counts.getExcused().toInt(),
                total = counts.getTotal().toInt()
            ),
            qrActive, s.createdAt, s.updatedAt
        )
    }
}
