package com.careflow.ai.controller;

import com.careflow.ai.entity.AiPromptHistory;
import com.careflow.ai.service.CopilotService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/copilot")
public class CopilotController {

    private final CopilotService copilotService;

    public CopilotController(CopilotService copilotService) {
        this.copilotService = copilotService;
    }

    @PostMapping("/timeline-question")
    @PreAuthorize("isAuthenticated()")
    public String timelineQuestion(
            @RequestParam UUID patientId,
            @RequestParam UUID userId,
            @RequestBody String question) {
        return copilotService.timelineQuestion(patientId, userId, question);
    }

    @PostMapping("/patient-summary")
    @PreAuthorize("isAuthenticated()")
    public String patientSummary(
            @RequestParam UUID patientId,
            @RequestParam UUID userId) {
        return copilotService.patientSummary(patientId, userId);
    }

    @PostMapping("/plain-language")
    @PreAuthorize("isAuthenticated()")
    public String plainLanguage(
            @RequestParam UUID patientId,
            @RequestParam UUID userId,
            @RequestBody String text) {
        return copilotService.plainLanguage(patientId, userId, text);
    }

    @GetMapping("/patient/{patientId}/history")
    @PreAuthorize("isAuthenticated()")
    public List<AiPromptHistory> getPatientPromptHistory(
            @PathVariable UUID patientId) {
        return copilotService.getPatientPromptHistory(patientId);
    }

    @GetMapping("/user/{userId}/history")
    @PreAuthorize("isAuthenticated()")
    public List<AiPromptHistory> getUserPromptHistory(
            @PathVariable UUID userId) {
        return copilotService.getUserPromptHistory(userId);
    }
}
