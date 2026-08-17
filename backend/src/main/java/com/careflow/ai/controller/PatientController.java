package com.careflow.ai.controller;

import com.careflow.ai.dto.PatientDto.PatientResponse;
import com.careflow.ai.dto.PatientDto.RegisterRequest;
import com.careflow.ai.dto.PatientDto.UpdateRequest;
import com.careflow.ai.service.PatientService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','ADMIN')")
    public PatientResponse register(
            @RequestBody RegisterRequest request,
            @RequestParam UUID actorUserId) {
        return patientService.register(request, actorUserId);
    }

    @PutMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','ADMIN')")
    public PatientResponse update(
            @PathVariable UUID patientId,
            @RequestBody UpdateRequest request,
            @RequestParam UUID actorUserId) {
        return patientService.update(patientId, request, actorUserId);
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public java.util.List<PatientResponse> getMyPatients() {
        return patientService.getMyPatients();
    }

    @GetMapping("/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public PatientResponse getById(
            @PathVariable UUID patientId) {
        return patientService.getById(patientId);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<PatientResponse> search(
            @RequestParam(required = false) String query,
            Pageable pageable) {
        return patientService.search(query, pageable);
    }
}
