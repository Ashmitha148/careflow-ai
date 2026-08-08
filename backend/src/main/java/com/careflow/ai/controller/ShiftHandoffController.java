package com.careflow.ai.controller;

import com.careflow.ai.entity.ShiftHandoff;
import com.careflow.ai.service.ShiftHandoffService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/shift-handoffs")
public class ShiftHandoffController {

    private final ShiftHandoffService shiftHandoffService;

    public ShiftHandoffController(
            ShiftHandoffService shiftHandoffService) {
        this.shiftHandoffService = shiftHandoffService;
    }

    @PostMapping
    @PreAuthorize("hasRole('NURSE')")
    public ShiftHandoff createHandoff(
            @RequestParam UUID patientId,
            @RequestParam UUID fromNurseId,
            @RequestParam UUID toNurseId,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String pendingTasks,
            @RequestParam(required = false) String observations,
            @RequestParam(required = false) String completedTasks,
            @RequestParam(required = false) String nextShiftInstructions,
            @RequestParam LocalDate shiftDate) {
        return shiftHandoffService.createHandoff(
                patientId,
                fromNurseId,
                toNurseId,
                notes,
                pendingTasks,
                observations,
                completedTasks,
                nextShiftInstructions,
                shiftDate);
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

