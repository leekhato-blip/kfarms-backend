package com.kfarms.reports;



import com.kfarms.entity.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // --------------------------------------
    // Monthly Summary
    // --------------------------------------
    @GetMapping("/summary/monthly")
    public ResponseEntity<ApiResponse<MonthlySummaryDto>> getMonthlyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month
    ){
        MonthlySummaryDto summary = reportService.getMonthlySummary(month);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Monthly summary fetched", summary)
        );
    }

    // --------------------------------------
    // Range Summary
    // --------------------------------------
    @GetMapping("/summary/range")
    public ResponseEntity<ApiResponse<MonthlySummaryDto>> getRangeSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ){
        MonthlySummaryDto summary = reportService.getRangeSummary(startDate, endDate);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Range summary fetched", summary)
        );
    }


    // --------------------------------------
    // Trend Data
    // --------------------------------------
    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<List<TrendPointDto>>> getTrends(
            @RequestParam(required = false) String metric,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "daily") String interval
    ) {
        List<TrendPointDto> data = reportService.getTrends(metric, startDate, endDate, interval);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Trend data fetched", data)
        );
    }

    // --------------------------------------
    // Data Export
    // --------------------------------------
    @GetMapping("/export")
    public ResponseEntity<InputStreamSource> exportData(
            @RequestParam String type,
            @RequestParam String category,
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end
    ){
     ExportResponseMeta meta = reportService.validateExportParams(type, category, start, end);
     InputStreamSource resource = reportService.generateExport(type, category, start, end);

     return ResponseEntity.ok()
             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + meta.getFilename())
             .contentType(MediaType.parseMediaType(meta.getContentType()))
             .body(resource);
    }

}
