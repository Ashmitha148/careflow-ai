package com.careflow.ai;

import com.careflow.ai.dto.CursorPageResponse;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.Role;
import com.careflow.ai.entity.User;
import com.careflow.ai.entity.Vital;
import com.careflow.ai.entity.VitalType;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.repository.VitalRepository;
import com.careflow.ai.security.CustomUserDetails;
import com.careflow.ai.service.VitalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
public class DatabaseLevelPaginationTest {

    private static final UUID ELEANOR_PATIENT_ID = UUID.fromString("a1111111-1111-1111-1111-111111111111");

    @Autowired
    private VitalService vitalService;

    @Autowired
    private VitalRepository vitalRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    private User doctor;
    private Patient patient;

    @BeforeEach
    void setUp() {
        doctor = userRepository.findByEmail("doctor1@careflow.ai").orElseThrow();
        patient = patientRepository.findById(ELEANOR_PATIENT_ID).orElseThrow();

        CustomUserDetails details = new CustomUserDetails(doctor);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Seed 105 vitals
        List<Vital> vitals = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().minusDays(10);
        for (int i = 0; i < 105; i++) {
            Vital v = new Vital();
            v.setPatient(patient);
            v.setRecordedBy(doctor);
            v.setType(VitalType.OXYGEN);
            v.setValue("98." + (i % 10));
            v.setRecordedAt(base.plusMinutes(i));
            vitals.add(v);
        }
        vitalRepository.saveAll(vitals);
    }

    @Test
    void testBoundedDatabaseLevelPagination() {
        CursorPageResponse<Vital> page = vitalService.getPatientVitalsCursor(ELEANOR_PATIENT_ID, null, 10);
        assertNotNull(page);
        assertEquals(10, page.getItems().size(), "Page 1 must contain exactly requested limit of 10 items");
        assertTrue(page.isHasMore(), "hasMore must be true when 105 records exist");
        assertNotNull(page.getNextCursor());
    }
}

