package com.kfarms.reports;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Data
@RequiredArgsConstructor
public class ReportRangeRequest {
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
}
