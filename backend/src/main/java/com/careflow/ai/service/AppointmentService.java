package com.careflow.ai.service;

import com.careflow.ai.entity.Appointment;
import com.careflow.ai.entity.AppointmentStatus;
import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.AppointmentRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.UserRepository;
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
    private final TimelineService timelineService;
    private final AuditLogService auditLogService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            TimelineService timelineService,
            AuditLogService auditLogService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.timelineService = timelineService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Appointment scheduleAppointment(
            UUID patientId,
            UUID doctorId,
            LocalDateTime scheduledAt,
            UUID createdByUserId) {

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
                createdByUserId
        );

        auditLogService.log(
                createdByUserId,
                "SCHEDULE_APPOINTMENT",
                "Appointment",
                saved.getId(),
                description
        );

        return saved;
    }

    @Transactional
    public Appointment updateStatus(
            UUID appointmentId,
            AppointmentStatus status,
            UUID updatedByUserId) {

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
                updatedByUserId
        );

        auditLogService.log(
                updatedByUserId,
                "UPDATE_APPOINTMENT_STATUS",
                "Appointment",
                saved.getId(),
                description
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public Appointment getAppointment(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Appointment not found: " + appointmentId));
    }

    @Transactional(readOnly = true)
    public List<Appointment> getPatientAppointments(UUID patientId) {
        return appointmentRepository
                .findByPatient_IdOrderByScheduledAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getDoctorAppointments(
            UUID doctorId,
            AppointmentStatus status) {

        return appointmentRepository
                .findByDoctor_IdAndStatusOrderByScheduledAtAsc(
                        doctorId,
                        status
                );
    }
}
