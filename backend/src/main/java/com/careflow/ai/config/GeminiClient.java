package com.careflow.ai.config;

import org.springframework.web.client.RestClient;

import java.util.Map;

public class GeminiClient {

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private final String apiKey;
    private final String model;
    private final RestClient restClient;

    public GeminiClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(API_URL + model + ":generateContent")
                .build();
    }

    public String generate(String prompt) {

        if (apiKey == null
                || apiKey.isBlank()
                || apiKey.equals("placeholder-gemini-api-key")) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not configured");
        }

        Map<String, Object> body = Map.of(
                "contents", new Object[] {
                        Map.of(
                                "parts", new Object[] {
                                        Map.of("text", prompt)
                                }
                        )
                }
        );

        Map<?, ?> response = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("key", apiKey)
                        .build())
                .body(body)
                .retrieve()
                .body(Map.class);

        return extractText(response);
    }

    private String extractText(Map<?, ?> response) {

        if (response == null) {
            throw new IllegalStateException(
                    "Gemini returned an empty response");
        }

        Object candidatesObject = response.get("candidates");

        if (!(candidatesObject instanceof java.util.List<?> candidates)
                || candidates.isEmpty()) {
            throw new IllegalStateException(
                    "Gemini returned no candidates");
        }

        Object firstCandidate = candidates.get(0);

        if (!(firstCandidate instanceof Map<?, ?> candidate)) {
            throw new IllegalStateException(
                    "Invalid Gemini response");
        }

        Object contentObject = candidate.get("content");

        if (!(contentObject instanceof Map<?, ?> content)) {
            throw new IllegalStateException(
                    "Gemini response has no content");
        }

        Object partsObject = content.get("parts");

        if (!(partsObject instanceof java.util.List<?> parts)
                || parts.isEmpty()) {
            throw new IllegalStateException(
                    "Gemini response has no parts");
        }

        Object firstPart = parts.get(0);

        if (!(firstPart instanceof Map<?, ?> part)) {
            throw new IllegalStateException(
                    "Invalid Gemini response part");
        }

        Object text = part.get("text");

        if (text == null) {
            throw new IllegalStateException(
                    "Gemini response contains no text");
        }

        return text.toString();
    }
}
