package com.prography11thbackend.repository

import com.prography11thbackend.dto.response.AttendanceSummaryProjection
import com.prography11thbackend.dto.response.CohortMemberAttendanceSummaryProjection
import com.prography11thbackend.dto.response.MemberAttendanceSummaryProjection
import com.prography11thbackend.dto.response.SessionAttendanceCountProjection
import com.prography11thbackend.entity.Attendance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AttendanceRepository : JpaRepository<Attendance, Long> {
    fun existsBySessionIdAndMemberId(sessionId: Long, memberId: Long): Boolean
    fun findByMemberId(memberId: Long): List<Attendance>
    fun findBySessionId(sessionId: Long): List<Attendance>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS present,
            COALESCE(SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END), 0) AS absent,
            COALESCE(SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END), 0) AS late,
            COALESCE(SUM(CASE WHEN a.status = 'EXCUSED' THEN 1 ELSE 0 END), 0) AS excused,
            COUNT(a) AS total
        FROM Attendance a
        WHERE a.session.id = :sessionId
    """)
    fun countBySessionId(@Param("sessionId") sessionId: Long): AttendanceSummaryProjection

    @Query("""
        SELECT
            a.session.id AS sessionId,
            COALESCE(SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS present,
            COALESCE(SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END), 0) AS absent,
            COALESCE(SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END), 0) AS late,
            COALESCE(SUM(CASE WHEN a.status = 'EXCUSED' THEN 1 ELSE 0 END), 0) AS excused,
            COUNT(a) AS total
        FROM Attendance a
        WHERE a.session.id IN :sessionIds
        GROUP BY a.session.id
    """)
    fun countBySessionIds(@Param("sessionIds") sessionIds: List<Long>): List<SessionAttendanceCountProjection>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS present,
            COALESCE(SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END), 0) AS absent,
            COALESCE(SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END), 0) AS late,
            COALESCE(SUM(CASE WHEN a.status = 'EXCUSED' THEN 1 ELSE 0 END), 0) AS excused,
            COALESCE(SUM(a.penaltyAmount), 0) AS totalPenalty
        FROM Attendance a
        WHERE a.member.id = :memberId
    """)
    fun summarizeByMemberId(@Param("memberId") memberId: Long): MemberAttendanceSummaryProjection

    @Query("""
        SELECT
            cm.member.id AS memberId,
            cm.member.name AS memberName,
            COALESCE(SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS present,
            COALESCE(SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END), 0) AS absent,
            COALESCE(SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END), 0) AS late,
            COALESCE(SUM(CASE WHEN a.status = 'EXCUSED' THEN 1 ELSE 0 END), 0) AS excused,
            COALESCE(SUM(a.penaltyAmount), 0) AS totalPenalty,
            cm.deposit AS deposit
        FROM CohortMember cm
        LEFT JOIN Attendance a ON a.member = cm.member
        WHERE cm.cohort.id = :cohortId
        GROUP BY cm.member.id, cm.member.name, cm.deposit
    """)
    fun summarizeByCohortId(@Param("cohortId") cohortId: Long): List<CohortMemberAttendanceSummaryProjection>
}
