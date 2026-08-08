package com.careflow.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientAccessId implements Serializable {

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "user_id")
    private UUID userId;
}
