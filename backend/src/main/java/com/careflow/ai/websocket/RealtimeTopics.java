package com.careflow.ai.websocket;

import java.util.UUID;

/**
 * Topic naming contract for the CareFlow STOMP broker.
 *
 * Topics are scoped by role and clinical concern rather than being a single
 * firehose, so a client only ever receives the messages it is authorized and
 * interested in:
 *
 * <pre>
 * /topic/patient/{patientId}/timeline   — append-only timeline events (new rows only)
 * /topic/patient/{patientId}/vitals     — live vitals stream
 * /topic/patient/{patientId}/tasks      — task state changes in the patient workspace
 * /topic/patient/{patientId}/handoffs   — async AI shift-handoff summary completions
 * /topic/patient/{patientId}/presence   — who is currently viewing this patient
 * /topic/nurse/{nurseId}/queue          — shift queue / task assignment changes
 * /user/queue/notifications             — private per-user notifications
 * </pre>
 *
 * This contract is documented in docs/adr/0001-websocket-topic-design.md and must
 * stay in sync with the frontend realtimeChannels module.
 */
public final class RealtimeTopics {

    public static final String APP_PREFIX = "/app";
    public static final String TOPIC_PREFIX = "/topic";
    public static final String USER_NOTIFICATIONS = "/user/queue/notifications";

    private RealtimeTopics() {
    }

    public static String patientTimeline(UUID patientId) {
        return TOPIC_PREFIX + "/patient/" + patientId + "/timeline";
    }

    public static String patientVitals(UUID patientId) {
        return TOPIC_PREFIX + "/patient/" + patientId + "/vitals";
    }

    public static String patientPresence(UUID patientId) {
        return TOPIC_PREFIX + "/patient/" + patientId + "/presence";
    }

    public static String nurseQueue(UUID nurseId) {
        return TOPIC_PREFIX + "/nurse/" + nurseId + "/queue";
    }

    /** Task state changes for a patient's workspace (in addition to the nurse queue). */
    public static String patientTasks(UUID patientId) {
        return TOPIC_PREFIX + "/patient/" + patientId + "/tasks";
    }

    /** Completed async AI shift-handoff summaries for a patient (Phase 2 outbox worker). */
    public static String patientHandoffs(UUID patientId) {
        return TOPIC_PREFIX + "/patient/" + patientId + "/handoffs";
    }

    /**
     * Session-scoped unicast queue used to deliver private per-user messages.
     * Client-facing subscriptions to {@link #USER_NOTIFICATIONS} are rewritten to
     * this destination in {@link WebSocketTopicAuthorizer}, which makes per-user
     * routing deterministic without relying on broker user-destination resolution.
     */
    public static String userQueue(String sessionId) {
        return "/queue/u-" + sessionId;
    }

    /** STOMP app-destination used by clients to send presence control messages. */
    public static String presenceControl(UUID patientId) {
        return APP_PREFIX + "/presence/patient/" + patientId;
    }
}