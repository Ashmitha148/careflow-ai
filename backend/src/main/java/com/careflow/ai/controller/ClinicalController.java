package com.careflow.ai.controller;

import com.careflow.ai.entity.Appointment;
import com.careflow.ai.entity.AppointmentStatus;
import com.careflow.ai.entity.FileAttachment;
import com.careflow.ai.entity.Medication;
import com.careflow.ai.entity.MedicationAdministration;
import com.careflow.ai.entity.Vital;
import com.careflow.ai.service.AppointmentService;
import com.careflow.ai.service.FileAttachmentService;
import com.careflow.ai.service.MedicationAdministrationService;
import com.careflow.ai.service.MedicationService;
import com.careflow.ai.service.VitalService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clinical")
public class ClinicalController {

    private final VitalService vitalService;
    private final MedicationService medicationService;
    private final MedicationAdministrationService medicationAdministrationService;
    private final AppointmentService appointmentService;
    private final FileAttachmentService fileAttachmentService;

    public ClinicalController(
            VitalService vitalService,
            MedicationService medicationService,
            MedicationAdministrationService medicationAdministrationService,
            AppointmentService appointmentService,
            FileAttachmentService fileAttachmentService) {
        this.vitalService = vitalService;
        this.medicationService = medicationService;
        this.medicationAdministrationService = medicationAdministrationService;
        this.appointmentService = appointmentService;
        this.fileAttachmentService = fileAttachmentService;
    }

    @PostMapping("/vitals/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE')")
    public Vital recordVital(
            @PathVariable UUID patientId,
            @RequestParam UUID recordedById,
            @RequestBody Vital vital) {
        return vitalService.recordVital(patientId, recordedById, vital);
    }

    @GetMapping("/vitals/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public List<Vital> getVitals(@PathVariable UUID patientId) {
        return vitalService.getPatientVitals(patientId);
    }

    @PostMapping("/medications")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public Medication prescribeMedication(
            @RequestParam UUID doctorUserId,
            @RequestParam UUID patientId,
            @RequestParam String name,
            @RequestParam String dosage,
            @RequestParam String frequency,
            @RequestParam java.time.LocalDate startDate,
            @RequestParam(required = false) java.time.LocalDate endDate) {
        return medicationService.prescribeMedication(
                patientId,
                doctorUserId,
                name,
                dosage,
                frequency,
                startDate,
                endDate);
    }

    @GetMapping("/medications/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public List<Medication> getMedications(
            @PathVariable UUID patientId) {
        return medicationService.getPatientMedications(patientId);
    }

    @GetMapping("/medications/item/{medicationId}")
    @PreAuthorize("isAuthenticated()")
    public Medication getMedication(
            @PathVariable UUID medicationId) {
        return medicationService.getMedication(medicationId);
    }

    @PatchMapping("/medications/{medicationId}/discontinue")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public Medication discontinueMedication(
            @PathVariable UUID medicationId,
            @RequestParam UUID userId) {
        return medicationService.discontinueMedication(
                medicationId, userId);
    }

    @PostMapping("/medications/{medicationId}/administrations")
    @PreAuthorize("hasAnyRole('NURSE','DOCTOR')")
    public MedicationAdministration recordAdministration(
            @PathVariable UUID medicationId,
            @RequestParam UUID nurseId,
            @RequestParam com.careflow.ai.entity.AdminStatus status,
            @RequestParam(required = false) String notes) {
        return medicationAdministrationService.recordAdministration(
                medicationId,
                nurseId,
                status,
                notes);
    }

    @GetMapping("/medications/{medicationId}/administrations")
    @PreAuthorize("isAuthenticated()")
    public List<MedicationAdministration> getMedicationHistory(
            @PathVariable UUID medicationId) {
        return medicationAdministrationService
                .getMedicationHistory(medicationId);
    }

    @PostMapping("/appointments")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public Appointment scheduleAppointment(
            @RequestParam UUID patientId,
            @RequestParam UUID doctorId,
            @RequestParam LocalDateTime scheduledAt,
            @RequestParam UUID createdByUserId) {
        return appointmentService.scheduleAppointment(
                patientId,
                doctorId,
                scheduledAt,
                createdByUserId);
    }

    @PatchMapping("/appointments/{appointmentId}/status")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public Appointment updateAppointmentStatus(
            @PathVariable UUID appointmentId,
            @RequestParam AppointmentStatus status,
            @RequestParam UUID updatedByUserId) {
        return appointmentService.updateStatus(
                appointmentId,
                status,
                updatedByUserId);
    }

    @GetMapping("/appointments/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public List<Appointment> getPatientAppointments(
            @PathVariable UUID patientId) {
        return appointmentService.getPatientAppointments(patientId);
    }

    @PostMapping(
            value = "/files/{patientId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','ADMIN')")
    public FileAttachment uploadFile(
            @PathVariable UUID patientId,
            @RequestParam UUID uploadedByUserId,
            @RequestPart("file") MultipartFile file) {
        return fileAttachmentService.uploadFile(
                patientId,
                uploadedByUserId,
                file);
    }

    @GetMapping("/files/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public List<FileAttachment> getPatientFiles(
            @PathVariable UUID patientId) {
        return fileAttachmentService.getPatientFiles(patientId);
    }

    @GetMapping("/files/item/{fileId}")
    @PreAuthorize("isAuthenticated()")
    public FileAttachment getFile(@PathVariable UUID fileId) {
        return fileAttachmentService.getFile(fileId);
    }

    @DeleteMapping("/files/{fileId}")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','ADMIN')")
    public void deleteFile(
            @PathVariable UUID fileId,
            @RequestParam UUID deletedByUserId) {
        fileAttachmentService.deleteFile(
                fileId,
                deletedByUserId);
    }
}

