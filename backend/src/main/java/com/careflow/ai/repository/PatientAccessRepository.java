package com.careflow.ai.repository;

import com.careflow.ai.entity.PatientAccess;
import com.careflow.ai.entity.PatientAccessId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientAccessRepository extends JpaRepository<PatientAccess, PatientAccessId> {

    List<PatientAccess> findByPatient_Id(UUID patientId);

    List<PatientAccess> findByUser_Id(UUID userId);

    boolean existsByPatient_IdAndUser_Id(UUID patientId, UUID userId);
}
