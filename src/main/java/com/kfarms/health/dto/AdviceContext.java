package com.kfarms.health.dto;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Data
@RequiredArgsConstructor
public class AdviceContext {

    private String ruleCode;
    private String ruleTitle;
    private String livestockType;
    private String season;
    private String contextNote;
}
