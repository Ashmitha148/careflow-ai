package com.careflow.ai.repository;

import com.careflow.ai.entity.ShiftHandoff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShiftHandoffRepository extends JpaRepository<ShiftHandoff, UUID> {

    List<ShiftHandoff> findByPatient_IdOrderByShiftDateDesc(UUID patientId);

    List<ShiftHandoff> findByToNurse_IdAndShiftDate(UUID nurseId, LocalDate shiftDate);

    List<ShiftHandoff> findByFromNurse_IdOrderByShiftDateDesc(UUID nurseId);

    long countByPatient_IdIn(Collection<UUID> patientIds);
}
