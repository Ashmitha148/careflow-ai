package com.careflow.ai.service;

import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.Notification;
import com.careflow.ai.entity.NotificationType;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.Role;
import com.careflow.ai.entity.ShiftHandoff;
import com.careflow.ai.entity.User;
import com.careflow.ai.outbox.OutboxEvent;
import com.careflow.ai.outbox.OutboxService;
import com.careflow.ai.repository.NotificationRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.ShiftHandoffRepository;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.websocket.RealtimeEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ShiftHandoffService {

    private final ShiftHandoffRepository handoffRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final TimelineService timelineService;
    private final AuditLogService auditLogService;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final OutboxService outboxService;

    public ShiftHandoffService(
            ShiftHandoffRepository handoffRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            TimelineService timelineService,
            AuditLogService auditLogService,
            RealtimeEventPublisher realtimeEventPublisher,
            OutboxService outboxService) {
        this.handoffRepository = handoffRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.timelineService = timelineService;
        this.auditLogService = auditLogService;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.outboxService = outboxService;
    }

    @Transactional
    public ShiftHandoff createHandoff(
            UUID patientId,
            UUID fromNurseId,
            UUID toNurseId,
            String notes,
            String pendingTasks,
            String observations,
            String completedTasks,
            String nextShiftInstructions,
            LocalDate shiftDate) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() ->
                        new RuntimeException("Patient not found: " + patientId));

        User fromNurse = userRepository.findById(fromNurseId)
                .orElseThrow(() ->
                        new RuntimeException("Outgoing nurse not found: " + fromNurseId));

        User toNurse = userRepository.findById(toNurseId)
                .orElseThrow(() ->
                        new RuntimeException("Incoming nurse not found: " + toNurseId));

        ShiftHandoff handoff = new ShiftHandoff();

        handoff.setPatient(patient);
        handoff.setFromNurse(fromNurse);
        handoff.setToNurse(toNurse);
        handoff.setNotes(notes);
        handoff.setPendingTasks(pendingTasks);
        handoff.setObservations(observations);
        handoff.setCompletedTasks(completedTasks);
        handoff.setNextShiftInstructions(nextShiftInstructions);
        handoff.setShiftDate(
                shiftDate != null ? shiftDate : LocalDate.now()
        );

        ShiftHandoff saved = handoffRepository.save(handoff);

        // Phase 2: the Gemini call moves OFF the request thread. The row
        // commits with aiStatus=PENDING; the outbox worker generates the
        // summary asynchronously and pushes it over
        // /topic/patient/{id}/handoffs. Clients without the socket can poll
        // GET /api/shift-handoffs/{id} (the polling fallback).
        saved.setAiStatus(ShiftHandoff.AI_PENDING);

        String description =
                "Shift handoff created from "
                        + fromNurse.getFullName()
                        + " to "
                        + toNurse.getFullName();

        timelineService.appendEvent(
                patientId,
                EventType.SHIFT_HANDOFF,
                description,
                fromNurseId
        );

        Notification notification = new Notification();
        notification.setUser(toNurse);
        notification.setType(NotificationType.HANDOFF_ASSIGNED);
        notification.setMessage(
                "New shift handoff for patient "
                        + patient.getName()
                        + " from "
                        + fromNurse.getFullName()
        );
        notification.setRead(false);

        notificationRepository.save(notification);

        realtimeEventPublisher.publishNotification(toNurse.getId(), notification);

        // Enqueue the AI job IN this transaction: if the handoff insert rolls
        // back, the job never existed; if it commits, the worker is guaranteed
        // to run (at-least-once, with backoff across outages).
        outboxService.enqueue(
                OutboxEvent.TYPE_HANDOFF_AI_SUMMARY,
                "SHIFT_HANDOFF",
                saved.getId(),
                null,
                null,
                Map.of("handoffId", saved.getId()));

        auditLogService.log(
                fromNurseId,
                "CREATE_SHIFT_HANDOFF",
                "ShiftHandoff",
                saved.getId(),
                description
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<ShiftHandoff> getPatientHandoffs(UUID patientId) {
        return handoffRepository
                .findByPatient_IdOrderByShiftDateDesc(patientId);
    }

    @Transactional(readOnly = true)
    public List<ShiftHandoff> getIncomingHandoffs(
            UUID nurseId,
            LocalDate shiftDate) {

        return handoffRepository
                .findByToNurse_IdAndShiftDate(nurseId, shiftDate);
    }

    @Transactional(readOnly = true)
    public List<ShiftHandoff> getOutgoingHandoffs(UUID nurseId) {
        return handoffRepository
                .findByFromNurse_IdOrderByShiftDateDesc(nurseId);
    }

    /**
     * Handoff-recipient options for the create form. Returns minimal
     * projections — never the User entity, whose password hash must not leak.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getNurseOptions() {
        return userRepository.findByRole(Role.NURSE).stream()
                .map(nurse -> Map.<String, Object>of(
                        "id", nurse.getId(),
                        "fullName", nurse.getFullName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ShiftHandoff getHandoff(UUID handoffId) {
        return handoffRepository.findById(handoffId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Shift handoff not found: " + handoffId));
    }

    @Transactional
    public ShiftHandoff updateAiSummary(
            UUID handoffId,
            String aiSummary) {

        ShiftHandoff handoff = handoffRepository.findById(handoffId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Shift handoff not found: " + handoffId));

        // Manual (non-async) summary set: a completed lifecycle state.
        handoff.setAiSummary(aiSummary);
        handoff.setAiStatus(ShiftHandoff.AI_DONE);
        handoff.setAiError(null);

        return handoffRepository.save(handoff);
    }
}
