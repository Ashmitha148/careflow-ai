package com.careflow.ai.repository;

import com.careflow.ai.entity.Medication;
import com.careflow.ai.entity.MedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, UUID> {

    List<Medication> findByPatient_IdOrderByStartDateDesc(UUID patientId);

    List<Medication> findByPatient_IdAndStatus(UUID patientId, MedStatus status);
}
