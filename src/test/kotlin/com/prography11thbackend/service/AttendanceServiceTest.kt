package com.prography11thbackend.service

import com.prography11thbackend.common.BusinessException
import com.prography11thbackend.common.ErrorCode
import com.prography11thbackend.dto.request.CheckInRequest
import com.prography11thbackend.dto.request.CreateAttendanceRequest
import com.prography11thbackend.dto.request.UpdateAttendanceRequest
import com.prography11thbackend.dto.response.CohortMemberAttendanceSummaryProjection
import com.prography11thbackend.dto.response.MemberAttendanceSummaryProjection
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
class AttendanceServiceTest {

    @Mock lateinit var attendanceRepository: AttendanceRepository
    @Mock lateinit var memberRepository: MemberRepository
    @Mock lateinit var sessionRepository: MeetingSessionRepository
    @Mock lateinit var qrCodeRepository: QrCodeRepository
    @Mock lateinit var cohortMemberRepository: CohortMemberRepository
    @Mock lateinit var depositHistoryRepository: DepositHistoryRepository

    private fun service() = AttendanceService(
        attendanceRepository, memberRepository, sessionRepository,
        qrCodeRepository, cohortMemberRepository, depositHistoryRepository, 11
    )

    private val cohort = Cohort(id = 2, generation = 11, name = "11기")
    private val member = Member(id = 1, loginId = "u", password = "p", name = "n", phone = "ph")
    private val session = MeetingSession(id = 1, cohort = cohort, title = "T",
        date = LocalDate.now(), time = LocalTime.now(), location = "L", status = SessionStatus.IN_PROGRESS)
    private val cm = CohortMember(id = 1, member = member, cohort = cohort, deposit = 100000)

    @Test
    fun `registerAttendance PRESENT creates attendance with no penalty`() {
        whenever(sessionRepository.findById(1L)).thenReturn(Optional.of(session))
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        whenever(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false)
        whenever(cohortMemberRepository.findByMemberIdAndCohortGeneration(1L, 11)).thenReturn(cm)
        whenever(attendanceRepository.save(any<Attendance>())).thenAnswer {
            val a = it.arguments[0] as Attendance
            Attendance(id = 1, session = a.session, member = a.member, status = a.status, penaltyAmount = a.penaltyAmount)
        }
        whenever(cohortMemberRepository.save(any<CohortMember>())).thenAnswer { it.arguments[0] }

        val result = service().registerAttendance(CreateAttendanceRequest(1, 1, AttendanceStatus.PRESENT))
        assertEquals(AttendanceStatus.PRESENT, result.status)
        assertEquals(0, result.penaltyAmount)
    }

    @Test
    fun `registerAttendance ABSENT deducts 10000 penalty`() {
        whenever(sessionRepository.findById(1L)).thenReturn(Optional.of(session))
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        whenever(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false)
        whenever(cohortMemberRepository.findByMemberIdAndCohortGeneration(1L, 11)).thenReturn(cm)
        whenever(attendanceRepository.save(any<Attendance>())).thenAnswer {
            val a = it.arguments[0] as Attendance
            Attendance(id = 1, session = a.session, member = a.member, status = a.status, penaltyAmount = a.penaltyAmount)
        }
        whenever(cohortMemberRepository.save(any<CohortMember>())).thenAnswer { it.arguments[0] }
        whenever(depositHistoryRepository.save(any<DepositHistory>())).thenAnswer { it.arguments[0] }

        val result = service().registerAttendance(CreateAttendanceRequest(1, 1, AttendanceStatus.ABSENT))
        assertEquals(AttendanceStatus.ABSENT, result.status)
        assertEquals(10000, result.penaltyAmount)
        assertEquals(90000, cm.deposit)
    }

