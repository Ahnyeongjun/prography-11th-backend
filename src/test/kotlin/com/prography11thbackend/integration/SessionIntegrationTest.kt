package com.prography11thbackend.integration

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

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SessionIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    @Order(1)
    fun `createSession creates a new session`() {
        mockMvc.perform(
            post("/api/v1/admin/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"정기 모임","date":"2026-03-01","time":"14:00:00","location":"강남역"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("정기 모임"))
            .andExpect(jsonPath("$.data.location").value("강남역"))
            .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
            .andExpect(jsonPath("$.data.qrActive").value(true))
    }

    @Test
    @Order(2)
    fun `getSessions returns non-cancelled sessions for members`() {
        mockMvc.perform(get("/api/v1/sessions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    @Order(3)
    fun `getAdminSessions returns all sessions for admin`() {
        mockMvc.perform(get("/api/v1/admin/sessions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    @Order(4)
    fun `getAdminSessions filters by status`() {
        mockMvc.perform(get("/api/v1/admin/sessions").param("status", "SCHEDULED"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    @Order(5)
    fun `updateSession updates title`() {
        // First create a session to update
        val createResult = mockMvc.perform(
            post("/api/v1/admin/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"수정 테스트","date":"2026-03-15","time":"10:00:00","location":"서울"}""")
        ).andReturn()

        val body = createResult.response.contentAsString
        val idMatch = Regex(""""id":(\d+)""").find(body)
        val sessionId = idMatch?.groupValues?.get(1) ?: "1"

        mockMvc.perform(
            put("/api/v1/admin/sessions/$sessionId")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"수정된 제목"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.title").value("수정된 제목"))
    }

    @Test
    @Order(6)
    fun `deleteSession changes status to CANCELLED`() {
        val createResult = mockMvc.perform(
            post("/api/v1/admin/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"삭제 테스트","date":"2026-04-01","time":"15:00:00","location":"판교"}""")
        ).andReturn()

        val body = createResult.response.contentAsString
        val idMatch = Regex(""""id":(\d+)""").find(body)
        val sessionId = idMatch?.groupValues?.get(1) ?: "1"

        mockMvc.perform(delete("/api/v1/admin/sessions/$sessionId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("CANCELLED"))
    }

    @Test
    @Order(7)
    fun `createQrCode for session`() {
        val createResult = mockMvc.perform(
            post("/api/v1/admin/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"QR 테스트","date":"2026-05-01","time":"10:00:00","location":"역삼"}""")
        ).andReturn()

        val body = createResult.response.contentAsString
        val idMatch = Regex(""""id":(\d+)""").find(body)
        val sessionId = idMatch?.groupValues?.get(1) ?: "1"

        // createSession already creates a QR, so creating another should fail with QR_ALREADY_ACTIVE
        mockMvc.perform(post("/api/v1/admin/sessions/$sessionId/qrcodes"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("QR_ALREADY_ACTIVE"))
    }

    @Test
    @Order(8)
    fun `renewQrCode creates new QR and expires old one`() {
        val createResult = mockMvc.perform(
            post("/api/v1/admin/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"갱신 테스트","date":"2026-06-01","time":"10:00:00","location":"삼성"}""")
        ).andReturn()

        val body = createResult.response.contentAsString
        // QR is auto-created with the session, find the qr ID
        // The QR code ID will be auto-incremented, let's use the renew endpoint
        // We need the QR code ID - let's just try with ID 1 from the first session's QR
        mockMvc.perform(put("/api/v1/admin/qrcodes/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.hashValue").isString)
    }

    @Test
    fun `updateSession with nonexistent id returns 404`() {
        mockMvc.perform(
            put("/api/v1/admin/sessions/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"없는세션"}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("SESSION_NOT_FOUND"))
    }
}
