package com.careflow.ai;

import com.careflow.ai.dto.AuthDto.AuthResponse;
import com.careflow.ai.dto.AuthDto.LoginRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 8 — Hospital (tenant) administration.
 *
 * Verifies:
 * 1. ADMIN can list facilities with patient/staff counts; caregivers get 403.
 * 2. ADMIN can onboard a facility; duplicate code/name and blank fields → 400.
 * 3. The tenant-admin lever: reassigning staff cuts their access to the old
 *    hospital's patients and lands them in the new tenant.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class Phase8HospitalManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String doctorToken;

    /** Seeded in V10 — the Riverside tenant's doctor. */
    private static final String RIVERSIDE_DOCTOR_ID = "d0000000-0000-0000-0000-000000000001";
    private static final String RIVERSIDE_PATIENT_ID = "b0000000-0000-0000-0000-000000000001";

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainToken("admin@careflow.ai", "password123");
        doctorToken = obtainToken("doctor1@careflow.ai", "password123");
    }

    private String obtainToken(String email, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(),
                AuthResponse.class).getToken();
    }

    /** ADMIN sees both seeded tenants with counts; DOCTOR is forbidden. */
    @Test
    void adminListsHospitalsWithCountsAndDoctorIsForbidden() throws Exception {
        mockMvc.perform(get("/api/hospitals")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.code == 'CGH')]").isNotEmpty())
                .andExpect(jsonPath("$.[?(@.code == 'RVS')]").isNotEmpty())
                .andExpect(jsonPath("$.[?(@.code == 'RVS')].patientCount").isNotEmpty())
                .andExpect(jsonPath("$.[?(@.code == 'RVS')].staffCount").isNotEmpty());

        mockMvc.perform(get("/api/hospitals")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden());
    }

    /** Onboarding works; duplicate code and blank fields are rejected. */
    @Test
    void adminCreatesHospitalAndDuplicatesAreRejected() throws Exception {
        mockMvc.perform(post("/api/hospitals")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Duplicat General\",\"code\":\"dgh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Duplicat General"))
                .andExpect(jsonPath("$.code").value("DGH"))
                .andExpect(jsonPath("$.patientCount").value(0))
                .andExpect(jsonPath("$.staffCount").value(0));

        // Duplicate code → 400
        mockMvc.perform(post("/api/hospitals")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Another Facility\",\"code\":\"CGH\"}"))
                .andExpect(status().isBadRequest());

        // Blank code → 400 (bean validation)
        mockMvc.perform(post("/api/hospitals")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Third Facility\",\"code\":\"\"}"))
                .andExpect(status().isBadRequest());

        // Caregivers cannot onboard facilities
        mockMvc.perform(post("/api/hospitals")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rogue Facility\",\"code\":\"RGE\"}"))
                .andExpect(status().isForbidden());
    }

    /** Reassignment cuts old-tenant access and lands the user in the new one. */
    @Test
    void staffReassignmentMovesTenantAndCutsOldHospitalAccess() throws Exception {
        // Onboard the new facility
        MvcResult created = mockMvc.perform(post("/api/hospitals")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test General\",\"code\":\"TGH\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String hospitalId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        // Before: the riverside doctor can read his own patient
        mockMvc.perform(get("/api/patients/" + RIVERSIDE_PATIENT_ID)
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + obtainToken(
                                "riverside.doctor@careflow.ai", "password123")))
                .andExpect(status().isOk());

        // Reassign him to the new facility
        mockMvc.perform(put("/api/hospitals/" + hospitalId + "/staff/" + RIVERSIDE_DOCTOR_ID)
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalId").value(hospitalId))
                .andExpect(jsonPath("$.hospitalName").value("Test General"))
                .andExpect(jsonPath("$.role").value("DOCTOR"));

        // The new tenant's roster contains him
        mockMvc.perform(get("/api/hospitals/" + hospitalId + "/staff")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.email == 'riverside.doctor@careflow.ai')]").isNotEmpty());

        // ...and he has lost access to his old hospital's patient
        mockMvc.perform(get("/api/patients/" + RIVERSIDE_PATIENT_ID)
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + obtainToken(
                                "riverside.doctor@careflow.ai", "password123")))
                .andExpect(status().is4xxClientError());

        // Unknown hospital / unknown user → 400
        mockMvc.perform(put("/api/hospitals/" + hospitalId + "/staff/"
                                + "00000000-0000-0000-0000-000000000009")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/hospitals/00000000-0000-0000-0000-000000000099/staff/"
                                + RIVERSIDE_DOCTOR_ID)
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        // Caregivers cannot move staff
        mockMvc.perform(put("/api/hospitals/" + hospitalId + "/staff/" + RIVERSIDE_DOCTOR_ID)
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden());
    }
}
