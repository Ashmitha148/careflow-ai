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
            @RequestBody Vital vital) {
        return vitalService.recordVital(patientId, vital);
    }

    @GetMapping("/vitals/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public Object getVitals(
            @PathVariable UUID patientId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        if (cursor != null || (limit != null && limit > 0)) {
            int effectiveLimit = limit != null ? limit : 50;
            return vitalService.getPatientVitalsCursor(patientId, cursor, effectiveLimit);
        }
        return vitalService.getPatientVitals(patientId);
    }

    @PostMapping("/medications")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public Medication prescribeMedication(
            @RequestParam UUID patientId,
            @RequestParam String name,
            @RequestParam String dosage,
            @RequestParam String frequency,
            @RequestParam java.time.LocalDate startDate,
            @RequestParam(required = false) java.time.LocalDate endDate,
            @RequestParam(required = false, defaultValue = "false") boolean important) {
        return medicationService.prescribeMedication(
                patientId,
                name,
                dosage,
                frequency,
                startDate,
                endDate,
                important);
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
            @PathVariable UUID medicationId) {
        return medicationService.discontinueMedication(medicationId);
    }

    @PostMapping("/medications/{medicationId}/administrations")
    @PreAuthorize("hasAnyRole('NURSE','DOCTOR')")
    public MedicationAdministration recordAdministration(
            @PathVariable UUID medicationId,
            @RequestParam com.careflow.ai.entity.AdminStatus status,
            @RequestParam(required = false) String notes) {
        return medicationAdministrationService.recordAdministration(
                medicationId,
                status,
                notes);
    }

    /**
     * Phase 5B: Record medication administration with video verification.
     * Required when patient has remote supervision enabled, no caregiver present,
     * and medication is marked as important.
     */
    @PostMapping(
            value = "/medications/{medicationId}/administrations/verify",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('NURSE','DOCTOR','ADMIN')")
    public MedicationAdministration recordAdministrationWithVideo(
            @PathVariable UUID medicationId,
            @RequestParam com.careflow.ai.entity.AdminStatus status,
            @RequestParam(required = false) String notes,
            @RequestPart(value = "video", required = false) MultipartFile video) {
        return medicationAdministrationService.recordAdministrationWithVideoVerification(
                medicationId,
                status,
                notes,
                video);
    }

    /**
     * Phase 5B: Check if a medication administration requires video verification.
     */
    @GetMapping("/medications/{medicationId}/verification-required")
    @PreAuthorize("isAuthenticated()")
    public boolean isVerificationRequired(
            @PathVariable UUID medicationId) {
        Medication medication = medicationService.getMedication(medicationId);
        return medicationAdministrationService.isRemoteVerificationRequired(
                medication.getPatient(), medication);
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
            @RequestParam LocalDateTime scheduledAt) {
        return appointmentService.scheduleAppointment(
                patientId,
                doctorId,
                scheduledAt);
    }

    @PatchMapping("/appointments/{appointmentId}/status")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public Appointment updateAppointmentStatus(
            @PathVariable UUID appointmentId,
            @RequestParam AppointmentStatus status) {
        return appointmentService.updateStatus(
                appointmentId,
                status);
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
            @RequestPart("file") MultipartFile file) {
        return fileAttachmentService.uploadFile(
                patientId,
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
    public void deleteFile(@PathVariable UUID fileId) {
        fileAttachmentService.deleteFile(fileId);
    }
}