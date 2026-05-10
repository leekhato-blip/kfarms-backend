package com.kfarms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class SupportAssistantChatRequestDto {

    @NotBlank(message = "Message is required")
    private String message;

    private Map<String, Object> context = new LinkedHashMap<>();

    private List<SupportAssistantHistoryItemDto> history = new ArrayList<>();
}
