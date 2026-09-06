package com.careflow.ai.outbox;

import com.careflow.ai.config.GeminiClient;
import com.careflow.ai.entity.Notification;
import com.careflow.ai.entity.NotificationType;
import com.careflow.ai.entity.ShiftHandoff;
import com.careflow.ai.repository.NotificationRepository;
import com.careflow.ai.repository.ShiftHandoffRepository;
import com.careflow.ai.websocket.RealtimeTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The async AI worker for shift-handoff summaries (Phase 2): consumes
 * HANDOFF_AI_SUMMARY outbox rows, calls Gemini OFF the request thread, persists
 * the summary, and fans the result out over WebSocket + a private notification.
 *
 * Design points:
 * <ul>
 *   <li>Idempotent under at-least-once relay delivery: a replayed row is a
 *       no-op once the handoff is DONE.</li>
 *   <li>The Gemini call runs OUTSIDE any transaction, so a slow LLM never
 *       holds a database connection.</li>
 *   <li>Failure is graceful, never a 500 to a client: transient Gemini errors
 *       are retried by the relay with backoff; a deterministic "not configured"
 *       error or an exhausted attempt budget marks the handoff FAILED with
 *       {@code ai_error} set — the raw clinical data stays fully usable.</li>
 *   <li>Completion fans out through the SAME transactional outbox (patient
 *       handoffs topic + private notification), so result delivery has the
 *       same no-loss guarantee as any other clinical event.</li>
 * </ul>
 */
@Component
public class HandoffAiSummaryHandler implements OutboxHandler {

    private static final Logger log = LoggerFactory.getLogger(HandoffAiSummaryHandler.class);

    private final ShiftHandoffRepository handoffRepository;
    private final NotificationRepository notificationRepository;
    private final GeminiClient geminiClient;
    private final OutboxService outboxService;
    private final TransactionTemplate transactionTemplate;
    private final int maxAttempts;

    public HandoffAiSummaryHandler(
            ShiftHandoffRepository handoffRepository,
            NotificationRepository notificationRepository,
            GeminiClient geminiClient,
            OutboxService outboxService,
            PlatformTransactionManager transactionManager,
            @Value("${careflow.outbox.max-attempts:5}") int maxAttempts) {
        this.handoffRepository = handoffRepository;
        this.notificationRepository = notificationRepository;
        this.geminiClient = geminiClient;
        this.outboxService = outboxService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.maxAttempts = maxAttempts;
    }

    @Override
    public String eventType() {
        return OutboxEvent.TYPE_HANDOFF_AI_SUMMARY;
    }

    @Override
    public void handle(OutboxEvent event) throws Exception {
        UUID handoffId = event.getAggregateId();
        if (handoffId == null) {
            throw new IllegalStateException(
                    "HANDOFF_AI_SUMMARY row " + event.getId() + " has no aggregateId");
        }

        boolean claimed = claimForProcessing(handoffId);
        if (!claimed) {
            return; // already done, or the handoff no longer exists
        }

        try {
            String summary = generateSummary(handoffId);
            persistSummaryAndFanOut(handoffId, summary);
            log.info("AI summary generated for shift handoff {}", handoffId);
        } catch (Exception e) {
            boolean notConfigured = e instanceof IllegalStateException
                    && e.getMessage() != null
                    && e.getMessage().contains("not configured");
            if (notConfigured || event.getAttempts() >= maxAttempts) {
                // Deterministic failure, or the relay's budget is exhausted:
                // surface it on the handoff instead of retrying forever.
                markFailed(handoffId, e);
            } else {
                log.warn("AI summary failed for handoff {} (attempt {}/{}) — will retry",
                        handoffId, event.getAttempts(), maxAttempts, e);
                throw e instanceof RuntimeException re ? re : new IllegalStateException(e);
            }
        }
    }

    /** Idempotency gate: NONE/PENDING → PROCESSING; DONE short-circuits. */
    private boolean claimForProcessing(UUID handoffId) {
        Boolean claimed = transactionTemplate.execute(tx -> {
            ShiftHandoff handoff = handoffRepository.findById(handoffId).orElse(null);
            if (handoff == null) {
                log.warn("HANDOFF_AI_SUMMARY row for missing handoff {} — skipping", handoffId);
                return false;
            }
            if (ShiftHandoff.AI_DONE.equals(handoff.getAiStatus())) {
                return false;
            }
            if (!ShiftHandoff.AI_PROCESSING.equals(handoff.getAiStatus())) {
                handoff.setAiStatus(ShiftHandoff.AI_PROCESSING);
                handoffRepository.save(handoff);
            }
            return true;
        });
        return Boolean.TRUE.equals(claimed);
    }

