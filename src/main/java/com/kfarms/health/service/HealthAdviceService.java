package com.kfarms.health.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class HealthAdviceService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    private static final String OPENAI_URL =
            "https://api.openai.com/v1/responses";

    public List<String> generateAdvice(
            String ruleTitle,
            String contextNote,
            String livestockType,
            String season,
            String predictionTag
    ) {

        System.out.println(">>> HealthAdviceService.generateAdvice() CALLED");
        

        RestTemplate restTemplate = new RestTemplate();


        Map<String, Object> body = Map.of(
                "model", model,
                "input", """
                You are a farm health advisor.
        
                Alert: %s
                Livestock/Fish: %s
                Season: %s
                Prediction: %s
                Context: %s
        
                Give exactly 3 short, practical, actionable steps.
                Keep advice suitable for small to medium farms.
                Avoid technical jargon.
                """
                        .formatted(ruleTitle, livestockType, season, predictionTag, contextNote)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            Map response = restTemplate.postForObject(OPENAI_URL, request, Map.class);

            if (response == null || !response.containsKey("output")) {
                return fallbackAdvice();
            }

            List<?> output = (List<?>) response.get("output");

            for (Object o : output) {
                if (!(o instanceof Map<?, ?> msg)) continue;

                Object contentObj = msg.get("content");
                if (!(contentObj instanceof List<?> content)) continue;

                for (Object c : content) {
                    if (!(c instanceof Map<?, ?> block)) continue;

                    if ("output_text".equals(block.get("type"))) {
                        String text = block.get("text").toString();
                        return parseAdvice(text);
                    }
                }
            }

            return fallbackAdvice();

        } catch (Exception e) {
            e.printStackTrace();
            return fallbackAdvice();
        }


    }

    private List<String> parseAdvice(String content) {
        List<String> steps = new ArrayList<>();

        for (String line : content.split("\n")) {
            line = line.trim();
            if (!line.isEmpty()) {
                steps.add(line.replaceAll("^\\d+\\.\\s*", ""));
            }
        }

        return steps.isEmpty()
                ? fallbackAdvice()
                : new ArrayList<>(steps);
    }

    private List<String> fallbackAdvice() {
        return new ArrayList<>(List.of(
                "Ensure clean water is available",
                "Reduce stress on animals",
                "Monitor conditions closely"
        ));
    }
}
