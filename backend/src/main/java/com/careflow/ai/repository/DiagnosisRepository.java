package com.careflow.ai.repository;

import com.careflow.ai.entity.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {

    List<Diagnosis> findByPatient_IdOrderByDiagnosedAtDesc(UUID patientId);

    List<Diagnosis> findByDoctor_IdOrderByDiagnosedAtDesc(UUID doctorId);
}
