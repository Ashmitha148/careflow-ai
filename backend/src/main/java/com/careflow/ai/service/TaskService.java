package com.careflow.ai.service;

import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.Notification;
import com.careflow.ai.entity.NotificationType;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.Role;
import com.careflow.ai.entity.Task;
import com.careflow.ai.entity.TaskStatus;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.NotificationRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.TaskRepository;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.security.PatientAuthorizationService;
import com.careflow.ai.websocket.RealtimeEventPublisher;
import com.careflow.ai.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PatientAuthorizationService patientAuthorizationService;
    private final TimelineService timelineService;
    private final AuditLogService auditLogService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public TaskService(
            TaskRepository taskRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            PatientAuthorizationService patientAuthorizationService,
            TimelineService timelineService,
            AuditLogService auditLogService,
            RealtimeEventPublisher realtimeEventPublisher) {
        this.taskRepository = taskRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.patientAuthorizationService = patientAuthorizationService;
        this.timelineService = timelineService;
        this.auditLogService = auditLogService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @CacheEvict(value = CacheConfig.DASHBOARD_SUMMARY_CACHE, allEntries = true)
    @Transactional
    public Task createTask(
            UUID patientId,
            UUID nurseId,
            String title,
            String description,
            LocalDateTime dueAt) {

        patientAuthorizationService.ensurePatientAccess(patientId);

        User doctor = resolveCurrentUser();

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() ->
                        new RuntimeException("Patient not found: " + patientId));

        User nurse = userRepository.findById(nurseId)
                .orElseThrow(() ->
                        new RuntimeException("Nurse not found: " + nurseId));

        Task task = new Task();
        task.setPatient(patient);
        task.setCreatedByDoctor(doctor);
        task.setAssignedNurse(nurse);
        task.setTitle(title);
        task.setDescription(description);
        task.setDueAt(dueAt);
        task.setStatus(TaskStatus.PENDING);

        Task saved = taskRepository.save(task);

        String eventDescription =
                "Task created: " + saved.getTitle()
                        + " assigned to " + nurse.getFullName();

        timelineService.appendEvent(
                patientId,
                EventType.TASK,
                eventDescription,
                doctor.getId()
        );

        Notification notification = new Notification();
        notification.setUser(nurse);
        notification.setType(NotificationType.TASK_ASSIGNED);
        notification.setMessage(
                "New task assigned: " + saved.getTitle()
                        + " for patient " + patient.getName()
        );
        notification.setRead(false);

        notificationRepository.save(notification);

        realtimeEventPublisher.publishNotification(nurse.getId(), notification);

        auditLogService.log(
                doctor.getId(),
                "CREATE_TASK",
                "Task",
                saved.getId(),
                eventDescription
        );

        realtimeEventPublisher.publishTaskUpdate(nurse.getId(), saved);

        realtimeEventPublisher.publishPatientTaskUpdate(saved.getPatient().getId(), saved);

        return saved;
    }

    @CacheEvict(value = CacheConfig.DASHBOARD_SUMMARY_CACHE, allEntries = true)
    @Transactional
    public Task startTask(UUID taskId) {

        ensurePatientAccessForTask(taskId);

        User actor = resolveCurrentUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new RuntimeException("Task not found: " + taskId));

        verifyAssignedNurse(task, actor.getId());

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Task is already completed");
        }

        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled task cannot be started");
        }

        task.setStatus(TaskStatus.IN_PROGRESS);

        Task saved = taskRepository.save(task);

        realtimeEventPublisher.publishTaskUpdate(saved.getAssignedNurse().getId(), saved);

        realtimeEventPublisher.publishPatientTaskUpdate(saved.getPatient().getId(), saved);

        return saved;
    }

    @CacheEvict(value = CacheConfig.DASHBOARD_SUMMARY_CACHE, allEntries = true)
    @Transactional
    public Task completeTask(UUID taskId) {

        ensurePatientAccessForTask(taskId);

        User actor = resolveCurrentUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new RuntimeException("Task not found: " + taskId));

        verifyAssignedNurse(task, actor.getId());

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Task is already completed");
        }

        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled task cannot be completed");
        }

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        Task saved = taskRepository.save(task);

        String eventDescription =
                "Task completed: " + saved.getTitle();

        timelineService.appendEvent(
                saved.getPatient().getId(),
                EventType.TASK,
                eventDescription,
                actor.getId()
        );

        auditLogService.log(
                actor.getId(),
                "COMPLETE_TASK",
                "Task",
                saved.getId(),
                eventDescription
        );

        realtimeEventPublisher.publishTaskUpdate(saved.getAssignedNurse().getId(), saved);

        realtimeEventPublisher.publishPatientTaskUpdate(saved.getPatient().getId(), saved);

        return saved;
    }

    @CacheEvict(value = CacheConfig.DASHBOARD_SUMMARY_CACHE, allEntries = true)
    @Transactional
    public Task cancelTask(UUID taskId) {

        ensurePatientAccessForTask(taskId);

        User actor = resolveCurrentUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new RuntimeException("Task not found: " + taskId));

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Completed task cannot be cancelled");
        }

        task.setStatus(TaskStatus.CANCELLED);

        Task saved = taskRepository.save(task);

        String eventDescription =
                "Task cancelled: " + saved.getTitle();

        timelineService.appendEvent(
                saved.getPatient().getId(),
                EventType.TASK,
                eventDescription,
                actor.getId()
        );

        auditLogService.log(
                actor.getId(),
                "CANCEL_TASK",
                "Task",
                saved.getId(),
                eventDescription
        );

        realtimeEventPublisher.publishTaskUpdate(saved.getAssignedNurse().getId(), saved);

        realtimeEventPublisher.publishPatientTaskUpdate(saved.getPatient().getId(), saved);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksForNurse(UUID nurseId) {
        ensureSelfOrAdmin(nurseId);
        return taskRepository
                .findByAssignedNurse_IdOrderByDueAtAsc(nurseId);
    }

    @Transactional(readOnly = true)
    public List<Task> getPendingTasksForNurse(UUID nurseId) {
        ensureSelfOrAdmin(nurseId);
        return taskRepository
                .findByAssignedNurse_IdAndStatusOrderByDueAtAsc(
                        nurseId,
                        TaskStatus.PENDING
                );
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksForPatient(UUID patientId) {
        patientAuthorizationService.ensurePatientAccess(patientId);

        return taskRepository
                .findByPatient_IdOrderByCreatedAtDesc(patientId);
    }

    private void ensurePatientAccessForTask(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        patientAuthorizationService.ensurePatientAccess(task.getPatient().getId());
    }

    private User resolveCurrentUser() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof com.careflow.ai.security.CustomUserDetails details)) {
            throw new RuntimeException("Authentication required");
        }
        return details.getUser();
    }

    private void ensureSelfOrAdmin(UUID nurseId) {
        User actor = resolveCurrentUser();
        if (actor.getRole() == Role.ADMIN || actor.getId().equals(nurseId)) {
            return;
        }
        throw new org.springframework.security.access.AccessDeniedException(
                "Not authorized to view another nurse's tasks");
    }

    @Transactional(readOnly = true)
    public Task getTask(UUID taskId) {
        ensurePatientAccessForTask(taskId);

        return taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new RuntimeException("Task not found: " + taskId));
    }

    private void verifyAssignedNurse(Task task, UUID nurseId) {
        if (task.getAssignedNurse() == null
                || !task.getAssignedNurse().getId().equals(nurseId)) {
            throw new IllegalStateException(
                    "Only the assigned nurse can modify this task");
        }
    }
}
