package com.kfarms.health.service;

import com.kfarms.health.dto.WeatherData;
import com.sun.tools.javac.Main;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class WeatherService {

    private static final String API_KEY = "746e1758d3b1d01f217e28e69ab2b5ab";
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather";


    public WeatherData getCurrentWeather(String city) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(API_URL)
                .queryParam("q", city)
                .queryParam("appid", API_KEY)
                .queryParam("units", "metric"); // Celsius

        RestTemplate restTemplate = new RestTemplate();
        OpenWeatherResponse response = restTemplate.getForObject(uriBuilder.toUriString(), OpenWeatherResponse.class);

        WeatherData data = new WeatherData();
        if (response != null) {
            data.setTemp(response.getMain().getTemp());
            data.setHumidity(response.getMain().getHumidity());
            data.setWindSpeed(response.getWind().getSpeed());
            data.setDescription(response.getWeather()[0].getDescription());
        }
        return data;
    }

    // Inner classes to map OpenWeather JSON
    @Data
    static class OpenWeatherResponse {
        private Main main;
        private Wind wind;
        private Weather[] weather;


        @Data
        static class Main {
            private double temp;
            private int humidity;
        }

        @Data
        static class Wind {
            private double speed;
        }

        @Data
        static class Weather {
            private String description;
        }
    }
}

