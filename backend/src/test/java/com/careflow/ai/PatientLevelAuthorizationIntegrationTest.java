package com.careflow.ai;

import com.careflow.ai.dto.AuthDto.AuthResponse;
import com.careflow.ai.dto.AuthDto.LoginRequest;
import com.careflow.ai.entity.AuditLog;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.Role;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.AuditLogRepository;
import com.careflow.ai.repository.PatientAccessRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Patient-level authorization matrix integration test.
 * Proves that PatientAuthorizationService is enforced across all patient-facing endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class PatientLevelAuthorizationIntegrationTest {

    private static final String ELEANOR_ID = "a1111111-1111-1111-1111-111111111111";
    private static final String RIVERSIDE_PATIENT_ID = "b1111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientAccessRepository patientAccessRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private String assignedDoctorToken;
    private String unassignedDoctorToken;
    private String riversideDoctorToken;
    private String patientToken;
    private User doctor1;
    private User unassignedDoctor;

    @BeforeEach
    void setUp() throws Exception {
        doctor1 = userRepository.findByEmail("doctor1@careflow.ai").orElseThrow();
        unassignedDoctor = upsertUnassignedDoctor();

        assignedDoctorToken = obtainToken("doctor1@careflow.ai", "password123");
        unassignedDoctorToken = obtainToken("unassigned-doctor@careflow.test", "password123");
        riversideDoctorToken = obtainToken("riverside.doctor@careflow.ai", "password123");
        patientToken = obtainToken("patient@careflow.ai", "password123");

        // Clean up access grants for unassigned doctor
        patientAccessRepository.findAll().stream()
                .filter(a -> a.getUser().getId().equals(unassignedDoctor.getId()))
                .forEach(patientAccessRepository::delete);
    }

    private User upsertUnassignedDoctor() {
        return userRepository.findByEmail("unassigned-doctor@careflow.test").orElseGet(() -> {
            User u = new User();
            u.setEmail("unassigned-doctor@careflow.test");
            u.setPasswordHash(passwordEncoder.encode("password123"));
            u.setFullName("Unassigned Doctor");
            u.setRole(Role.DOCTOR);
            u.setHospital(patientRepository.findById(UUID.fromString(ELEANOR_ID)).orElseThrow().getHospital());
            return userRepository.save(u);
        });
    }

    private String obtainToken(String email, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class).getToken();
    }

    @Test
    void unassignedDoctorGets403OnPatientEndpoints() throws Exception {
        mockMvc.perform(get("/api/patients/" + ELEANOR_ID)
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + unassignedDoctorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/patients/" + ELEANOR_ID + "/timeline")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + unassignedDoctorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/clinical/vitals/" + ELEANOR_ID)
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + unassignedDoctorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void crossTenantDoctorGets403() throws Exception {
        mockMvc.perform(get("/api/patients/" + ELEANOR_ID)
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + riversideDoctorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignedDoctorSucceeds() throws Exception {
        mockMvc.perform(get("/api/patients/" + ELEANOR_ID)
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + assignedDoctorToken))
                .andExpect(status().isOk());
    }

    @Test
    void patientRoleUserCannotReadOtherPatientData() throws Exception {
        mockMvc.perform(get("/api/patients/" + RIVERSIDE_PATIENT_ID)
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditLogRecordsRealAuthenticatedUser() throws Exception {
        String body = objectMapper.createObjectNode()
                .put("type", "OXYGEN")
                .put("value", "98.0")
                .toString();

        mockMvc.perform(post("/api/clinical/vitals/" + ELEANOR_ID)
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + assignedDoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, 5)).getContent();
        assertFalse(logs.isEmpty());
        assertEquals(doctor1.getId(), logs.get(0).getUser().getId());
    }
}
