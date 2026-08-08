package com.careflow.ai.repository;

import com.careflow.ai.entity.Vital;
import com.careflow.ai.entity.VitalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface VitalRepository extends JpaRepository<Vital, UUID> {

    List<Vital> findByPatient_IdOrderByRecordedAtDesc(UUID patientId);

    List<Vital> findByPatient_IdAndTypeOrderByRecordedAtDesc(UUID patientId, VitalType type);

    List<Vital> findByPatient_IdAndRecordedAtBetweenOrderByRecordedAtAsc(
            UUID patientId, LocalDateTime start, LocalDateTime end);
}
