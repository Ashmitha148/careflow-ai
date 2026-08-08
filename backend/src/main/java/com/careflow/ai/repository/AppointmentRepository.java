package com.careflow.ai.repository;

import com.careflow.ai.entity.Appointment;
import com.careflow.ai.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByPatient_IdOrderByScheduledAtDesc(UUID patientId);

    List<Appointment> findByDoctor_IdAndStatusOrderByScheduledAtAsc(UUID doctorId, AppointmentStatus status);
}
