package com.kfarms.health.dto;

import lombok.Data;

@Data
public class WeatherData {
    private double temp;
    private int humidity;
    private double windSpeed;
    private String description;
}
