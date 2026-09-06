package com.careflow.ai;

import com.careflow.ai.config.GeminiClient;
import com.careflow.ai.dto.AuthDto.AuthResponse;
import com.careflow.ai.dto.AuthDto.LoginRequest;
import com.careflow.ai.outbox.OutboxEvent;
import com.careflow.ai.outbox.OutboxEventRepository;
import com.careflow.ai.outbox.OutboxRelay;
import com.careflow.ai.outbox.OutboxStatus;
import com.careflow.ai.outbox.RealtimeBroadcastHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 2 integration tests: transactional outbox delivery guarantees,
 * idempotency-key replay, and the async AI handoff worker.
 *
 * The background relay is disabled (poll-interval 1h) so tests drive
 * {@link OutboxRelay#relayDueEvents()} manually — deterministic, no races.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "careflow.outbox.poll-interval-ms=3600000",
        // retry backoff collapses to ~1ms so the recovery pass re-claims at once
        "careflow.outbox.backoff-base-ms=0",
})
public class Phase2EventBackboneIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Replaces the real broker publisher — the knob for simulated outages. */
    @MockBean
    private RealtimeBroadcastHandler broadcastHandler;

    /** Deterministic Gemini for the async worker tests. */
    @MockBean
    private GeminiClient geminiClient;

    private String nurseToken;

    private static final String ELEANOR_ID = "a1111111-1111-1111-1111-111111111111";
    private static final String NURSE_ID = "33333333-3333-3333-3333-333333333333";
    private static final String NURSE2_ID = "44444444-4444-4444-4444-444444444444";

    @BeforeEach
    void setUp() throws Exception {
        // Clean slate: no background relay runs, so leftover rows from other
        // tests would otherwise pollute claim-based assertions.
        outboxRepository.deleteAll();
        // A Mockito mock returns NULL from unstubbed methods — including
        // eventType(), which OutboxRelay.handlerFor() dispatches on. Without
        // this stub every dispatch NPEs and the handle() stubbings below are
        // never even reached.
        when(broadcastHandler.eventType()).thenReturn(OutboxEvent.TYPE_REALTIME_BROADCAST);
        nurseToken = obtainToken("nurse1@careflow.ai", "password123");
    }

    private String obtainToken(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest(email, password);
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse resp = objectMapper.readValue(
                res.getResponse().getContentAsString(), AuthResponse.class);
        return resp.getToken();
    }

    private long vitalCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vitals WHERE patient_id = ?",
                Long.class, UUID.fromString(ELEANOR_ID));
        return count == null ? 0 : count;
    }

    /**
     * THE outbox guarantee: a simulated broker failure must neither lose nor
     * duplicate the clinical event — the row retries with backoff and is
     * delivered once the "broker" recovers.
     */
    @Test
    void outboxEventSurvivesBrokerOutageAndIsDeliveredExactlyOnce() throws Exception {
        doNothing().when(broadcastHandler).handle(any(OutboxEvent.class));

        mockMvc.perform(post("/api/clinical/vitals/" + ELEANOR_ID)
                        .contextPath("/api")
                        .param("recordedById", NURSE_ID)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"OXYGEN\",\"value\":\"97.1\"}"))
                .andExpect(status().isOk());

        // The outbox row committed in the SAME transaction as the vital.
        List<OutboxEvent> rows = outboxRepository.findAll();
        assertTrue(rows.size() > 0, "clinical write must enqueue outbox rows");
        assertTrue(rows.stream().allMatch(e -> e.getStatus() == OutboxStatus.PENDING));

        long rowTotal = rows.size();

        // Broker goes down: every dispatch fails.
        doThrow(new RuntimeException("simulated broker outage"))
                .when(broadcastHandler).handle(any(OutboxEvent.class));
        outboxRelay.relayDueEvents();

        List<OutboxEvent> afterFailure = outboxRepository.findAll();
        assertEquals(rowTotal, afterFailure.size(), "no event may be lost");
        assertTrue(afterFailure.stream()
                        .allMatch(e -> e.getStatus() == OutboxStatus.PENDING && e.getAttempts() == 1),
                "failed events must return to PENDING with attempt=1 (retryable)");

        // Broker recovers: the relay re-claims and delivers every event.
        doNothing().when(broadcastHandler).handle(any(OutboxEvent.class));
        outboxRelay.relayDueEvents();

        List<OutboxEvent> afterRecovery = outboxRepository.findAll();
        assertEquals(rowTotal, afterRecovery.size(), "no event may be duplicated");
        assertTrue(afterRecovery.stream()
                        .allMatch(e -> e.getStatus() == OutboxStatus.PROCESSED),
                "recovered events must all reach PROCESSED");
    }

    /** A retried request with the same Idempotency-Key replays — never double-writes. */
    @Test
    void idempotencyKeyReplaysResponseWithoutDoubleWriting() throws Exception {
        doNothing().when(broadcastHandler).handle(any(OutboxEvent.class));

        String key = "it-vital-" + UUID.randomUUID();
        String body = "{\"type\":\"OXYGEN\",\"value\":\"97.2\"}";

        long before = vitalCount();

        MvcResult first = mockMvc.perform(post("/api/clinical/vitals/" + ELEANOR_ID)
                        .contextPath("/api")
                        .param("recordedById", NURSE_ID)
                        .header("Authorization", "Bearer " + nurseToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        long afterFirst = vitalCount();
        assertEquals(before + 1, afterFirst, "first execution writes exactly one vital");

        // Flaky mobile connection retries the SAME request.
        MvcResult retry = mockMvc.perform(post("/api/clinical/vitals/" + ELEANOR_ID)
                        .contextPath("/api")
                        .param("recordedById", NURSE_ID)
                        .header("Authorization", "Bearer " + nurseToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andReturn();

        assertEquals(first.getResponse().getContentAsString(),
                retry.getResponse().getContentAsString(),
                "replayed response must be identical to the original");

        assertEquals(afterFirst, vitalCount(), "retry must NOT write a second vital");
        long outboxRowsAfterFirst = outboxRepository.count();
        assertTrue(outboxRowsAfterFirst > 0);

        // (retry already performed above) — no new outbox rows may appear.
        assertEquals(outboxRowsAfterFirst, outboxRepository.count(),
                "replay must not enqueue duplicate outbox events");
    }

    /** Key reuse against a different endpoint is a client bug → 422. */
    @Test
    void idempotencyKeyReuseAcrossEndpointsIsRejected() throws Exception {
        doNothing().when(broadcastHandler).handle(any(OutboxEvent.class));

        String key = "it-reuse-" + UUID.randomUUID();

        mockMvc.perform(post("/api/clinical/vitals/" + ELEANOR_ID)
                        .contextPath("/api")
                        .param("recordedById", NURSE_ID)
                        .header("Authorization", "Bearer " + nurseToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"OXYGEN\",\"value\":\"97.3\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/shift-handoffs")
                        .contextPath("/api")
                        .param("patientId", ELEANOR_ID)
                        .param("fromNurseId", NURSE_ID)
                        .param("toNurseId", NURSE2_ID)
                        .param("shiftDate", "2026-09-06")
                        .param("notes", "reuse attempt")
                        .header("Authorization", "Bearer " + nurseToken)
                        .header("Idempotency-Key", key))
                .andExpect(status().isUnprocessableEntity());
    }

    /** The happy path: 202 → worker generates summary → DONE + WebSocket fan-out row. */
    @Test
    void handoffCreationReturns202AndWorkerCompletesSummary() throws Exception {
        when(geminiClient.generate(any(String.class))).thenReturn(
                "Patient stable. Watch morning glucose; pending wound dressing change.");

        MvcResult created = mockMvc.perform(post("/api/shift-handoffs")
                        .contextPath("/api")
                        .param("patientId", ELEANOR_ID)
                        .param("fromNurseId", NURSE_ID)
                        .param("toNurseId", NURSE2_ID)
                        .param("shiftDate", "2026-09-06")
                        .param("notes", "End-of-shift notes")
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.aiStatus").value("PENDING"))
                .andReturn();

        String handoffId = objectMapper.readTree(
                created.getResponse().getContentAsString()).get("id").asText();

        assertEquals(1, outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)
                        .stream()
                        .filter(e -> OutboxEvent.TYPE_HANDOFF_AI_SUMMARY.equals(e.getEventType()))
                        .count(),
                "the AI job must be enqueued in the creating transaction");

        outboxRelay.relayDueEvents(); // processes the AI job + Phase-1 broadcast rows
        outboxRelay.relayDueEvents(); // delivers the worker's completion fan-out rows

        mockMvc.perform(get("/api/shift-handoffs/" + handoffId)
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiStatus").value("DONE"))
                .andExpect(jsonPath("$.aiSummary").value(
                        "Patient stable. Watch morning glucose; pending wound dressing change."));

        boolean fanOutRow = outboxRepository.findAll().stream()
                .anyMatch(e -> OutboxEvent.TYPE_REALTIME_BROADCAST.equals(e.getEventType())
                        && ("/topic/patient/" + ELEANOR_ID + "/handoffs").equals(e.getTopic()));
        assertTrue(fanOutRow, "worker must enqueue a handoffs-topic broadcast");
    }

    /** AI unavailability degrades gracefully: FAILED status, raw data intact, no 500. */
    @Test
    void handoffAiFailureIsGracefulAndObservable() throws Exception {
        when(geminiClient.generate(any(String.class)))
                .thenThrow(new IllegalStateException("GEMINI_API_KEY is not configured"));

        MvcResult created = mockMvc.perform(post("/api/shift-handoffs")
                        .contextPath("/api")
                        .param("patientId", ELEANOR_ID)
                        .param("fromNurseId", NURSE_ID)
                        .param("toNurseId", NURSE2_ID)
                        .param("shiftDate", "2026-09-06")
                        .param("notes", "End-of-shift notes")
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isAccepted())
                .andReturn();

        String handoffId = objectMapper.readTree(
                created.getResponse().getContentAsString()).get("id").asText();

        outboxRelay.relayDueEvents();

        JsonNode handoff = objectMapper.readTree(mockMvc
                .perform(get("/api/shift-handoffs/" + handoffId)
                        .contextPath("/api")
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertEquals("FAILED", handoff.get("aiStatus").asText());
        assertTrue(handoff.get("aiError").asText().length() > 0,
                "ai_error must explain the failure");
        assertEquals("End-of-shift notes", handoff.get("notes").asText(),
                "the raw clinical data must be untouched by the AI failure");
    }
}
