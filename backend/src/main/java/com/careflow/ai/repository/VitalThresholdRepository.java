package com.careflow.ai.repository;

import com.careflow.ai.entity.VitalThreshold;
import com.careflow.ai.entity.VitalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VitalThresholdRepository extends JpaRepository<VitalThreshold, UUID> {

    Optional<VitalThreshold> findByPatient_IdAndVitalType(UUID patientId, VitalType vitalType);

    List<VitalThreshold> findByPatient_IdOrderByVitalType(UUID patientId);
}