package com.prography11thbackend.integration

import com.prography11thbackend.repository.CohortRepository
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
class MemberIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var cohortRepository: CohortRepository

    @Test
    @Order(1)
    fun `getMember returns admin member info`() {
        mockMvc.perform(get("/api/v1/members/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.loginId").value("admin"))
            .andExpect(jsonPath("$.data.name").value("관리자"))
    }

    @Test
    @Order(2)
    fun `getMember with nonexistent id returns 404`() {
        mockMvc.perform(get("/api/v1/members/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("MEMBER_NOT_FOUND"))
    }

    @Test
    @Order(3)
    fun `createMember and getMemberDetail`() {
        val cohortId = cohortRepository.findByGeneration(11)!!.id

        mockMvc.perform(
            post("/api/v1/admin/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"loginId":"testuser","password":"test1234","name":"테스트","phone":"010-1111-1111","cohortId":$cohortId}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.loginId").value("testuser"))
            .andExpect(jsonPath("$.data.name").value("테스트"))
            .andExpect(jsonPath("$.data.generation").value(11))
    }

    @Test
    @Order(4)
    fun `createMember with duplicate loginId returns 409`() {
        mockMvc.perform(
            post("/api/v1/admin/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"loginId":"admin","password":"pass","name":"dup","phone":"010-0000-0000","cohortId":1}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error.code").value("DUPLICATE_LOGIN_ID"))
    }

    @Test
    @Order(5)
    fun `getMembersDashboard returns paged results`() {
        mockMvc.perform(get("/api/v1/admin/members").param("page", "0").param("size", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.page").value(0))
    }

    @Test
    @Order(6)
    fun `getMemberDetail returns admin detail with cohort info`() {
        mockMvc.perform(get("/api/v1/admin/members/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.loginId").value("admin"))
            .andExpect(jsonPath("$.data.generation").value(11))
    }

    @Test
    @Order(7)
    fun `updateMember updates name`() {
        mockMvc.perform(
            put("/api/v1/admin/members/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"관리자수정"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.name").value("관리자수정"))
    }

    @Test
    @Order(8)
    fun `deleteMember changes status to WITHDRAWN`() {
        val cohortId = cohortRepository.findByGeneration(11)!!.id
        val createResult = mockMvc.perform(
            post("/api/v1/admin/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"loginId":"deluser","password":"pass","name":"삭제대상","phone":"010-2222-2222","cohortId":$cohortId}""")
        ).andReturn()

        val body = createResult.response.contentAsString
        val idMatch = Regex(""""id":(\d+)""").find(body)
        val memberId = idMatch?.groupValues?.get(1) ?: "1"

        mockMvc.perform(delete("/api/v1/admin/members/$memberId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("WITHDRAWN"))
    }
}
