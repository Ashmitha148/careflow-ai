package com.careflow.ai.service;

import com.careflow.ai.dto.DashboardDto.ActivityItem;
import com.careflow.ai.dto.DashboardDto.Summary;
import com.careflow.ai.entity.AppointmentStatus;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.PatientAccess;
import com.careflow.ai.entity.Role;
import com.careflow.ai.entity.TaskStatus;
import com.careflow.ai.entity.TimelineEvent;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.AppointmentRepository;
import com.careflow.ai.repository.PatientAccessRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.ShiftHandoffRepository;
import com.careflow.ai.repository.TaskRepository;
import com.careflow.ai.repository.TimelineEventRepository;
import com.careflow.ai.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DashboardService {

    private static final int RECENT_ACTIVITY_LIMIT = 5;

    private final PatientRepository patientRepository;
    private final PatientAccessRepository patientAccessRepository;
    private final TaskRepository taskRepository;
    private final AppointmentRepository appointmentRepository;
    private final ShiftHandoffRepository shiftHandoffRepository;
    private final TimelineEventRepository timelineEventRepository;

    public DashboardService(PatientRepository patientRepository,
                            PatientAccessRepository patientAccessRepository,
                            TaskRepository taskRepository,
                            AppointmentRepository appointmentRepository,
                            ShiftHandoffRepository shiftHandoffRepository,
                            TimelineEventRepository timelineEventRepository) {
        this.patientRepository = patientRepository;
        this.patientAccessRepository = patientAccessRepository;
        this.taskRepository = taskRepository;
        this.appointmentRepository = appointmentRepository;
        this.shiftHandoffRepository = shiftHandoffRepository;
        this.timelineEventRepository = timelineEventRepository;
    }

    @Transactional(readOnly = true)
    public Summary getSummary() {
        User actor = resolveCurrentUser();
        Set<UUID> accessibleIds = resolveAccessiblePatientIds(actor);

        Summary.SummaryBuilder builder = Summary.builder()
                .activePatients(accessibleIds.size());

        if (accessibleIds.isEmpty()) {
            return builder
                    .tasksAwaitingAction(0)
                    .tasksCompleted(0)
                    .tasksTotal(0)
                    .upcomingAppointments(0)
                    .handoffs(0)
                    .recentActivity(List.of())
                    .build();
        }

        long tasksAwaiting = taskRepository.countByPatient_IdInAndStatus(accessibleIds, TaskStatus.PENDING);
        long tasksTotal = taskRepository.countByPatient_IdIn(accessibleIds);
        long tasksCompleted = taskRepository.countByPatient_IdInAndStatus(accessibleIds, TaskStatus.COMPLETED);
        long upcomingAppointments = appointmentRepository
                .countByPatient_IdInAndStatusAndScheduledAtGreaterThanEqual(
                        accessibleIds, AppointmentStatus.SCHEDULED, LocalDateTime.now());
        long handoffs = shiftHandoffRepository.countByPatient_IdIn(accessibleIds);

        return builder
                .tasksAwaitingAction(tasksAwaiting)
                .tasksCompleted(tasksCompleted)
                .tasksTotal(tasksTotal)
                .upcomingAppointments(upcomingAppointments)
                .handoffs(handoffs)
                .recentActivity(resolveRecentActivity(accessibleIds))
                .build();
    }

    private List<ActivityItem> resolveRecentActivity(Collection<UUID> patientIds) {
        List<TimelineEvent> events = timelineEventRepository
                .findByPatient_IdInOrderByCreatedAtDesc(patientIds);

        List<ActivityItem> items = new ArrayList<>();
        int count = 0;
        for (TimelineEvent event : events) {
            if (count >= RECENT_ACTIVITY_LIMIT) {
                break;
            }
            Patient patient = event.getPatient();
            items.add(ActivityItem.builder()
                    .id(event.getId())
                    .patientId(patient != null ? patient.getId() : null)
                    .patientName(patient != null ? patient.getName() : null)
                    .eventType(event.getEventType())
                    .description(event.getDescription())
                    .createdAt(event.getCreatedAt())
                    .build());
            count++;
        }
        return items;
    }

    private Set<UUID> resolveAccessiblePatientIds(User actor) {
        if (actor.getRole() == Role.ADMIN) {
            return patientRepository.findAll().stream()
                    .map(Patient::getId)
                    .collect(java.util.stream.Collectors.toSet());
        }

        Set<UUID> ids = new HashSet<>(patientRepository.findAccessibleIdsByAssignment(actor.getId()));
        List<PatientAccess> accesses = patientAccessRepository.findByUser_Id(actor.getId());
        for (PatientAccess access : accesses) {
            if (access != null && access.getId() != null) {
                ids.add(access.getId().getPatientId());
            }
        }
        return ids;
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            throw new RuntimeException("Authentication required");
        }
        return details.getUser();
    }
}