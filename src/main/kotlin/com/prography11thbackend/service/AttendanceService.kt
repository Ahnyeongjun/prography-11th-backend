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
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class AttendanceService(
    private val attendanceRepository: AttendanceRepository,
    private val memberRepository: MemberRepository,
    private val sessionRepository: MeetingSessionRepository,
    private val qrCodeRepository: QrCodeRepository,
    private val cohortMemberRepository: CohortMemberRepository,
    private val depositHistoryRepository: DepositHistoryRepository,
    @Value("\${current-cohort.generation}") private val currentGeneration: Int
) {

    @Transactional
    fun checkIn(request: CheckInRequest): AttendanceResponse {
        val qr = qrCodeRepository.findByHashValue(request.hashValue)
            ?: throw BusinessException(ErrorCode.QR_INVALID)
        if (qr.expiresAt.isBefore(Instant.now())) throw BusinessException(ErrorCode.QR_EXPIRED)

        val session = qr.session
        if (session.status != SessionStatus.IN_PROGRESS) throw BusinessException(ErrorCode.SESSION_NOT_IN_PROGRESS)

        val member = memberRepository.findById(request.memberId)
            .orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
        if (member.status == MemberStatus.WITHDRAWN) throw BusinessException(ErrorCode.MEMBER_WITHDRAWN)
        if (attendanceRepository.existsBySessionIdAndMemberId(session.id, member.id))
            throw BusinessException(ErrorCode.ATTENDANCE_ALREADY_CHECKED)

        val cm = cohortMemberRepository.findByMemberIdAndCohortGeneration(member.id, currentGeneration)
            ?: throw BusinessException(ErrorCode.COHORT_MEMBER_NOT_FOUND)

        val sessionDateTime = LocalDateTime.of(session.date, session.time)
            .atZone(ZoneId.of("Asia/Seoul")).toInstant()
        val now = Instant.now()
        val isLate = now.isAfter(sessionDateTime)
        val lateMinutes = if (isLate) Duration.between(sessionDateTime, now).toMinutes().toInt() else null
        val status = if (isLate) AttendanceStatus.LATE else AttendanceStatus.PRESENT
        val penalty = calculatePenalty(status, lateMinutes)

        if (penalty > 0 && cm.deposit < penalty) throw BusinessException(ErrorCode.DEPOSIT_INSUFFICIENT)

        val attendance = attendanceRepository.save(Attendance(
            session = session, member = member, qrCode = qr,
            status = status, lateMinutes = lateMinutes, penaltyAmount = penalty,
            checkedInAt = now
        ))

        if (penalty > 0) {
            cm.deposit -= penalty
            cohortMemberRepository.save(cm)
            depositHistoryRepository.save(DepositHistory(
                cohortMember = cm, type = DepositType.PENALTY,
                amount = -penalty, balanceAfter = cm.deposit,
                attendance = attendance, description = "출결 등록 - ${status.name} 패널티 ${penalty}원"
            ))
        }

        return toAttendanceResponse(attendance)
    }

    fun getMyAttendances(memberId: Long): List<MyAttendanceResponse> {
        memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
        return attendanceRepository.findByMemberId(memberId).map {
            MyAttendanceResponse(
                it.id, it.session.id, it.session.title, it.status,
                it.lateMinutes, it.penaltyAmount, it.reason, it.checkedInAt, it.createdAt
            )
        }
    }

    fun getAttendanceSummary(memberId: Long): AttendanceSummaryResponse {
        memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
        val summary = attendanceRepository.summarizeByMemberId(memberId)
        val cm = cohortMemberRepository.findByMemberIdAndCohortGeneration(memberId, currentGeneration)
        return AttendanceSummaryResponse(
            memberId = memberId,
            present = summary.getPresent().toInt(),
            absent = summary.getAbsent().toInt(),
            late = summary.getLate().toInt(),
            excused = summary.getExcused().toInt(),
            totalPenalty = summary.getTotalPenalty().toInt(),
            deposit = cm?.deposit
        )
    }

    @Transactional
    fun registerAttendance(request: CreateAttendanceRequest): AttendanceResponse {
        val session = sessionRepository.findById(request.sessionId)
            .orElseThrow { BusinessException(ErrorCode.SESSION_NOT_FOUND) }
        val member = memberRepository.findById(request.memberId)
            .orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
        if (attendanceRepository.existsBySessionIdAndMemberId(session.id, member.id))
            throw BusinessException(ErrorCode.ATTENDANCE_ALREADY_CHECKED)

        val cm = cohortMemberRepository.findByMemberIdAndCohortGeneration(member.id, currentGeneration)
            ?: throw BusinessException(ErrorCode.COHORT_MEMBER_NOT_FOUND)

        if (request.status == AttendanceStatus.EXCUSED) {
            if (cm.excuseCount >= 3) throw BusinessException(ErrorCode.EXCUSE_LIMIT_EXCEEDED)
            cm.excuseCount++
        }

        val penalty = calculatePenalty(request.status, request.lateMinutes)
        if (penalty > 0 && cm.deposit < penalty) throw BusinessException(ErrorCode.DEPOSIT_INSUFFICIENT)

        val attendance = attendanceRepository.save(Attendance(
            session = session, member = member,
            status = request.status, lateMinutes = request.lateMinutes,
            penaltyAmount = penalty, reason = request.reason
        ))

        if (penalty > 0) {
            cm.deposit -= penalty
            depositHistoryRepository.save(DepositHistory(
                cohortMember = cm, type = DepositType.PENALTY,
                amount = -penalty, balanceAfter = cm.deposit,
                attendance = attendance, description = "출결 등록 - ${request.status.name} 패널티 ${penalty}원"
            ))
        }
        cohortMemberRepository.save(cm)

        return toAttendanceResponse(attendance)
    }

    @Transactional
    fun updateAttendance(id: Long, request: UpdateAttendanceRequest): AttendanceResponse {
        val attendance = attendanceRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.ATTENDANCE_NOT_FOUND) }

        val cm = cohortMemberRepository.findByMemberIdAndCohortGeneration(
            attendance.member.id, currentGeneration
        ) ?: throw BusinessException(ErrorCode.COHORT_MEMBER_NOT_FOUND)

        val oldStatus = attendance.status
        val oldPenalty = attendance.penaltyAmount
        val newPenalty = calculatePenalty(request.status, request.lateMinutes)
        val diff = newPenalty - oldPenalty

        // EXCUSED transitions
        if (oldStatus != AttendanceStatus.EXCUSED && request.status == AttendanceStatus.EXCUSED) {
            if (cm.excuseCount >= 3) throw BusinessException(ErrorCode.EXCUSE_LIMIT_EXCEEDED)
            cm.excuseCount++
        } else if (oldStatus == AttendanceStatus.EXCUSED && request.status != AttendanceStatus.EXCUSED) {
            cm.excuseCount = maxOf(0, cm.excuseCount - 1)
        }

        if (diff > 0 && cm.deposit < diff) throw BusinessException(ErrorCode.DEPOSIT_INSUFFICIENT)

        attendance.status = request.status
        attendance.lateMinutes = request.lateMinutes
        attendance.penaltyAmount = newPenalty
        request.reason?.let { attendance.reason = it }
        attendance.updatedAt = Instant.now()
        attendanceRepository.save(attendance)

        if (diff > 0) {
            cm.deposit -= diff
            depositHistoryRepository.save(DepositHistory(
                cohortMember = cm, type = DepositType.PENALTY,
                amount = -diff, balanceAfter = cm.deposit,
                attendance = attendance, description = "출결 수정 - 추가 차감 ${diff}원"
            ))
        } else if (diff < 0) {
            cm.deposit -= diff // diff is negative, so this adds
            depositHistoryRepository.save(DepositHistory(
                cohortMember = cm, type = DepositType.REFUND,
                amount = -diff, balanceAfter = cm.deposit,
                attendance = attendance, description = "출결 수정 - 환급 ${-diff}원"
            ))
        }
        cohortMemberRepository.save(cm)

        return toAttendanceResponse(attendance)
    }

    fun getSessionAttendanceSummary(sessionId: Long): List<MemberAttendanceSummaryItem> {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { BusinessException(ErrorCode.SESSION_NOT_FOUND) }
        return attendanceRepository.summarizeByCohortId(session.cohort.id).map { p ->
            MemberAttendanceSummaryItem(
                memberId = p.getMemberId(),
                memberName = p.getMemberName(),
                present = p.getPresent().toInt(),
                absent = p.getAbsent().toInt(),
                late = p.getLate().toInt(),
                excused = p.getExcused().toInt(),
                totalPenalty = p.getTotalPenalty().toInt(),
                deposit = p.getDeposit()
            )
        }
    }

    fun getMemberAttendanceDetail(memberId: Long): MemberAttendanceDetailResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
        val cm = cohortMemberRepository.findByMemberIdAndCohortGeneration(memberId, currentGeneration)
        val attendances = attendanceRepository.findByMemberId(memberId)
        return MemberAttendanceDetailResponse(
            memberId = member.id, memberName = member.name,
            generation = cm?.cohort?.generation, partName = cm?.part?.name, teamName = cm?.team?.name,
            deposit = cm?.deposit, excuseCount = cm?.excuseCount,
            attendances = attendances.map { toAttendanceResponse(it) }
        )
    }

    fun getSessionAttendances(sessionId: Long): SessionAttendanceResponse {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { BusinessException(ErrorCode.SESSION_NOT_FOUND) }
        val attendances = attendanceRepository.findBySessionId(sessionId)
        return SessionAttendanceResponse(
            sessionId = session.id, sessionTitle = session.title,
            attendances = attendances.map { toAttendanceResponse(it) }
        )
    }

    fun getDepositHistory(cohortMemberId: Long): List<DepositHistoryResponse> {
        cohortMemberRepository.findById(cohortMemberId)
            .orElseThrow { BusinessException(ErrorCode.COHORT_MEMBER_NOT_FOUND) }
        return depositHistoryRepository.findByCohortMemberIdOrderByCreatedAtAsc(cohortMemberId).map {
            DepositHistoryResponse(
                it.id, it.cohortMember.id, it.type, it.amount, it.balanceAfter,
                it.attendance?.id, it.description, it.createdAt
            )
        }
    }

    companion object {
        fun calculatePenalty(status: AttendanceStatus, lateMinutes: Int?): Int = when (status) {
            AttendanceStatus.PRESENT -> 0
            AttendanceStatus.ABSENT -> 10000
            AttendanceStatus.LATE -> minOf((lateMinutes ?: 0) * 500, 10000)
            AttendanceStatus.EXCUSED -> 0
        }
    }

    private fun toAttendanceResponse(a: Attendance) = AttendanceResponse(
        a.id, a.session.id, a.member.id, a.status,
        a.lateMinutes, a.penaltyAmount, a.reason, a.checkedInAt,
        a.createdAt, a.updatedAt
    )
}
