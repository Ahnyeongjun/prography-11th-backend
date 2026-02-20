package com.prography11thbackend.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `login with valid admin credentials returns success`() {
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"loginId": "admin", "password": "admin1234"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.loginId").value("admin"))
            .andExpect(jsonPath("$.data.name").value("관리자"))
            .andExpect(jsonPath("$.data.role").value("ADMIN"))
    }

    @Test
    fun `login with wrong password returns 401`() {
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"loginId": "admin", "password": "wrongpass"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"))
    }

    @Test
    fun `login with nonexistent user returns 401`() {
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"loginId": "nobody", "password": "pass"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"))
    }
}
