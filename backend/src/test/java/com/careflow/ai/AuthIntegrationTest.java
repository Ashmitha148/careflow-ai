package com.careflow.ai;

import com.careflow.ai.dto.AuthDto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testLoginSuccessForSeededDoctor() throws Exception {
        LoginRequest loginRequest = new LoginRequest("sarah.jenkins@careflow.ai", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email").value("sarah.jenkins@careflow.ai"))
                .andExpect(jsonPath("$.user.fullName").value("Dr. Sarah Jenkins"))
                .andExpect(jsonPath("$.user.role").value("DOCTOR"));
    }

    @Test
    public void testLoginSuccessForSeededNurse() throws Exception {
        LoginRequest loginRequest = new LoginRequest("emily.watson@careflow.ai", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email").value("emily.watson@careflow.ai"))
                .andExpect(jsonPath("$.user.fullName").value("Nurse Emily Watson"))
                .andExpect(jsonPath("$.user.role").value("NURSE"));
    }

    @Test
    public void testLoginFailureInvalidPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest("sarah.jenkins@careflow.ai", "wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}
