package com.kfarms.reports;

import org.springframework.core.io.InputStreamSource;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    MonthlySummaryDto getMonthlySummary(LocalDate month);
    MonthlySummaryDto getRangeSummary(LocalDate startDate, LocalDate endDate);
    List<TrendPointDto> getTrends(String metric, LocalDate startDate, LocalDate endDate, String interval);
    ExportResponseMeta validateExportParams(String type, String category, LocalDate start, LocalDate end);
    InputStreamSource generateExport(String type, String category, LocalDate start, LocalDate end);
}
