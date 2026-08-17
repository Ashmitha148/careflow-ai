package com.careflow.ai;

import com.careflow.ai.dto.AuthDto.LoginRequest;
import com.careflow.ai.dto.AuthDto.AuthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class TimelineAndPatientIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String doctorToken;
    private String nurseToken;
    private String patientToken;
    private String adminToken;

    private static final String ELEANOR_ID = "a1111111-1111-1111-1111-111111111111";
    private static final String NURSE_ID = "33333333-3333-3333-3333-333333333333";

    @BeforeEach
    void setUp() throws Exception {
        doctorToken = obtainToken("doctor1@careflow.ai", "password123");
        nurseToken = obtainToken("nurse1@careflow.ai", "password123");
        patientToken = obtainToken("patient@careflow.ai", "password123");
        adminToken = obtainToken("admin@careflow.ai", "password123");
    }

    private String obtainToken(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest(email, password);
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse resp = objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class);
        return resp.getToken();
    }

    @Test
    void testTimelineWithoutFilters() throws Exception {
        mockMvc.perform(get("/api/patients/" + ELEANOR_ID + "/timeline")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$.length()", greaterThan(0)));
    }

    @Test
    void testTimelineWithEventTypeFilter() throws Exception {
        mockMvc.perform(get("/api/patients/" + ELEANOR_ID + "/timeline")
                        .contextPath("/api")
                        .param("eventType", "VITAL")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testTimelineWithDateFilters() throws Exception {
        mockMvc.perform(get("/api/patients/" + ELEANOR_ID + "/timeline")
                        .contextPath("/api")
                        .param("start", "2020-01-01T00:00:00")
                        .param("end", "2030-01-01T00:00:00")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testTimelineReplay24h() throws Exception {
        mockMvc.perform(get("/api/patients/" + ELEANOR_ID + "/timeline/replay/24h")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testTimelineReplay7d() throws Exception {
        mockMvc.perform(get("/api/patients/" + ELEANOR_ID + "/timeline/replay/7d")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testGetMyPatientsForPatient() throws Exception {
        mockMvc.perform(get("/api/patients/my")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Eleanor Vance"));
    }

    @Test
    void testGetMyPatientsForDoctor() throws Exception {
        mockMvc.perform(get("/api/patients/my")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$.length()", greaterThan(0)));
    }

    @Test
    void testGetTasksForPatient() throws Exception {
        mockMvc.perform(get("/api/tasks/patient/" + ELEANOR_ID)
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testGetPendingTasksForNurse() throws Exception {
        mockMvc.perform(get("/api/tasks/nurse/" + NURSE_ID + "/pending")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }
}
