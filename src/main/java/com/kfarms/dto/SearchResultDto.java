package com.kfarms.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchResultDto {
    private Object id;          //"string|number"
    private String title;       // friendly label
    private String subtitle;    // nullable
    private String url;         // frontend route
}
