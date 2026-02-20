package com.prography11thbackend.service

import com.prography11thbackend.common.BusinessException
import com.prography11thbackend.common.ErrorCode
import com.prography11thbackend.dto.request.CreateSessionRequest
import com.prography11thbackend.dto.request.UpdateSessionRequest
import com.prography11thbackend.dto.response.AttendanceSummaryProjection
import com.prography11thbackend.dto.response.SessionAttendanceCountProjection
import com.prography11thbackend.entity.*
import com.prography11thbackend.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*

@ExtendWith(MockitoExtension::class)
class SessionServiceTest {

    @Mock lateinit var sessionRepository: MeetingSessionRepository
    @Mock lateinit var cohortRepository: CohortRepository
    @Mock lateinit var qrCodeRepository: QrCodeRepository
    @Mock lateinit var attendanceRepository: AttendanceRepository

    private fun service() = SessionService(
        sessionRepository, cohortRepository, qrCodeRepository, attendanceRepository, 11
    )

    private val cohort = Cohort(id = 2, generation = 11, name = "11기")

    private fun emptyAttendanceSummary(): AttendanceSummaryProjection = object : AttendanceSummaryProjection {
        override fun getPresent() = 0L
        override fun getAbsent() = 0L
        override fun getLate() = 0L
        override fun getExcused() = 0L
        override fun getTotal() = 0L
    }

    @Test
    fun `createSession creates session with QR code`() {
        val request = CreateSessionRequest("Meeting", LocalDate.of(2026, 3, 1), LocalTime.of(14, 0), "Gangnam")
        whenever(cohortRepository.findByGeneration(11)).thenReturn(cohort)
        whenever(sessionRepository.save(any<MeetingSession>())).thenAnswer {
            val s = it.arguments[0] as MeetingSession
            MeetingSession(id = 1, cohort = s.cohort, title = s.title, date = s.date, time = s.time, location = s.location)
        }
        whenever(qrCodeRepository.save(any<QrCode>())).thenAnswer { it.arguments[0] as QrCode }
        whenever(attendanceRepository.countBySessionId(any())).thenReturn(emptyAttendanceSummary())
        whenever(qrCodeRepository.findBySessionIdAndExpiresAtAfter(any(), any())).thenReturn(null)

        val result = service().createSession(request)
        assertEquals("Meeting", result.title)
        assertEquals(SessionStatus.SCHEDULED, result.status)
        verify(qrCodeRepository).save(any<QrCode>())
    }

    @Test
    fun `updateSession fails for cancelled session`() {
        val session = MeetingSession(id = 1, cohort = cohort, title = "T", date = LocalDate.now(),
            time = LocalTime.now(), location = "L", status = SessionStatus.CANCELLED)
        whenever(sessionRepository.findById(1L)).thenReturn(Optional.of(session))

        val ex = assertThrows(BusinessException::class.java) {
            service().updateSession(1L, UpdateSessionRequest(title = "New"))
        }
        assertEquals(ErrorCode.SESSION_ALREADY_CANCELLED, ex.errorCode)
    }

    @Test
    fun `deleteSession changes status to CANCELLED`() {
        val session = MeetingSession(id = 1, cohort = cohort, title = "T", date = LocalDate.now(),
            time = LocalTime.now(), location = "L")
        whenever(sessionRepository.findById(1L)).thenReturn(Optional.of(session))
        whenever(sessionRepository.save(any<MeetingSession>())).thenAnswer { it.arguments[0] as MeetingSession }
        whenever(attendanceRepository.countBySessionId(any())).thenReturn(emptyAttendanceSummary())
        whenever(qrCodeRepository.findBySessionIdAndExpiresAtAfter(any(), any())).thenReturn(null)

        val result = service().deleteSession(1L)
        assertEquals(SessionStatus.CANCELLED, result.status)
    }

    @Test
    fun `getSessions excludes CANCELLED sessions`() {
        val session = MeetingSession(id = 1, cohort = cohort, title = "T", date = LocalDate.now(),
            time = LocalTime.now(), location = "L")
        whenever(sessionRepository.findByCohortGenerationAndStatusNot(11, SessionStatus.CANCELLED))
            .thenReturn(listOf(session))

        val result = service().getSessions()
        assertEquals(1, result.size)
    }

