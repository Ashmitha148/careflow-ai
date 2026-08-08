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
}
