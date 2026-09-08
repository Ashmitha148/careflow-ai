package com.careflow.ai.controller;

import com.careflow.ai.entity.VitalThreshold;
import com.careflow.ai.entity.VitalType;
import com.careflow.ai.security.CustomUserDetails;
import com.careflow.ai.service.VitalThresholdService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-patient vital alert thresholds (Phase 5).
 *
 * <p>Reads are open to any authenticated user with chart access; writes are
 * restricted to doctors (and admins), mirroring the medication-management
 * split in {@link ClinicalController}. Deleting an override reverts the
 * patient to the global clinical defaults.</p>
 */
@RestController
@RequestMapping("/clinical/vitals/{patientId}/thresholds")
public class VitalThresholdController {

    private final VitalThresholdService vitalThresholdService;

    public VitalThresholdController(VitalThresholdService vitalThresholdService) {
        this.vitalThresholdService = vitalThresholdService;
    }

    /** All overrides configured for the patient (missing types use defaults). */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<VitalThreshold> getThresholds(@PathVariable UUID patientId) {
        return vitalThresholdService.findAllForPatient(patientId);
    }

    /** Create or replace the override for one vital type. */
    @PutMapping("/{type}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public VitalThreshold upsertThreshold(@PathVariable UUID patientId,
                                          @PathVariable String type,
                                          @RequestBody Map<String, Double> body,
                                          @AuthenticationPrincipal CustomUserDetails principal) {
        VitalType vitalType = parseType(type);
        return vitalThresholdService.upsertThreshold(
                patientId,
                vitalType,
                body.get("minValue"),
                body.get("maxValue"),
                body.get("systolicMax"),
                body.get("diastolicMax"),
                principal.getId());
    }

    /** Remove the override — the patient falls back to the global defaults. */
    @DeleteMapping("/{type}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public Map<String, String> deleteThreshold(@PathVariable UUID patientId,
                                               @PathVariable String type,
                                               @AuthenticationPrincipal CustomUserDetails principal) {
        vitalThresholdService.deleteThreshold(patientId, parseType(type), principal.getId());
        return Map.of("message",
                "Threshold override removed; global defaults apply again for " + type);
    }

    private static VitalType parseType(String type) {
        try {
            return VitalType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown vital type '" + type + "'. Valid: OXYGEN, BLOOD_PRESSURE, TEMPERATURE, SUGAR");
        }
    }
}