    /** Loads the handoff in a short transaction, then calls Gemini with no tx open. */
    private String generateSummary(UUID handoffId) {
        String prompt = transactionTemplate.execute(tx -> {
            ShiftHandoff handoff = handoffRepository.findById(handoffId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Shift handoff not found: " + handoffId));
            return buildPrompt(
                    handoff.getPatient().getName(),
                    handoff.getShiftDate(),
                    handoff.getFromNurse() == null ? "" : handoff.getFromNurse().getFullName(),
                    handoff.getToNurse() == null ? "" : handoff.getToNurse().getFullName(),
                    handoff.getNotes(),
                    handoff.getPendingTasks(),
                    handoff.getObservations(),
                    handoff.getCompletedTasks(),
                    handoff.getNextShiftInstructions());
        });
        return geminiClient.generate(prompt);
    }

    private void persistSummaryAndFanOut(UUID handoffId, String summary) {
        transactionTemplate.executeWithoutResult(tx -> {
            ShiftHandoff handoff = handoffRepository.findById(handoffId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Shift handoff not found: " + handoffId));
            handoff.setAiSummary(summary);
            handoff.setAiStatus(ShiftHandoff.AI_DONE);
            handoff.setAiError(null);
            ShiftHandoff saved = handoffRepository.save(handoff);

            // Worker-thread payloads must be plain maps: the entity's lazy
            // associations are unreachable once this transaction closes.
            Map<String, Object> handoffPayload = new LinkedHashMap<>();
            handoffPayload.put("id", saved.getId());
            handoffPayload.put("patientId", saved.getPatient().getId());
            handoffPayload.put("aiStatus", saved.getAiStatus());
            handoffPayload.put("aiSummary", saved.getAiSummary());
            handoffPayload.put("shiftDate", saved.getShiftDate().toString());
            handoffPayload.put("notes", saved.getNotes());
            outboxService.enqueue(
                    OutboxEvent.TYPE_REALTIME_BROADCAST,
                    "SHIFT_HANDOFF",
                    saved.getId(),
                    RealtimeTopics.patientHandoffs(saved.getPatient().getId()),
                    null,
                    handoffPayload);

            if (saved.getFromNurse() != null) {
                Notification notification = new Notification();
                notification.setUser(saved.getFromNurse());
                notification.setType(NotificationType.AI_SUMMARY_READY);
                notification.setMessage("AI summary ready for the " + saved.getShiftDate()
                        + " handoff of patient " + saved.getPatient().getName());
                notification.setRead(false);
                notificationRepository.save(notification);

                Map<String, Object> notificationPayload = new LinkedHashMap<>();
                notificationPayload.put("id", notification.getId());
                notificationPayload.put("type", notification.getType().name());
                notificationPayload.put("message", notification.getMessage());
                notificationPayload.put("read", false);
                outboxService.enqueue(
                        OutboxEvent.TYPE_REALTIME_BROADCAST,
                        "NOTIFICATION",
                        null,
                        null,
                        saved.getFromNurse().getId(),
                        notificationPayload);
            }
        });
    }

    private void markFailed(UUID handoffId, Exception cause) {
        transactionTemplate.executeWithoutResult(tx -> {
            handoffRepository.findById(handoffId).ifPresent(handoff -> {
                handoff.setAiStatus(ShiftHandoff.AI_FAILED);
                handoff.setAiError(truncate(cause.getMessage(), 512));
                handoffRepository.save(handoff);
            });
        });
        log.error("AI summary failed permanently for handoff {} — marked FAILED; "
                + "the raw handoff data remains fully usable", handoffId, cause);
    }

    private String buildPrompt(
            String patientName,
            LocalDate shiftDate,
            String fromNurse,
            String toNurse,
            String notes,
            String pendingTasks,
            String observations,
            String completedTasks,
            String nextShiftInstructions) {

        return "Summarize the following nursing shift handoff into a concise, "
                + "clinically-focused brief (max 150 words) for the incoming nurse. "
                + "Prioritize: patient status changes, pending tasks, and safety risks. "
                + "Use plain, professional language.\n\n"
                + "Patient: " + patientName + "\n"
                + "Shift date: " + shiftDate + "\n"
                + "Outgoing nurse: " + fromNurse + "\n"
                + "Incoming nurse: " + toNurse + "\n"
                + "Handoff notes: " + safe(notes) + "\n"
                + "Pending tasks: " + safe(pendingTasks) + "\n"
                + "Observations: " + safe(observations) + "\n"
                + "Completed tasks: " + safe(completedTasks) + "\n"
                + "Next-shift instructions: " + safe(nextShiftInstructions) + "\n";
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "(none provided)" : value;
    }

    private static String truncate(String value, int limit) {
        if (value == null) {
            return "AI summary generation failed";
        }
        return value.length() <= limit ? value : value.substring(0, limit - 3) + "...";
    }
}