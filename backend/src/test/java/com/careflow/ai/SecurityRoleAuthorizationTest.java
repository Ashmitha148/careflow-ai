package com.careflow.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class SecurityRoleAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Unauthenticated request to clinical vitals endpoint must be rejected with 401 or 403")
    void unauthenticatedRequestRejected() throws Exception {
        mockMvc.perform(get("/clinical/vitals/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "caregiver@careflow.ai", roles = {"CAREGIVER"})
    @DisplayName("Caregiver role must be blocked from prescribing medications with 403 Forbidden")
    void caregiverCannotPrescribeMedication() throws Exception {
        mockMvc.perform(post("/clinical/medications")
                        .param("patientId", UUID.randomUUID().toString())
                        .param("name", "Amoxicillin")
                        .param("dosage", "500mg")
                        .param("frequency", "BID")
                        .param("startDate", "2026-10-12"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(username = "patient@careflow.ai", roles = {"PATIENT"})
    @DisplayName("Patient role must be blocked from creating tasks with 403 Forbidden")
    void patientCannotCreateTasks() throws Exception {
        mockMvc.perform(post("/tasks")
                        .param("patientId", UUID.randomUUID().toString())
                        .param("assignedNurseId", UUID.randomUUID().toString())
                        .param("title", "Check vitals")
                        .param("description", "Routine check"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(username = "nurse@careflow.ai", roles = {"NURSE"})
    @DisplayName("Nurse role must be blocked from accessing admin audit log panel with 403 Forbidden")
    void nurseCannotAccessAdminAuditLogs() throws Exception {
        mockMvc.perform(get("/audit-logs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(username = "doctor@careflow.ai", roles = {"DOCTOR"})
    @DisplayName("Doctor role must be blocked from accessing hospital tenant onboarding with 403 Forbidden")
    void doctorCannotAccessHospitalManagement() throws Exception {
        mockMvc.perform(post("/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"City Hospital\",\"code\":\"CITY\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
