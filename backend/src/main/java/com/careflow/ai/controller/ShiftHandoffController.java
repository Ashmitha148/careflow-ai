package com.careflow.ai.controller;

import com.careflow.ai.entity.Role;
import com.careflow.ai.entity.ShiftHandoff;
import com.careflow.ai.service.ShiftHandoffService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/shift-handoffs")
public class ShiftHandoffController {

    private final ShiftHandoffService shiftHandoffService;

    public ShiftHandoffController(
            ShiftHandoffService shiftHandoffService) {
        this.shiftHandoffService = shiftHandoffService;
    }

    /**
     * Phase 2: returns 202 Accepted — the handoff row is committed (aiStatus
     * PENDING) but the AI summary is generated asynchronously by the outbox
     * worker. The completed summary arrives on
     * /topic/patient/{patientId}/handoffs; clients that missed the socket
     * event can poll {@code GET /shift-handoffs/{handoffId}} (polling
     * fallback), whose body carries aiStatus/aiSummary/aiError.
     */
    @PostMapping
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<ShiftHandoff> createHandoff(
            @RequestParam UUID patientId,
            @RequestParam UUID toNurseId,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String pendingTasks,
            @RequestParam(required = false) String observations,
            @RequestParam(required = false) String completedTasks,
            @RequestParam(required = false) String nextShiftInstructions,
            @RequestParam LocalDate shiftDate) {
        ShiftHandoff handoff = shiftHandoffService.createHandoff(
                patientId,
                toNurseId,
                notes,
                pendingTasks,
                observations,
                completedTasks,
                nextShiftInstructions,
                shiftDate);
        return ResponseEntity.accepted().body(handoff);
    }

    @GetMapping("/nurses")
    @PreAuthorize("hasAnyRole('NURSE','DOCTOR','ADMIN')")
    public List<Map<String, Object>> getNurseOptions() {
        return shiftHandoffService.getNurseOptions();
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public List<ShiftHandoff> getPatientHandoffs(
            @PathVariable UUID patientId) {
        return shiftHandoffService
                .getPatientHandoffs(patientId);
    }

    @GetMapping("/incoming/{nurseId}")
    @PreAuthorize("hasAnyRole('NURSE','ADMIN')")
    public List<ShiftHandoff> getIncomingHandoffs(
            @PathVariable UUID nurseId,
            @RequestParam LocalDate shiftDate) {
        return shiftHandoffService
                .getIncomingHandoffs(nurseId, shiftDate);
    }

    @GetMapping("/outgoing/{nurseId}")
    @PreAuthorize("hasAnyRole('NURSE','ADMIN')")
    public List<ShiftHandoff> getOutgoingHandoffs(
            @PathVariable UUID nurseId) {
        return shiftHandoffService
                .getOutgoingHandoffs(nurseId);
    }

    @GetMapping("/{handoffId}")
    @PreAuthorize("isAuthenticated()")
    public ShiftHandoff getHandoff(
            @PathVariable UUID handoffId) {
        return shiftHandoffService.getHandoff(handoffId);
    }

    @PatchMapping("/{handoffId}/ai-summary")
    @PreAuthorize("hasAnyRole('NURSE','DOCTOR','ADMIN')")
    public ShiftHandoff updateAiSummary(
            @PathVariable UUID handoffId,
            @RequestParam String aiSummary) {
        return shiftHandoffService.updateAiSummary(
                handoffId,
                aiSummary);
    }
}

