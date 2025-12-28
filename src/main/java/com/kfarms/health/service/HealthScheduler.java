package com.kfarms.health.service;

import com.kfarms.health.dto.WeatherData;
import com.kfarms.health.enums.FarmSeason;
import com.kfarms.repository.FishPondRepository;
import com.kfarms.repository.LivestockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class HealthScheduler {

    private final WeatherService weatherService;
    private final HealthService healthService;

    private final FishPondRepository fishPondRepo;
    private final LivestockRepository livestockRepo;

    private final Map<String, Double> heatThresholds = Map.of(
            "HEAT_STRESS_POULTRY", 35.0
    );

    private final double predictionMargin = 2.0;
    private final String farmCity = "Abuja";

    @Scheduled(fixedRateString = "${kfarms.health.scheduler.rate:1800000}")
    public void runHealthChecks() {

        System.out.println(">>> HealthScheduler running <<<");

        FarmSeason season = detectSeason(LocalDate.now());
        WeatherData weather;

        try {
            weather = weatherService.getCurrentWeather(farmCity);
        } catch (Exception e) {
            weather = offlineFallbackWeather(season);
        }

        double temp = weather.getTemp();
        int humidity = weather.getHumidity();

        for (var entry : heatThresholds.entrySet()) {
            if (temp >= entry.getValue() - predictionMargin) {

                String context = String.format(
                        "Temp: %.1f°C | Season: %s | Humidity: %d%%",
                        temp, season, humidity
                );

                System.out.println("About to call triggerRuleByCode " + context);
                healthService.triggerRuleByCode(
                        entry.getKey(),
                        context,
                        season.name()
                );
            }
        }

        checkFishMortality();
        checkLivestockMortality();
    }

    private FarmSeason detectSeason(LocalDate date) {
        Month m = date.getMonth();
        int month = m.getValue();

        if (month == 11 || month == 12 || month <= 2)
            return FarmSeason.HARMATTAN;

        if (month >= 4 && month <= 10)
            return FarmSeason.RAINY;

        return FarmSeason.DRY;
    }

    private WeatherData offlineFallbackWeather(FarmSeason season) {
        WeatherData w = new WeatherData();
        w.setTemp(32);
        w.setHumidity(40);
        return w;
    }

    private void checkFishMortality() {

        fishPondRepo.findAll().forEach(pond -> {

            if (pond.getMortalityCount() == null || pond.getMortalityCount() == 0)
                return;

            int total =
                    pond.getCurrentStock() + pond.getMortalityCount();

            if (total == 0) return;

            double rate =
                    (double) pond.getMortalityCount() / total;

            if (rate >= 0.05) { // 5% threshold
                String context = String.format(
                        "Pond: %s | Mortality: %d/%d (%.1f%%)",
                        pond.getPondName(),
                        pond.getMortalityCount(),
                        total,
                        rate * 100
                );

                healthService.triggerRuleByCode(
                        "UNUSUAL_MORTALITY_FISH",
                        context,
                        detectSeason(LocalDate.now()).name()
                );
            }
        });
    }
    private void checkLivestockMortality() {

        livestockRepo.findAll().forEach(batch -> {

            if (batch.getMortality() == null || batch.getMortality() == 0)
                return;

            int total =
                    batch.getCurrentStock() + batch.getMortality();

            if (total == 0) return;

            double rate =
                    (double) batch.getMortality() / total;

            if (rate >= 0.03) { // 3% batch mortality
                String context = String.format(
                        "Batch: %s | Type: %s | Mortality: %d/%d (%.1f%%)",
                        batch.getBatchName(),
                        batch.getType(),
                        batch.getMortality(),
                        total,
                        rate * 100
                );

                healthService.triggerRuleByCode(
                        "UNUSUAL_MORTALITY_POULTRY",
                        context,
                        detectSeason(LocalDate.now()).name()
                );
            }
        });
    }


}
