package com.careflow.ai.service;

import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.User;
import com.careflow.ai.entity.Vital;
import com.careflow.ai.entity.VitalThreshold;
import com.careflow.ai.entity.VitalType;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.repository.VitalThresholdRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-patient alert-threshold evaluation (Phase 5 — configurable thresholds).
 *
 * <p>Resolves the override row for (patient, vital type) and evaluates a
 * recorded value against it. When no override exists, evaluation falls back to
 * the global clinical defaults in {@link VitalService#isCritical} — the
 * patient-agnostic behavior all previous phases shipped with.</p>
 */
@Service
public class VitalThresholdService {

    private final VitalThresholdRepository thresholdRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public VitalThresholdService(VitalThresholdRepository thresholdRepository,
                                 PatientRepository patientRepository,
                                 UserRepository userRepository,
                                 AuditLogService auditLogService) {
        this.thresholdRepository = thresholdRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    /** Fetch the raw override row, or empty when the patient uses defaults. */
    public Optional<VitalThreshold> findOverride(UUID patientId, VitalType type) {
        return thresholdRepository.findByPatient_IdAndVitalType(patientId, type);
    }

    /** All overrides configured for a patient (for GET of the full map). */
    public List<VitalThreshold> findAllForPatient(UUID patientId) {
        return thresholdRepository.findByPatient_IdOrderByVitalType(patientId);
    }

    /**
     * True when the recorded value breaches the patient's override for this
     * vital type. Returns {@code false} when there is no override — the caller
     * then applies the global defaults.
     */
    public boolean isCriticalWithOverride(Vital vital, VitalThreshold override) {
        if (override == null || vital == null
                || vital.getType() == null || vital.getValue() == null) {
            return false;
        }
        String value = vital.getValue().trim();
        try {
            switch (override.getVitalType()) {
                case OXYGEN:
                    return override.getMinValue() != null
                            && Double.parseDouble(value) < override.getMinValue();
                case TEMPERATURE:
                    return override.getMaxValue() != null
                            && Double.parseDouble(stripUnit(value)) > override.getMaxValue();
                case SUGAR:
                    double sugar = Double.parseDouble(value);
                    return (override.getMinValue() != null && sugar < override.getMinValue())
                            || (override.getMaxValue() != null && sugar > override.getMaxValue());
                case BLOOD_PRESSURE:
                    String[] parts = value.split("[/\\-]");
                    if (parts.length != 2) {
                        return false;
                    }
                    double systolic = Double.parseDouble(parts[0].trim());
                    double diastolic = Double.parseDouble(parts[1].trim());
                    return (override.getSystolicMax() != null && systolic > override.getSystolicMax())
                            || (override.getDiastolicMax() != null
                                    && diastolic > override.getDiastolicMax());
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            return false; // malformed values are never alerts
        }
    }

    /**
     * Human-readable description of the ACTIVE threshold for a patient and
     * vital type — the override when present, otherwise the global default.
     * Used so the timeline explains exactly why an alert fired.
     */
    public String describeActiveThreshold(UUID patientId, VitalType type) {
        if (type == null) {
            return "N/A";
        }
        Optional<VitalThreshold> override =
                thresholdRepository.findByPatient_IdAndVitalType(patientId, type);
        if (override.isPresent()) {
            VitalThreshold o = override.get();
            switch (type) {
                case OXYGEN:
                    if (o.getMinValue() != null) {
                        return String.format(Locale.ROOT,
                                "Oxygen saturation < %.0f%% (patient-specific)", o.getMinValue());
                    }
                    break;
                case TEMPERATURE:
                    if (o.getMaxValue() != null) {
                        return String.format(Locale.ROOT,
                                "Temperature > %.1f °F (patient-specific)", o.getMaxValue());
                    }
                    break;
                case SUGAR:
                    if (o.getMinValue() != null && o.getMaxValue() != null) {
                        return String.format(Locale.ROOT,
                                "Blood sugar outside %.0f-%.0f mg/dL (patient-specific)",
                                o.getMinValue(), o.getMaxValue());
                    }
                    if (o.getMinValue() != null) {
                        return String.format(Locale.ROOT,
                                "Blood sugar < %.0f mg/dL (patient-specific)", o.getMinValue());
                    }
                    if (o.getMaxValue() != null) {
                        return String.format(Locale.ROOT,
                                "Blood sugar > %.0f mg/dL (patient-specific)", o.getMaxValue());
                    }
                    break;
                case BLOOD_PRESSURE:
                    if (o.getSystolicMax() != null && o.getDiastolicMax() != null) {
                        return String.format(Locale.ROOT,
                                "Systolic > %.0f mmHg or diastolic > %.0f mmHg (patient-specific)",
                                o.getSystolicMax(), o.getDiastolicMax());
                    }
                    break;
                default:
                    break;
            }
        }
        return VitalService.describeThreshold(type);
    }

    /**
     * Create or replace the override for (patient, vital type). Validates that
     * the supplied bounds match the vital type's semantics and audit-logs the
     * change. {@code null} bounds not relevant to the type are rejected —
     * clients must send only the fields that apply.
     */
    @Transactional
    public VitalThreshold upsertThreshold(UUID patientId,
                                          VitalType type,
                                          Double minValue,
                                          Double maxValue,
                                          Double systolicMax,
                                          Double diastolicMax,
                                          UUID actingUserId) {
        validateBounds(type, minValue, maxValue, systolicMax, diastolicMax);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));
        User actingUser = userRepository.findById(actingUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + actingUserId));

        VitalThreshold threshold = thresholdRepository
                .findByPatient_IdAndVitalType(patientId, type)
                .orElseGet(VitalThreshold::new);
        boolean isNew = threshold.getId() == null;

        threshold.setPatient(patient);
        threshold.setVitalType(type);
        threshold.setMinValue(minValue);
        threshold.setMaxValue(maxValue);
        threshold.setSystolicMax(systolicMax);
        threshold.setDiastolicMax(diastolicMax);
        threshold.setCreatedBy(actingUser);

        VitalThreshold saved = thresholdRepository.save(threshold);

        auditLogService.log(actingUserId,
                isNew ? "SET_VITAL_THRESHOLD" : "UPDATE_VITAL_THRESHOLD",
                "VitalThreshold",
                saved.getId(),
                describeActiveThreshold(patientId, type));

        return saved;
    }

    /** Removes the override so the patient falls back to the global defaults. */
    @Transactional
    public void deleteThreshold(UUID patientId, VitalType type, UUID actingUserId) {
        VitalThreshold threshold = thresholdRepository
                .findByPatient_IdAndVitalType(patientId, type)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No threshold override configured for " + type));

        thresholdRepository.delete(threshold);

        auditLogService.log(actingUserId,
                "DELETE_VITAL_THRESHOLD",
                "VitalThreshold",
                threshold.getId(),
                "Reverted " + type + " to global default: "
                        + VitalService.describeThreshold(type));
    }

    /** Bounds must match the vital type's semantics (see V9 migration). */
    private static void validateBounds(VitalType type,
                                       Double minValue, Double maxValue,
                                       Double systolicMax, Double diastolicMax) {
        switch (type) {
            case OXYGEN:
                require(minValue != null,
                        "OXYGEN override requires minValue (alert when SpO2 falls below it)");
                break;
            case TEMPERATURE:
                require(maxValue != null,
                        "TEMPERATURE override requires maxValue in °F (alert when exceeded)");
                break;
            case SUGAR:
                require(minValue != null || maxValue != null,
                        "SUGAR override requires minValue and/or maxValue (mg/dL)");
                break;
            case BLOOD_PRESSURE:
                require(systolicMax != null && diastolicMax != null,
                        "BLOOD_PRESSURE override requires both systolicMax and diastolicMax (mmHg)");
                break;
            default:
                require(false, "Unsupported vital type: " + type);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /** Removes a trailing C/F unit character (input may be any case). */
    static String stripUnit(String value) {
        String upper = value.toUpperCase(Locale.ROOT);
        if (upper.endsWith("C") || upper.endsWith("F")) {
            return upper.substring(0, upper.length() - 1).trim();
        }
        return value.trim();
    }
}