package com.careflow.ai.repository;

import com.careflow.ai.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByMrn(String mrn);

    boolean existsByMrn(String mrn);

    long countByMrnStartingWith(String mrnPrefix);

    @Query("""
            SELECT p FROM Patient p
            WHERE (:query IS NULL OR :query = ''
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(p.mrn) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<Patient> searchByNameOrMrn(@Param("query") String query, Pageable pageable);
}
