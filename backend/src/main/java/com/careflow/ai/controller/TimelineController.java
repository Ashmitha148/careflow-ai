package com.careflow.ai.controller;

import com.careflow.ai.dto.TimelineDto.TimelineEventResponse;
import com.careflow.ai.entity.EventType;
import com.careflow.ai.service.TimelineService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients/{patientId}/timeline")
public class TimelineController {

    private final TimelineService timelineService;

    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<TimelineEventResponse> getTimeline(
            @PathVariable UUID patientId,
            @RequestParam(required = false) EventType eventType,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end) {

        return timelineService.getTimeline(
                patientId,
                eventType,
                start,
                end
        );
    }

    @GetMapping("/replay/24h")
    @PreAuthorize("isAuthenticated()")
    public List<TimelineEventResponse> replay24Hours(
            @PathVariable UUID patientId) {
        return timelineService.replayLast24Hours(patientId);
    }

    @GetMapping("/replay/7d")
    @PreAuthorize("isAuthenticated()")
    public List<TimelineEventResponse> replay7Days(
            @PathVariable UUID patientId) {
        return timelineService.replayLast7Days(patientId);
    }

    @PostMapping("/{eventId}/correction")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','ADMIN')")
    public TimelineEventResponse correctEvent(
            @PathVariable UUID eventId,
            @RequestParam String correctionDescription,
            @RequestParam UUID createdByUserId) {

        return timelineService.correctEvent(
                eventId,
                correctionDescription,
                createdByUserId
        );
    }
}
