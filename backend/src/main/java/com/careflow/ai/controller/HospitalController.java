package com.careflow.ai.controller;

import com.careflow.ai.dto.HospitalDto.CreateHospitalRequest;
import com.careflow.ai.dto.HospitalDto.HospitalResponse;
import com.careflow.ai.dto.HospitalDto.StaffMemberResponse;
import com.careflow.ai.service.HospitalService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Phase 8 — Hospital (tenant) administration endpoints.
 *
 * Tenant admins are the seeded ADMIN users; every endpoint here is admin-only.
 */
@RestController
@RequestMapping("/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<HospitalResponse> list() {
        return hospitalService.list();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public HospitalResponse create(@Valid @RequestBody CreateHospitalRequest request) {
        return hospitalService.create(request);
    }

    @GetMapping("/{hospitalId}/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StaffMemberResponse> listStaff(@PathVariable UUID hospitalId) {
        return hospitalService.listStaff(hospitalId);
    }

    @PutMapping("/{hospitalId}/staff/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public StaffMemberResponse assignStaff(@PathVariable UUID hospitalId,
                                           @PathVariable UUID userId) {
        return hospitalService.assignStaff(hospitalId, userId);
    }
}
