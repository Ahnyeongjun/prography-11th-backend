package com.prography11thbackend.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class CohortIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `getCohorts returns all cohorts from seed data`() {
        mockMvc.perform(get("/api/v1/admin/cohorts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
    }

    @Test
    fun `getCohortDetail returns cohort with parts and teams`() {
        mockMvc.perform(get("/api/v1/admin/cohorts/2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.generation").value(11))
            .andExpect(jsonPath("$.data.parts").isArray)
            .andExpect(jsonPath("$.data.parts.length()").value(5))
            .andExpect(jsonPath("$.data.teams").isArray)
            .andExpect(jsonPath("$.data.teams.length()").value(3))
    }

    @Test
    fun `getCohortDetail with nonexistent id returns 404`() {
        mockMvc.perform(get("/api/v1/admin/cohorts/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("COHORT_NOT_FOUND"))
    }
}
