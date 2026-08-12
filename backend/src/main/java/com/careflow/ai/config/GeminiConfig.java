package com.careflow.ai.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class GeminiConfig {

    @Bean
    public GeminiClient geminiClient(
            @Value("${careflow.gemini.api-key}") String apiKey,
            @Value("${careflow.gemini.model:gemini-1.5-flash}") String model) {

        return new GeminiClient(apiKey, model);
    }

    @Bean
    public Cloudinary cloudinary(
            @Value("${careflow.cloudinary.cloud-name}") String cloudName,
            @Value("${careflow.cloudinary.api-key}") String apiKey,
            @Value("${careflow.cloudinary.api-secret}") String apiSecret) {

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        return new Cloudinary(config);
    }
}