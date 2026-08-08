package com.careflow.ai.controller;

import com.careflow.ai.entity.Task;
import com.careflow.ai.service.TaskService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public Task createTask(
            @RequestParam UUID patientId,
            @RequestParam UUID doctorId,
            @RequestParam UUID assignedNurseId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) java.time.LocalDateTime dueAt) {
        return taskService.createTask(
                patientId,
                doctorId,
                assignedNurseId,
                title,
                description,
                dueAt);
    }

    @PostMapping("/{taskId}/start")
    @PreAuthorize("hasRole('NURSE')")
    public Task startTask(
            @PathVariable UUID taskId,
            @RequestParam UUID nurseId) {
        return taskService.startTask(taskId, nurseId);
    }

    @PostMapping("/{taskId}/complete")
    @PreAuthorize("hasRole('NURSE')")
    public Task completeTask(
            @PathVariable UUID taskId,
            @RequestParam UUID nurseId) {
        return taskService.completeTask(taskId, nurseId);
    }

    @PostMapping("/{taskId}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public Task cancelTask(
            @PathVariable UUID taskId,
            @RequestParam UUID userId) {
        return taskService.cancelTask(taskId, userId);
    }

    @GetMapping("/nurse/{nurseId}")
    @PreAuthorize("hasAnyRole('NURSE','DOCTOR','ADMIN')")
    public List<Task> getTasksForNurse(
            @PathVariable UUID nurseId) {
        return taskService.getTasksForNurse(nurseId);
    }

    @GetMapping("/nurse/{nurseId}/pending")
    @PreAuthorize("hasAnyRole('NURSE','DOCTOR','ADMIN')")
    public List<Task> getPendingTasksForNurse(
            @PathVariable UUID nurseId) {
        return taskService.getPendingTasksForNurse(nurseId);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public List<Task> getTasksForPatient(
            @PathVariable UUID patientId) {
        return taskService.getTasksForPatient(patientId);
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public Task getTask(@PathVariable UUID taskId) {
        return taskService.getTask(taskId);
    }
}