    @Test
    fun `registerAttendance LATE calculates penalty correctly`() {
        whenever(sessionRepository.findById(1L)).thenReturn(Optional.of(session))
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        whenever(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false)
        whenever(cohortMemberRepository.findByMemberIdAndCohortGeneration(1L, 11)).thenReturn(cm)
        whenever(attendanceRepository.save(any<Attendance>())).thenAnswer {
            val a = it.arguments[0] as Attendance
            Attendance(id = 1, session = a.session, member = a.member, status = a.status,
                lateMinutes = a.lateMinutes, penaltyAmount = a.penaltyAmount)
        }
        whenever(cohortMemberRepository.save(any<CohortMember>())).thenAnswer { it.arguments[0] }
        whenever(depositHistoryRepository.save(any<DepositHistory>())).thenAnswer { it.arguments[0] }

        val result = service().registerAttendance(
            CreateAttendanceRequest(1, 1, AttendanceStatus.LATE, lateMinutes = 5)
        )
        assertEquals(2500, result.penaltyAmount) // 5 * 500
    }

    @Test
    fun `registerAttendance LATE caps penalty at 10000`() {
        whenever(sessionRepository.findById(1L)).thenReturn(Optional.of(session))
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        whenever(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false)
        whenever(cohortMemberRepository.findByMemberIdAndCohortGeneration(1L, 11)).thenReturn(cm)
        whenever(attendanceRepository.save(any<Attendance>())).thenAnswer {
            val a = it.arguments[0] as Attendance
            Attendance(id = 1, session = a.session, member = a.member, status = a.status,
                lateMinutes = a.lateMinutes, penaltyAmount = a.penaltyAmount)
        }
        whenever(cohortMemberRepository.save(any<CohortMember>())).thenAnswer { it.arguments[0] }
        whenever(depositHistoryRepository.save(any<DepositHistory>())).thenAnswer { it.arguments[0] }

        val result = service().registerAttendance(
            CreateAttendanceRequest(1, 1, AttendanceStatus.LATE, lateMinutes = 30)
        )
        assertEquals(10000, result.penaltyAmount) // min(30*500, 10000)
    }

