package com.careflow.ai.service;

import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.Notification;
import com.careflow.ai.entity.NotificationType;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.ShiftHandoff;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.NotificationRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.ShiftHandoffRepository;
import com.careflow.ai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ShiftHandoffService {

    private final ShiftHandoffRepository handoffRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final TimelineService timelineService;
    private final AuditLogService auditLogService;

    public ShiftHandoffService(
            ShiftHandoffRepository handoffRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            TimelineService timelineService,
            AuditLogService auditLogService) {
        this.handoffRepository = handoffRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.timelineService = timelineService;
        this.auditLogService = auditLogService;
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

        handoff.setAiSummary(aiSummary);

        return handoffRepository.save(handoff);
    }
}
