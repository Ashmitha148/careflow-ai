package com.careflow.ai.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "patient_access")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientAccess {

    @EmbeddedId
    private PatientAccessId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("patientId")
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "relationship", nullable = false)
    private String relationship;
}
