package com.careflow.ai;

import com.careflow.ai.entity.EventType;
import com.careflow.ai.repository.VitalThresholdRepository;
import com.careflow.ai.repository.TimelineEventRepository;
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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 5 — per-patient vital alert thresholds.
 *
 * Verifies:
 * 1. A doctor's override LOWERS the alert floor (OXYGEN 91% is normal by the
 *    global default but critical once the patient override says &lt; 92%).
 * 2. A override can RELAX a threshold (BP 185/95 stops alerting when the
 *    patient's override allows up to 190/110) and SUGAR becomes alertable at
 *    all (no global default exists).
 * 3. The timeline alert detail explains the ACTIVE threshold ("patient-specific").
 * 4. Writes are doctor/admin-only; nurses get 403.
 * 5. Validation: wrong/missing bounds → 400; unknown vital type → 400.
 * 6. DELETE reverts to global defaults.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class Phase5ThresholdIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TimelineEventRepository timelineEventRepository;

    @Autowired
    private VitalThresholdRepository vitalThresholdRepository;

    private String doctorToken;
    private String nurseToken;

    private static final String ELEANOR_ID = "a1111111-1111-1111-1111-111111111111";
    private static final String NURSE_ID = "33333333-3333-3333-3333-333333333333";

    @BeforeEach
    void setUp() throws Exception {
        // Tests share one context database — clear overrides so each test
        // starts from the global clinical defaults.
        vitalThresholdRepository.deleteAll();
        doctorToken = obtainToken("doctor1@careflow.ai", "password123");
        nurseToken = obtainToken("nurse1@careflow.ai", "password123");
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

    /** Override LOWERS the floor: 91% SpO2 alerts only once configured. */
    @Test
    void oxygenOverrideLowersAlertFloorAndTimelineExplainsWhy() throws Exception {
        long criticalBefore = criticalAlertCount();

        // 91% is above the GLOBAL floor (90) → no alert
        recordVital("OXYGEN", "91");
        assertEquals(criticalBefore, criticalAlertCount(),
                "91% must NOT alert under the global default");

        // Doctor tightens this patient's floor to 92
        mockMvc.perform(put("/api/clinical/vitals/" + ELEANOR_ID + "/thresholds/OXYGEN")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minValue\":92}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minValue").value(92));

        // Same value now breaches the patient-specific floor → CRITICAL_ALERT
        recordVital("OXYGEN", "91");
        assertEquals(criticalBefore + 1, criticalAlertCount(),
                "91% must alert once the 92% override is active");
    }

    /** Override RELAXES BP bounds; SUGAR becomes alertable at all. */
    @Test
    void bpOverrideRelaxesAndSugarBecomesAlertable() throws Exception {
        long criticalBefore = criticalAlertCount();

        // 185/115 breaches the global default (180/120) → alert
        recordVital("BLOOD_PRESSURE", "185/115");
        assertEquals(criticalBefore + 1, criticalAlertCount());

        long afterDefaultAlert = criticalAlertCount();

        // Doctor relaxes THIS patient's bounds
        mockMvc.perform(put("/api/clinical/vitals/" + ELEANOR_ID + "/thresholds/BLOOD_PRESSURE")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"systolicMax\":190,\"diastolicMax\":120}"))
                .andExpect(status().isOk());

        // Same value is now within the patient's plan → no new alert
        recordVital("BLOOD_PRESSURE", "185/115");
        assertEquals(afterDefaultAlert, criticalAlertCount(),
                "185/115 must NOT alert under the relaxed override");

        // SUGAR has NO global default — it never alerts without an override
        recordVital("SUGAR", "260");
        assertEquals(afterDefaultAlert, criticalAlertCount());

        mockMvc.perform(put("/api/clinical/vitals/" + ELEANOR_ID + "/thresholds/SUGAR")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minValue\":70,\"maxValue\":180}"))
                .andExpect(status().isOk());

        recordVital("SUGAR", "260");
        assertEquals(afterDefaultAlert + 1, criticalAlertCount(),
                "260 mg/dL must alert once the SUGAR override exists");
    }

    /** The alert detail on the timeline names the ACTIVE threshold. */
    @Test
    void timelineAlertDetailShowsPatientSpecificThreshold() throws Exception {
        mockMvc.perform(put("/api/clinical/vitals/" + ELEANOR_ID + "/thresholds/OXYGEN")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minValue\":92}"))
                .andExpect(status().isOk());

        recordVital("OXYGEN", "90");

        mockMvc.perform(get("/api/patients/" + ELEANOR_ID + "/timeline")
                        .contextPath("/api")
                        .param("eventType", "CRITICAL_ALERT")
                        .param("limit", "5")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].eventType").value("CRITICAL_ALERT"))
                .andExpect(jsonPath("$.items[0].criticalAlert.vitalType").value("OXYGEN"))
                .andExpect(jsonPath("$.items[0].criticalAlert.threshold")
                        .value(containsString("patient-specific")))
                .andExpect(jsonPath("$.items[0].criticalAlert.threshold")
                        .value(containsString("92")));
    }

    /** Nurses cannot change thresholds; validation errors are 400s. */
    @Test
    void roleAndValidationGuards() throws Exception {
        // Nurse is FORBIDDEN to write
        mockMvc.perform(put("/api/clinical/vitals/" + ELEANOR_ID + "/thresholds/OXYGEN")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minValue\":92}"))
                .andExpect(status().isForbidden());

        // Missing the bound the type requires → 400
        mockMvc.perform(put("/api/clinical/vitals/" + ELEANOR_ID + "/thresholds/OXYGEN")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // BP requires BOTH bounds → 400
        mockMvc.perform(put("/api/clinical/vitals/" + ELEANOR_ID + "/thresholds/BLOOD_PRESSURE")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"systolicMax\":190}"))
                .andExpect(status().isBadRequest());

        // Unknown vital type → 400
        mockMvc.perform(put("/api/clinical/vitals/" + ELEANOR_ID + "/thresholds/PULSE")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minValue\":50}"))
                .andExpect(status().isBadRequest());

        // GET is open to any authenticated user (nurse reads fine)
        mockMvc.perform(get("/api/clinical/vitals/" + ELEANOR_ID + "/thresholds")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk());
    }

    /** DELETE removes the override — global defaults apply again. */
    @Test
    void deleteRevertsToGlobalDefaults() throws Exception {
        mockMvc.perform(put("/api/clinical/vitals/" + ELEANOR_ID + "/thresholds/OXYGEN")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minValue\":92}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/clinical/vitals/" + ELEANOR_ID + "/thresholds/OXYGEN")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk());

        // Override gone: 91% is back to non-critical
        long before = criticalAlertCount();
        recordVital("OXYGEN", "91");
        assertEquals(before, criticalAlertCount(),
                "after DELETE the global default (90) must apply again");

        // Deleting a non-existent override → 400
        mockMvc.perform(delete("/api/clinical/vitals/" + ELEANOR_ID + "/thresholds/OXYGEN")
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Records a vital as the assigned nurse via the REST API. */
    private void recordVital(String type, String value) throws Exception {
        mockMvc.perform(post("/api/clinical/vitals/" + ELEANOR_ID)
                        .contextPath("/api")
                        .queryParam("recordedById", NURSE_ID)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"" + type + "\",\"value\":\"" + value + "\"}"))
                .andExpect(status().isOk());
    }

    /** Total CRITICAL_ALERT timeline events for the test patient. */
    private long criticalAlertCount() {
        return timelineEventRepository
                .findByPatient_IdAndEventTypeOrderByCreatedAtAsc(
                        UUID.fromString(ELEANOR_ID), EventType.CRITICAL_ALERT)
                .size();
    }
}
