package com.careflow.ai.repository;

import com.careflow.ai.entity.MedicationAdministration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationAdministrationRepository extends JpaRepository<MedicationAdministration, UUID> {

    List<MedicationAdministration> findByMedication_IdOrderByAdministeredAtDesc(UUID medicationId);

    List<MedicationAdministration> findByAdministeredBy_IdOrderByAdministeredAtDesc(UUID nurseId);
}
