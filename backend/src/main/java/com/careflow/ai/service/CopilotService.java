package com.careflow.ai.service;

import com.careflow.ai.config.GeminiClient;
import com.careflow.ai.dto.TimelineDto.TimelineEventResponse;
import com.careflow.ai.entity.AiPromptHistory;
import com.careflow.ai.entity.Patient;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.AiPromptHistoryRepository;
import com.careflow.ai.repository.PatientRepository;
import com.careflow.ai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CopilotService {

    private static final String SAFETY_INSTRUCTION = """
            You are the CareFlow AI clinical information assistant.
            
            Use ONLY the patient information supplied in the context.
            Do not invent patient facts, medications, diagnoses, measurements,
            events, or other clinical information.
            
            You may summarize and explain documented information.
            You must NOT diagnose conditions, prescribe treatment, recommend
            medications, or make independent clinical decisions.
            
            If the supplied context does not contain enough information,
            explicitly say that the available record does not provide enough
            information.
            
            Clearly distinguish documented facts from interpretation.
            
            Include this disclaimer when appropriate:
            "AI-generated information is for clinical support only and does
            not replace professional medical judgment."
            """;

    private final GeminiClient geminiClient;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AiPromptHistoryRepository aiPromptHistoryRepository;
    private final TimelineService timelineService;

    public CopilotService(
            GeminiClient geminiClient,
            PatientRepository patientRepository,
            UserRepository userRepository,
            AiPromptHistoryRepository aiPromptHistoryRepository,
            TimelineService timelineService) {
        this.geminiClient = geminiClient;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.aiPromptHistoryRepository = aiPromptHistoryRepository;
        this.timelineService = timelineService;
    }

    @Transactional
    public String timelineQuestion(
            UUID patientId,
            UUID userId,
            String question) {

        validateQuestion(question);

        Patient patient = getPatient(patientId);
        User user = getUser(userId);

        List<TimelineEventResponse> events =
                timelineService.replayLast7Days(patientId);

        String context = formatTimeline(events);

        String prompt = SAFETY_INSTRUCTION
                + "\n\nFeature: Timeline Q&A"
                + "\n\nPatient: " + patient.getName()
                + "\n\nTimeline context from the last 7 days:\n"
                + context
                + "\n\nQuestion:\n"
                + question;

        return executeAndRecord(
                patient,
                user,
                "TIMELINE_QA",
                prompt,
                context
        );
    }

    @Transactional
    public String patientSummary(
            UUID patientId,
            UUID userId) {

        Patient patient = getPatient(patientId);
        User user = getUser(userId);

        List<TimelineEventResponse> events =
                timelineService.replayLast7Days(patientId);

        String context = formatTimeline(events);

        String prompt = SAFETY_INSTRUCTION
                + "\n\nFeature: Patient Summary"
                + "\n\nCreate a concise clinical summary using only "
                + "the documented timeline below."
                + "\n\nPatient: " + patient.getName()
                + "\n\nTimeline context:\n"
                + context
                + "\n\nStructure the response as:"
                + "\n1. Recent documented events"
                + "\n2. Current documented concerns"
                + "\n3. Recent medications/procedures if explicitly documented"
                + "\n4. Important follow-up items explicitly documented"
                + "\n5. Information that is missing or unclear";

        return executeAndRecord(
                patient,
                user,
                "PATIENT_SUMMARY",
                prompt,
                context
        );
    }

    @Transactional
    public String plainLanguage(
            UUID patientId,
            UUID userId,
            String text) {

        validateQuestion(text);

        Patient patient = getPatient(patientId);
        User user = getUser(userId);

        String prompt = SAFETY_INSTRUCTION
                + "\n\nFeature: Plain-language explanation"
                + "\n\nRewrite the supplied clinical information "
                + "in simple, patient-friendly language."
                + "\nDo not add facts that are not present."
                + "\nDo not change the meaning."
                + "\nDo not provide diagnosis or treatment advice."
                + "\n\nPatient: " + patient.getName()
                + "\n\nClinical information:\n"
                + text;

        return executeAndRecord(
                patient,
                user,
                "PLAIN_LANGUAGE",
                prompt,
                text
        );
    }

    @Transactional(readOnly = true)
    public List<AiPromptHistory> getPatientPromptHistory(
            UUID patientId) {

        getPatient(patientId);

        return aiPromptHistoryRepository
                .findByPatient_IdOrderByCreatedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public List<AiPromptHistory> getUserPromptHistory(
            UUID userId) {

        getUser(userId);

        return aiPromptHistoryRepository
                .findByUser_IdOrderByCreatedAtDesc(userId);
    }

    private String executeAndRecord(
            Patient patient,
            User user,
            String featureName,
            String prompt,
            String context) {

        AiPromptHistory history = new AiPromptHistory();
        history.setPatient(patient);
        history.setUser(user);
        history.setFeatureName(featureName);
        history.setPrompt(prompt);
        history.setRetrievedContext(context);
        history.setResponseStatus("SUCCESS");

        try {
            String response = geminiClient.generate(prompt);

            history.setAiResponse(response);
            history.setResponseStatus("SUCCESS");

            aiPromptHistoryRepository.save(history);

            return response;

        } catch (RuntimeException ex) {

            history.setAiResponse(
                    "AI response unavailable: " + ex.getMessage());
            history.setResponseStatus("ERROR");

            aiPromptHistoryRepository.save(history);

            throw ex;
        }
    }

    private Patient getPatient(UUID patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Patient not found: " + patientId));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found: " + userId));
    }

    private void validateQuestion(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Question or text cannot be empty");
        }

        if (value.length() > 10000) {
            throw new IllegalArgumentException(
                    "Question or text cannot exceed 10,000 characters");
        }
    }

    private String formatTimeline(
            List<TimelineEventResponse> events) {

        if (events == null || events.isEmpty()) {
            return "No timeline events were documented in this period.";
        }

        return events.stream()
                .map(event -> {
                    String timestamp = event.getCreatedAt() != null
                            ? event.getCreatedAt().toString()
                            : "unknown time";

                    String type = event.getEventType() != null
                            ? event.getEventType().name()
                            : "UNKNOWN";

                    String author = event.getCreatedByName() != null
                            ? event.getCreatedByName()
                            : "unknown author";

                    String correction = event.getCorrectsEventId() != null
                            ? " [corrects event "
                            + event.getCorrectsEventId()
                            + "]"
                            : "";

                    return timestamp
                            + " | "
                            + type
                            + " | "
                            + author
                            + " | "
                            + event.getDescription()
                            + correction;
                })
                .collect(Collectors.joining("\n"));
    }
}

