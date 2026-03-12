package com.kfarms.health.service;

import com.kfarms.health.dto.AdviceContext;
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

    @Value("${kfarms.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    private static final String OPENAI_URL =
            "https://api.openai.com/v1/responses";

    public List<String> generateAdvice(AdviceContext context) {
        if(!aiEnabled){
            return ruleBasedAdvice(context);
        }

        try {
            return aiAdvice(context);
        }catch (Exception e) {
            return ruleBasedAdvice(context);
        }
    }

    private List<String> aiAdvice(AdviceContext context) {
        RestTemplate restTemplate = new RestTemplate();


        Map<String, Object> body = Map.of(
                "model", model,
                "input", """
                You are a farm health advisor.
        
                Alert: %s
                Livestock/Fish: %s
                Season: %s
                Context: %s
        
                Give exactly 3 short, practical, actionable steps.
                Keep advice suitable for small to medium farms.
                Avoid technical jargon.
                """
                        .formatted(context.getRuleTitle(), context.getLivestockType(), context.getSeason(), context.getContextNote())
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

    private List<String> ruleBasedAdvice(AdviceContext context) {
        return switch (context.getRuleCode()) {

            // ================= WEATHER – POULTRY =================

            case "HEAT_STRESS_POULTRY" -> List.of(
                    "Increase access to clean, cool drinking water",
                    "Improve airflow using fans or open ventilation",
                    "Avoid handling birds during hot hours"
            );

            case "EXTREME_HEAT_POULTRY" -> List.of(
                    "Sprinkle cool water around housing to reduce heat",
                    "Provide emergency shade and strong ventilation",
                    "Check birds frequently for signs of collapse"
            );

            case "COLD_STRESS_POULTRY" -> List.of(
                    "Reduce drafts and close open gaps in housing",
                    "Provide dry bedding and additional warmth",
                    "Increase feed slightly to help birds generate heat"
            );

            case "HIGH_HUMIDITY_POULTRY" -> List.of(
                    "Improve ventilation to reduce moisture buildup",
                    "Replace wet litter immediately",
                    "Clean drinkers to prevent bacterial growth"
            );

            // ================= WATER / FISH =================

            case "LOW_OXYGEN_FISH" -> List.of(
                    "Increase aeration immediately if available",
                    "Stop feeding until oxygen levels improve",
                    "Remove dead or weak fish promptly"
            );

            case "HIGH_WATER_TEMP_FISH" -> List.of(
                    "Reduce feeding to lower oxygen demand",
                    "Add fresh water gradually if possible",
                    "Provide shade over ponds or tanks"
            );

            case "DIRTY_WATER_FISH" -> List.of(
                    "Change part of the water carefully",
                    "Remove leftover feed and waste",
                    "Reduce feeding until water clears"
            );

            // ================= HEALTH / DISEASE =================

            case "UNUSUAL_MORTALITY_POULTRY" -> List.of(
                    "Isolate affected birds immediately",
                    "Disinfect housing, feeders, and drinkers",
                    "Observe remaining birds closely for symptoms"
            );

            case "UNUSUAL_MORTALITY_FISH" -> List.of(
                    "Check water quality and oxygen levels",
                    "Remove dead fish immediately",
                    "Reduce feeding until the cause is identified"
            );

            // ================= FALLBACK =================

            default -> List.of(
                    "Ensure clean water is available",
                    "Reduce stress on animals",
                    "Monitor conditions closely"
            );
        };
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
