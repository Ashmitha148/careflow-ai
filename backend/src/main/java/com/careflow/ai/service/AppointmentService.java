package com.careflow.ai.service;

import com.careflow.ai.entity.Appointment;
import com.careflow.ai.entity.AppointmentStatus;
import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.AppointmentRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.security.PatientAuthorizationService;
import com.careflow.ai.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PatientAuthorizationService patientAuthorizationService;
    private final TimelineService timelineService;
    private final AuditLogService auditLogService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            PatientAuthorizationService patientAuthorizationService,
            TimelineService timelineService,
            AuditLogService auditLogService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.patientAuthorizationService = patientAuthorizationService;
        this.timelineService = timelineService;
        this.auditLogService = auditLogService;
    }

    @CacheEvict(value = CacheConfig.DASHBOARD_SUMMARY_CACHE, allEntries = true)
    @Transactional
    public Appointment scheduleAppointment(
            UUID patientId,
            UUID doctorId,
            LocalDateTime scheduledAt) {

        patientAuthorizationService.ensurePatientAccess(patientId);

        User createdBy = resolveCurrentUser();

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() ->
                        new RuntimeException("Patient not found: " + patientId));

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() ->
                        new RuntimeException("Doctor not found: " + doctorId));

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setScheduledAt(scheduledAt);
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        Appointment saved = appointmentRepository.save(appointment);

        String description =
                "Appointment scheduled with "
                        + doctor.getFullName()
                        + " for "
                        + scheduledAt;

        timelineService.appendEvent(
                patientId,
                EventType.APPOINTMENT,
                description,
                createdBy.getId()
        );

        auditLogService.log(
                createdBy.getId(),
                "SCHEDULE_APPOINTMENT",
                "Appointment",
                saved.getId(),
                description
        );

        return saved;
    }

    @CacheEvict(value = CacheConfig.DASHBOARD_SUMMARY_CACHE, allEntries = true)
    @Transactional
    public Appointment updateStatus(
            UUID appointmentId,
            AppointmentStatus status) {

        ensurePatientAccessForAppointment(appointmentId);

        User updatedBy = resolveCurrentUser();

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Appointment not found: " + appointmentId));

        appointment.setStatus(status);

        Appointment saved = appointmentRepository.save(appointment);

        String description =
                "Appointment status changed to " + status;

        timelineService.appendEvent(
                saved.getPatient().getId(),
                EventType.APPOINTMENT,
                description,
                updatedBy.getId()
        );

        auditLogService.log(
                updatedBy.getId(),
                "UPDATE_APPOINTMENT_STATUS",
                "Appointment",
                saved.getId(),
                description
        );

        return saved;
    }

    private User resolveCurrentUser() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof com.careflow.ai.security.CustomUserDetails details)) {
            throw new RuntimeException("Authentication required");
        }
        return details.getUser();
    }

    @Transactional(readOnly = true)
    public Appointment getAppointment(UUID appointmentId) {
        ensurePatientAccessForAppointment(appointmentId);

        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Appointment not found: " + appointmentId));
    }

    @Transactional(readOnly = true)
    public List<Appointment> getPatientAppointments(UUID patientId) {
        patientAuthorizationService.ensurePatientAccess(patientId);

        return appointmentRepository
                .findByPatient_IdOrderByScheduledAtDesc(patientId);
    }

    private void ensurePatientAccessForAppointment(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Appointment not found: " + appointmentId));
        patientAuthorizationService.ensurePatientAccess(appointment.getPatient().getId());
    }

    @Transactional(readOnly = true)
    public List<Appointment> getDoctorAppointments(
            AppointmentStatus status) {

        User actor = resolveCurrentUser();
        return appointmentRepository
                .findByDoctor_IdAndStatusOrderByScheduledAtAsc(
                        actor.getId(),
                        status
                );
    }
}
