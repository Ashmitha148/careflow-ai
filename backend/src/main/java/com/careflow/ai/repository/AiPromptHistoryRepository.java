package com.careflow.ai.repository;

import com.careflow.ai.entity.AiPromptHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiPromptHistoryRepository extends JpaRepository<AiPromptHistory, UUID> {

    List<AiPromptHistory> findByPatient_IdOrderByCreatedAtDesc(UUID patientId);

    List<AiPromptHistory> findByUser_IdOrderByCreatedAtDesc(UUID userId);
}
