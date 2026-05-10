package com.kfarms.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class TrendPointDto {
    private LocalDate date;
    private BigDecimal value;
}