    @Test
    fun `registerAttendance fails with duplicate attendance`() {
        whenever(sessionRepository.findById(1L)).thenReturn(Optional.of(session))
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        whenever(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(true)

        val ex = assertThrows(BusinessException::class.java) {
            service().registerAttendance(CreateAttendanceRequest(1, 1, AttendanceStatus.PRESENT))
        }
        assertEquals(ErrorCode.ATTENDANCE_ALREADY_CHECKED, ex.errorCode)
    }

    @Test
    fun `registerAttendance EXCUSED fails when limit exceeded`() {
        val cmFull = CohortMember(id = 1, member = member, cohort = cohort, deposit = 100000, excuseCount = 3)
        whenever(sessionRepository.findById(1L)).thenReturn(Optional.of(session))
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        whenever(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false)
        whenever(cohortMemberRepository.findByMemberIdAndCohortGeneration(1L, 11)).thenReturn(cmFull)

        val ex = assertThrows(BusinessException::class.java) {
            service().registerAttendance(CreateAttendanceRequest(1, 1, AttendanceStatus.EXCUSED))
        }
        assertEquals(ErrorCode.EXCUSE_LIMIT_EXCEEDED, ex.errorCode)
    }

    @Test
    fun `updateAttendance adjusts deposit on penalty change`() {
        val attendance = Attendance(id = 1, session = session, member = member,
            status = AttendanceStatus.ABSENT, penaltyAmount = 10000)
        val cmTest = CohortMember(id = 1, member = member, cohort = cohort, deposit = 90000)

        whenever(attendanceRepository.findById(1L)).thenReturn(Optional.of(attendance))
        whenever(cohortMemberRepository.findByMemberIdAndCohortGeneration(1L, 11)).thenReturn(cmTest)
        whenever(attendanceRepository.save(any<Attendance>())).thenAnswer { it.arguments[0] }
        whenever(cohortMemberRepository.save(any<CohortMember>())).thenAnswer { it.arguments[0] }
        whenever(depositHistoryRepository.save(any<DepositHistory>())).thenAnswer { it.arguments[0] }

        val result = service().updateAttendance(1L, UpdateAttendanceRequest(AttendanceStatus.EXCUSED))
        assertEquals(AttendanceStatus.EXCUSED, result.status)
        assertEquals(0, result.penaltyAmount)
        assertEquals(100000, cmTest.deposit) // refund 10000
    }

    @Test
    fun `calculatePenalty returns correct values`() {
        assertEquals(0, AttendanceService.calculatePenalty(AttendanceStatus.PRESENT, null))
        assertEquals(10000, AttendanceService.calculatePenalty(AttendanceStatus.ABSENT, null))
        assertEquals(2500, AttendanceService.calculatePenalty(AttendanceStatus.LATE, 5))
        assertEquals(10000, AttendanceService.calculatePenalty(AttendanceStatus.LATE, 30))
        assertEquals(0, AttendanceService.calculatePenalty(AttendanceStatus.EXCUSED, null))
    }

    // --- QR 출석 체크 (checkIn) ---

    @Test
    fun `checkIn fails with invalid QR hashValue`() {
        whenever(qrCodeRepository.findByHashValue("invalid")).thenReturn(null)

        val ex = assertThrows(BusinessException::class.java) {
            service().checkIn(CheckInRequest("invalid", 1L))
        }
        assertEquals(ErrorCode.QR_INVALID, ex.errorCode)
    }

    @Test
    fun `checkIn fails with expired QR`() {
        val expiredQr = QrCode(id = 1, session = session, hashValue = "hash",
            expiresAt = Instant.now().minus(1, ChronoUnit.HOURS))
        whenever(qrCodeRepository.findByHashValue("hash")).thenReturn(expiredQr)

        val ex = assertThrows(BusinessException::class.java) {
            service().checkIn(CheckInRequest("hash", 1L))
        }
        assertEquals(ErrorCode.QR_EXPIRED, ex.errorCode)
    }

    @Test
    fun `checkIn fails when session not IN_PROGRESS`() {
        val scheduledSession = MeetingSession(id = 1, cohort = cohort, title = "T",
            date = LocalDate.now(), time = LocalTime.now(), location = "L", status = SessionStatus.SCHEDULED)
        val qr = QrCode(id = 1, session = scheduledSession, hashValue = "hash",
            expiresAt = Instant.now().plus(12, ChronoUnit.HOURS))
        whenever(qrCodeRepository.findByHashValue("hash")).thenReturn(qr)

        val ex = assertThrows(BusinessException::class.java) {
            service().checkIn(CheckInRequest("hash", 1L))
        }
        assertEquals(ErrorCode.SESSION_NOT_IN_PROGRESS, ex.errorCode)
    }

    @Test
    fun `checkIn fails for withdrawn member`() {
        val withdrawn = Member(id = 2, loginId = "w", password = "p", name = "w", phone = "p",
            status = MemberStatus.WITHDRAWN)
        val qr = QrCode(id = 1, session = session, hashValue = "hash",
            expiresAt = Instant.now().plus(12, ChronoUnit.HOURS))
        whenever(qrCodeRepository.findByHashValue("hash")).thenReturn(qr)
        whenever(memberRepository.findById(2L)).thenReturn(Optional.of(withdrawn))

        val ex = assertThrows(BusinessException::class.java) {
            service().checkIn(CheckInRequest("hash", 2L))
        }
        assertEquals(ErrorCode.MEMBER_WITHDRAWN, ex.errorCode)
    }

    @Test
    fun `checkIn fails with duplicate attendance`() {
        val qr = QrCode(id = 1, session = session, hashValue = "hash",
            expiresAt = Instant.now().plus(12, ChronoUnit.HOURS))
        whenever(qrCodeRepository.findByHashValue("hash")).thenReturn(qr)
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        whenever(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(true)

        val ex = assertThrows(BusinessException::class.java) {
            service().checkIn(CheckInRequest("hash", 1L))
        }
        assertEquals(ErrorCode.ATTENDANCE_ALREADY_CHECKED, ex.errorCode)
    }

    // --- 조회 API ---

    @Test
    fun `getMyAttendances returns member attendance list`() {
        val attendance = Attendance(id = 1, session = session, member = member,
            status = AttendanceStatus.PRESENT, penaltyAmount = 0)
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        whenever(attendanceRepository.findByMemberId(1L)).thenReturn(listOf(attendance))

        val result = service().getMyAttendances(1L)
        assertEquals(1, result.size)
        assertEquals(AttendanceStatus.PRESENT, result[0].status)
    }

    @Test
    fun `getMyAttendances fails when member not found`() {
        whenever(memberRepository.findById(99L)).thenReturn(Optional.empty())

        val ex = assertThrows(BusinessException::class.java) {
            service().getMyAttendances(99L)
        }
        assertEquals(ErrorCode.MEMBER_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `getAttendanceSummary returns correct counts`() {
        val summaryProjection = object : MemberAttendanceSummaryProjection {
            override fun getPresent() = 1L
            override fun getAbsent() = 1L
            override fun getLate() = 1L
            override fun getExcused() = 0L
            override fun getTotalPenalty() = 12500L
        }
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        whenever(attendanceRepository.summarizeByMemberId(1L)).thenReturn(summaryProjection)
        whenever(cohortMemberRepository.findByMemberIdAndCohortGeneration(1L, 11)).thenReturn(cm)

        val result = service().getAttendanceSummary(1L)
        assertEquals(1, result.present)
        assertEquals(1, result.absent)
        assertEquals(1, result.late)
        assertEquals(0, result.excused)
        assertEquals(12500, result.totalPenalty)
        assertEquals(100000, result.deposit)
    }

    @Test
    fun `getSessionAttendances returns session attendance list`() {
        val attendance = Attendance(id = 1, session = session, member = member,
            status = AttendanceStatus.PRESENT, penaltyAmount = 0)
        whenever(sessionRepository.findById(1L)).thenReturn(Optional.of(session))
        whenever(attendanceRepository.findBySessionId(1L)).thenReturn(listOf(attendance))

        val result = service().getSessionAttendances(1L)
        assertEquals(1L, result.sessionId)
        assertEquals(1, result.attendances.size)
    }

    @Test
    fun `getDepositHistory returns ordered history`() {
        val history = DepositHistory(id = 1, cohortMember = cm, type = DepositType.INITIAL,
            amount = 100000, balanceAfter = 100000, description = "초기 보증금")
        whenever(cohortMemberRepository.findById(1L)).thenReturn(Optional.of(cm))
        whenever(depositHistoryRepository.findByCohortMemberIdOrderByCreatedAtAsc(1L)).thenReturn(listOf(history))

        val result = service().getDepositHistory(1L)
        assertEquals(1, result.size)
        assertEquals(DepositType.INITIAL, result[0].type)
        assertEquals(100000, result[0].amount)
    }

    @Test
    fun `getDepositHistory fails when cohort member not found`() {
        whenever(cohortMemberRepository.findById(99L)).thenReturn(Optional.empty())

        val ex = assertThrows(BusinessException::class.java) {
            service().getDepositHistory(99L)
        }
        assertEquals(ErrorCode.COHORT_MEMBER_NOT_FOUND, ex.errorCode)
    }

    // --- 출결 수정 추가 케이스 ---

    @Test
    fun `updateAttendance from EXCUSED to ABSENT decrements excuseCount and adds penalty`() {
        val cmExcused = CohortMember(id = 1, member = member, cohort = cohort, deposit = 100000, excuseCount = 1)
        val attendance = Attendance(id = 1, session = session, member = member,
            status = AttendanceStatus.EXCUSED, penaltyAmount = 0)

        whenever(attendanceRepository.findById(1L)).thenReturn(Optional.of(attendance))
        whenever(cohortMemberRepository.findByMemberIdAndCohortGeneration(1L, 11)).thenReturn(cmExcused)
        whenever(attendanceRepository.save(any<Attendance>())).thenAnswer { it.arguments[0] }
        whenever(cohortMemberRepository.save(any<CohortMember>())).thenAnswer { it.arguments[0] }
        whenever(depositHistoryRepository.save(any<DepositHistory>())).thenAnswer { it.arguments[0] }

        val result = service().updateAttendance(1L, UpdateAttendanceRequest(AttendanceStatus.ABSENT))
        assertEquals(AttendanceStatus.ABSENT, result.status)
        assertEquals(10000, result.penaltyAmount)
        assertEquals(0, cmExcused.excuseCount) // decremented
        assertEquals(90000, cmExcused.deposit) // penalty deducted
    }

    @Test
    fun `getSessionAttendanceSummary returns summary per member`() {
        whenever(sessionRepository.findById(1L)).thenReturn(Optional.of(session))
        val projection = object : CohortMemberAttendanceSummaryProjection {
            override fun getMemberId() = 1L
            override fun getMemberName() = "n"
            override fun getPresent() = 1L
            override fun getAbsent() = 0L
            override fun getLate() = 1L
            override fun getExcused() = 0L
            override fun getTotalPenalty() = 2500L
            override fun getDeposit() = 100000
        }
        whenever(attendanceRepository.summarizeByCohortId(2L)).thenReturn(listOf(projection))

        val result = service().getSessionAttendanceSummary(1L)
        assertEquals(1, result.size)
        assertEquals(1, result[0].present)
        assertEquals(1, result[0].late)
        assertEquals(2500, result[0].totalPenalty)
        assertEquals(100000, result[0].deposit)
    }

    @Test
    fun `getSessionAttendanceSummary fails when session not found`() {
        whenever(sessionRepository.findById(99L)).thenReturn(Optional.empty())
        val ex = assertThrows(BusinessException::class.java) {
            service().getSessionAttendanceSummary(99L)
        }
        assertEquals(ErrorCode.SESSION_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `getMemberAttendanceDetail returns detail with attendances`() {
        val part = Part(id = 1, cohort = cohort, name = "SERVER")
        val team = Team(id = 1, cohort = cohort, name = "Team A")
        val cmDetail = CohortMember(id = 1, member = member, cohort = cohort, part = part, team = team, deposit = 90000, excuseCount = 1)
        val attendance = Attendance(id = 1, session = session, member = member, status = AttendanceStatus.PRESENT, penaltyAmount = 0)

        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        whenever(cohortMemberRepository.findByMemberIdAndCohortGeneration(1L, 11)).thenReturn(cmDetail)
        whenever(attendanceRepository.findByMemberId(1L)).thenReturn(listOf(attendance))

        val result = service().getMemberAttendanceDetail(1L)
        assertEquals("n", result.memberName)
        assertEquals(11, result.generation)
        assertEquals("SERVER", result.partName)
        assertEquals(90000, result.deposit)
        assertEquals(1, result.excuseCount)
        assertEquals(1, result.attendances.size)
    }

    @Test
    fun `getMemberAttendanceDetail fails when member not found`() {
        whenever(memberRepository.findById(99L)).thenReturn(Optional.empty())
        val ex = assertThrows(BusinessException::class.java) {
            service().getMemberAttendanceDetail(99L)
        }
        assertEquals(ErrorCode.MEMBER_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `registerAttendance EXCUSED increments excuseCount`() {
        val cmFresh = CohortMember(id = 1, member = member, cohort = cohort, deposit = 100000, excuseCount = 0)
        whenever(sessionRepository.findById(1L)).thenReturn(Optional.of(session))
        whenever(memberRepository.findById(1L)).thenReturn(Optional.of(member))
        whenever(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false)
        whenever(cohortMemberRepository.findByMemberIdAndCohortGeneration(1L, 11)).thenReturn(cmFresh)
        whenever(attendanceRepository.save(any<Attendance>())).thenAnswer {
            val a = it.arguments[0] as Attendance
            Attendance(id = 1, session = a.session, member = a.member, status = a.status, penaltyAmount = a.penaltyAmount)
        }
        whenever(cohortMemberRepository.save(any<CohortMember>())).thenAnswer { it.arguments[0] }

        val result = service().registerAttendance(CreateAttendanceRequest(1, 1, AttendanceStatus.EXCUSED))
        assertEquals(AttendanceStatus.EXCUSED, result.status)
        assertEquals(0, result.penaltyAmount)
        assertEquals(1, cmFresh.excuseCount)
    }
}