    @Test
    fun `getAdminSessions filters by date and status`() {
        val s1 = MeetingSession(id = 1, cohort = cohort, title = "A", date = LocalDate.of(2026, 3, 1),
            time = LocalTime.of(14, 0), location = "L")
        val s2 = MeetingSession(id = 2, cohort = cohort, title = "B", date = LocalDate.of(2026, 3, 15),
            time = LocalTime.of(14, 0), location = "L")
        whenever(sessionRepository.findByGenerationWithFilters(eq(11), eq(LocalDate.of(2026, 3, 10)), isNull(), isNull()))
            .thenReturn(listOf(s2))
        val countProjection = object : SessionAttendanceCountProjection {
            override fun getSessionId() = 2L
            override fun getPresent() = 0L
            override fun getAbsent() = 0L
            override fun getLate() = 0L
            override fun getExcused() = 0L
            override fun getTotal() = 0L
        }
        whenever(attendanceRepository.countBySessionIds(listOf(2L))).thenReturn(listOf(countProjection))
        whenever(qrCodeRepository.findActiveSessionIds(eq(listOf(2L)), any())).thenReturn(emptyList())

        val result = service().getAdminSessions(LocalDate.of(2026, 3, 10), null, null)
        assertEquals(1, result.size)
        assertEquals("B", result[0].title)
    }

    @Test
    fun `updateSession updates fields successfully`() {
        val session = MeetingSession(id = 1, cohort = cohort, title = "Old", date = LocalDate.now(),
            time = LocalTime.now(), location = "Old Location")
        whenever(sessionRepository.findById(1L)).thenReturn(Optional.of(session))
        whenever(sessionRepository.save(any<MeetingSession>())).thenAnswer { it.arguments[0] as MeetingSession }
        whenever(attendanceRepository.countBySessionId(any())).thenReturn(emptyAttendanceSummary())
        whenever(qrCodeRepository.findBySessionIdAndExpiresAtAfter(any(), any())).thenReturn(null)

        val result = service().updateSession(1L, UpdateSessionRequest(title = "New Title", location = "New Location"))
        assertEquals("New Title", result.title)
    }

    @Test
    fun `createQrCode creates QR when no active QR exists`() {
        val session = MeetingSession(id = 1, cohort = cohort, title = "T", date = LocalDate.now(),
            time = LocalTime.now(), location = "L")
        whenever(sessionRepository.findById(1L)).thenReturn(Optional.of(session))
        whenever(qrCodeRepository.findBySessionIdAndExpiresAtAfter(eq(1L), any())).thenReturn(null)
        whenever(qrCodeRepository.save(any<QrCode>())).thenAnswer {
            val qr = it.arguments[0] as QrCode
            QrCode(id = 1, session = qr.session, hashValue = qr.hashValue, expiresAt = qr.expiresAt)
        }

        val result = service().createQrCode(1L)
        assertEquals(1L, result.sessionId)
        assertNotNull(result.hashValue)
    }

    @Test
    fun `createQrCode fails when active QR already exists`() {
        val session = MeetingSession(id = 1, cohort = cohort, title = "T", date = LocalDate.now(),
            time = LocalTime.now(), location = "L")
        val activeQr = QrCode(id = 1, session = session, hashValue = "abc",
            expiresAt = Instant.now().plus(12, ChronoUnit.HOURS))
        whenever(sessionRepository.findById(1L)).thenReturn(Optional.of(session))
        whenever(qrCodeRepository.findBySessionIdAndExpiresAtAfter(eq(1L), any())).thenReturn(activeQr)

        val ex = assertThrows(BusinessException::class.java) {
            service().createQrCode(1L)
        }
        assertEquals(ErrorCode.QR_ALREADY_ACTIVE, ex.errorCode)
    }

    @Test
    fun `renewQrCode expires old QR and creates new one`() {
        val session = MeetingSession(id = 1, cohort = cohort, title = "T", date = LocalDate.now(),
            time = LocalTime.now(), location = "L")
        val oldQr = QrCode(id = 1, session = session, hashValue = "old-hash",
            expiresAt = Instant.now().plus(12, ChronoUnit.HOURS))
        whenever(qrCodeRepository.findById(1L)).thenReturn(Optional.of(oldQr))
        whenever(qrCodeRepository.save(any<QrCode>())).thenAnswer {
            val qr = it.arguments[0] as QrCode
            if (qr.id == 1L) qr else QrCode(id = 2, session = qr.session, hashValue = qr.hashValue, expiresAt = qr.expiresAt)
        }

        val result = service().renewQrCode(1L)
        assertEquals(2L, result.id)
        assertNotEquals("old-hash", result.hashValue)
    }

    @Test
    fun `renewQrCode fails when QR not found`() {
        whenever(qrCodeRepository.findById(99L)).thenReturn(Optional.empty())

        val ex = assertThrows(BusinessException::class.java) {
            service().renewQrCode(99L)
        }
        assertEquals(ErrorCode.QR_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `deleteSession fails when already cancelled`() {
        val session = MeetingSession(id = 1, cohort = cohort, title = "T", date = LocalDate.now(),
            time = LocalTime.now(), location = "L", status = SessionStatus.CANCELLED)
        whenever(sessionRepository.findById(1L)).thenReturn(Optional.of(session))

        val ex = assertThrows(BusinessException::class.java) {
            service().deleteSession(1L)
        }
        assertEquals(ErrorCode.SESSION_ALREADY_CANCELLED, ex.errorCode)
    }
}
