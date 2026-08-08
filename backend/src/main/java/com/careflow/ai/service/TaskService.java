package com.careflow.ai.service;

import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.Notification;
import com.careflow.ai.entity.NotificationType;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.Task;
import com.careflow.ai.entity.TaskStatus;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.NotificationRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.TaskRepository;
import com.careflow.ai.repository.UserRepository;
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
    private final TimelineService timelineService;
    private final AuditLogService auditLogService;

    public TaskService(
            TaskRepository taskRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            TimelineService timelineService,
            AuditLogService auditLogService) {
        this.taskRepository = taskRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.timelineService = timelineService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Task createTask(
            UUID patientId,
            UUID doctorId,
            UUID nurseId,
            String title,
            String description,
            LocalDateTime dueAt) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() ->
                        new RuntimeException("Patient not found: " + patientId));

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() ->
                        new RuntimeException("Doctor not found: " + doctorId));

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
                doctorId
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

        auditLogService.log(
                doctorId,
                "CREATE_TASK",
                "Task",
                saved.getId(),
                eventDescription
        );

        return saved;
    }

    @Transactional
    public Task startTask(UUID taskId, UUID nurseId) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new RuntimeException("Task not found: " + taskId));

        verifyAssignedNurse(task, nurseId);

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Task is already completed");
        }

        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled task cannot be started");
        }

        task.setStatus(TaskStatus.IN_PROGRESS);

        return taskRepository.save(task);
    }

    @Transactional
    public Task completeTask(UUID taskId, UUID nurseId) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new RuntimeException("Task not found: " + taskId));

        verifyAssignedNurse(task, nurseId);

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
                nurseId
        );

        auditLogService.log(
                nurseId,
                "COMPLETE_TASK",
                "Task",
                saved.getId(),
                eventDescription
        );

        return saved;
    }

    @Transactional
    public Task cancelTask(UUID taskId, UUID userId) {

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
                userId
        );

        auditLogService.log(
                userId,
                "CANCEL_TASK",
                "Task",
                saved.getId(),
                eventDescription
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksForNurse(UUID nurseId) {
        return taskRepository
                .findByAssignedNurse_IdOrderByDueAtAsc(nurseId);
    }

    @Transactional(readOnly = true)
    public List<Task> getPendingTasksForNurse(UUID nurseId) {
        return taskRepository
                .findByAssignedNurse_IdAndStatusOrderByDueAtAsc(
                        nurseId,
                        TaskStatus.PENDING
                );
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksForPatient(UUID patientId) {
        return taskRepository
                .findByPatient_IdOrderByCreatedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public Task getTask(UUID taskId) {
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
