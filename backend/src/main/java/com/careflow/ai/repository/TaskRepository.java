package com.careflow.ai.repository;

import com.careflow.ai.entity.Task;
import com.careflow.ai.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByAssignedNurse_IdOrderByDueAtAsc(UUID nurseId);

    List<Task> findByAssignedNurse_IdAndStatusOrderByDueAtAsc(UUID nurseId, TaskStatus status);

    List<Task> findByPatient_IdOrderByCreatedAtDesc(UUID patientId);
}
