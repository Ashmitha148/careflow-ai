package com.careflow.ai;

import com.careflow.ai.dto.AuthDto.AuthResponse;
import com.careflow.ai.dto.AuthDto.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 7 — multi-hospital tenancy.
 *
 * Shared-schema, row-scoped isolation seeded by V10:
 * - CareFlow General (CGH) owns ALL pre-existing users/patients.
 * - Riverside Clinic (RVS) has its own doctor + patient (Robert Riverside).
 *
 * Verifies:
 * 1. A Riverside doctor's patient list contains ONLY their own tenant's patient.
 * 2. Cross-tenant reads are rejected (Riverside doctor → CareFlow General patient).
 * 3. Cross-tenant caregiver assignment is rejected at patient creation.
 * 4. New patients inherit the creating user's hospital.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class Phase7TenancyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String generalDoctorToken;
    private String riversideDoctorToken;
    private String adminToken;

    private static final String ELEANOR_ID = "a1111111-1111-1111-1111-111111111111";
    private static final String RIVERSIDE_DOCTOR_ID = "d0000000-0000-0000-0000-000000000001";
    /** Seeded CareFlow General doctor (doctor1@careflow.ai) — actor for writes. */
    private static final String GENERAL_DOCTOR_ID = "11111111-1111-1111-1111-111111111111";
    private static final String GENERAL_HOSPITAL_ID = "c0000000-0000-0000-0000-000000000001";
    private static final String GENERAL_HOSPITAL_NAME = "CareFlow General";

    @BeforeEach
    void setUp() throws Exception {
        generalDoctorToken = obtainToken("doctor1@careflow.ai", "password123");
        riversideDoctorToken = obtainToken("riverside.doctor@careflow.ai", "password123");
        adminToken = obtainToken("admin@careflow.ai", "password123");
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

    /** The Riverside doctor sees ONLY Riverside patients, never CareFlow General's. */
    @Test
    void patientListIsScopedToOwnHospital() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/patients/my")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + riversideDoctorToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(res.getResponse().getContentAsString());
        JsonNode patients = content.has("content") ? content.get("content") : content;

        assertEquals(1, patients.size(),
                "Riverside doctor must see exactly their own tenant's patient");
        assertEquals("Robert Riverside", patients.get(0).get("name").asText());
        assertEquals("c0000000-0000-0000-0000-000000000002",
                patients.get(0).get("hospitalId").asText());
    }

    /** Cross-tenant read: Riverside doctor cannot reach a CareFlow General patient. */
    @Test
    void crossTenantReadIsRejected() throws Exception {
        mockMvc.perform(get("/api/patients/" + ELEANOR_ID)
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + riversideDoctorToken))
                .andExpect(status().is4xxClientError());
    }

    /** Cross-tenant assignment: CGH doctor cannot assign the Riverside doctor. */
    @Test
    void crossTenantCaregiverAssignmentIsRejected() throws Exception {
        String body = objectMapper.createObjectNode()
                .put("name", "Tenancy Probe")
                .put("dob", "1970-01-01")
                .put("gender", "Female")
                .put("contactInfo", "555-9999")
                .put("assignedDoctorId", RIVERSIDE_DOCTOR_ID)
                .toString();

        MvcResult res = mockMvc.perform(post("/api/patients")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + generalDoctorToken)
                        .param("actorUserId", GENERAL_DOCTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();

        assertEquals(400, res.getResponse().getStatus(),
                "assigning a caregiver from another hospital must be rejected");
        assertTrue(res.getResponse().getContentAsString()
                        .contains("different hospital"),
                "error must name the tenancy violation");
    }

    /** A new patient inherits the creating doctor's hospital tenant. */
    @Test
    void newPatientInheritsCreatorsHospital() throws Exception {
        String body = objectMapper.createObjectNode()
                .put("name", "Same-Tenant Probe")
                .put("dob", "1980-06-15")
                .put("gender", "Male")
                .put("contactInfo", "555-8888")
                .toString();

        MvcResult res = mockMvc.perform(post("/api/patients")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + generalDoctorToken)
                        .param("actorUserId", GENERAL_DOCTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalId").value(GENERAL_HOSPITAL_ID))
                .andReturn();

        UUID createdId = UUID.fromString(objectMapper.readTree(
                res.getResponse().getContentAsString()).get("id").asText());

        // Round-trip as admin (a fresh patient has no assignments yet, so the
        // creating doctor cannot read it back until assigned — pre-existing
        // access rule): the STORED tenant must be the creator's hospital.
        mockMvc.perform(get("/api/patients/" + createdId)
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalId").value(GENERAL_HOSPITAL_ID));
    }
}
