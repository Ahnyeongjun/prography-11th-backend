package com.prography11thbackend.integration

import com.prography11thbackend.entity.SessionStatus
import com.prography11thbackend.repository.CohortMemberRepository
import com.prography11thbackend.repository.MeetingSessionRepository
import com.prography11thbackend.repository.QrCodeRepository
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AttendanceIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var sessionRepository: MeetingSessionRepository
    @Autowired lateinit var qrCodeRepository: QrCodeRepository
    @Autowired lateinit var cohortMemberRepository: CohortMemberRepository

    private fun createSessionAndGetId(): Long {
        val result = mockMvc.perform(
            post("/api/v1/admin/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"출결 테스트","date":"2026-03-10","time":"14:00:00","location":"테스트"}""")
        ).andReturn()
        val body = result.response.contentAsString
        return Regex(""""id":(\d+)""").find(body)!!.groupValues[1].toLong()
    }

    private fun setSessionInProgress(sessionId: Long) {
        val session = sessionRepository.findById(sessionId).get()
        session.status = SessionStatus.IN_PROGRESS
        sessionRepository.save(session)
    }

    @Test
    @Order(1)
    fun `registerAttendance creates attendance record`() {
        val sessionId = createSessionAndGetId()
        setSessionInProgress(sessionId)

        mockMvc.perform(
            post("/api/v1/admin/attendances")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"sessionId":$sessionId,"memberId":1,"status":"PRESENT"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("PRESENT"))
            .andExpect(jsonPath("$.data.penaltyAmount").value(0))
    }

    @Test
    @Order(2)
    fun `getMyAttendances returns member attendance list`() {
        mockMvc.perform(get("/api/v1/attendances").param("memberId", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    @Order(3)
    fun `getAttendanceSummary returns summary for member`() {
        mockMvc.perform(get("/api/v1/members/1/attendance-summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.deposit").isNumber)
    }

    @Test
    @Order(4)
    fun `registerAttendance with duplicate returns 409`() {
        // Use same session and member from Order 1 test
        val sessions = sessionRepository.findAll().toList()
        val inProgressSession = sessions.firstOrNull { it.status == SessionStatus.IN_PROGRESS }

        if (inProgressSession != null) {
            mockMvc.perform(
                post("/api/v1/admin/attendances")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"sessionId":${inProgressSession.id},"memberId":1,"status":"PRESENT"}""")
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error.code").value("ATTENDANCE_ALREADY_CHECKED"))
        }
    }

    @Test
    @Order(5)
    fun `updateAttendance changes status`() {
        mockMvc.perform(
            put("/api/v1/admin/attendances/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"LATE","lateMinutes":5}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("LATE"))
    }

    @Test
    @Order(6)
    fun `getSessionAttendanceSummary returns summary per member`() {
        val sessions = sessionRepository.findAll().toList()
        val session = sessions.firstOrNull { it.status == SessionStatus.IN_PROGRESS }

        if (session != null) {
            mockMvc.perform(get("/api/v1/admin/attendances/sessions/${session.id}/summary"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
        }
    }

    @Test
    @Order(7)
    fun `getMemberAttendanceDetail returns detail`() {
        mockMvc.perform(get("/api/v1/admin/attendances/members/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.memberName").isString)
    }

    @Test
    @Order(8)
    fun `getSessionAttendances returns attendance list for session`() {
        val sessions = sessionRepository.findAll().toList()
        val session = sessions.firstOrNull { it.status == SessionStatus.IN_PROGRESS }

        if (session != null) {
            mockMvc.perform(get("/api/v1/admin/attendances/sessions/${session.id}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value(session.id))
                .andExpect(jsonPath("$.data.attendances").isArray)
        }
    }

    @Test
    @Order(9)
    fun `getDepositHistory returns history for cohort member`() {
        val cm = cohortMemberRepository.findByMemberId(1L).firstOrNull()
        if (cm != null) {
            mockMvc.perform(get("/api/v1/admin/cohort-members/${cm.id}/deposits"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].type").value("INITIAL"))
        }
    }

    @Test
    fun `checkIn with invalid QR returns 400`() {
        mockMvc.perform(
            post("/api/v1/attendances")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"hashValue":"invalid-hash","memberId":1}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("QR_INVALID"))
    }

    @Test
    fun `getAttendanceSummary with nonexistent member returns 404`() {
        mockMvc.perform(get("/api/v1/members/999/attendance-summary"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("MEMBER_NOT_FOUND"))
    }
}
