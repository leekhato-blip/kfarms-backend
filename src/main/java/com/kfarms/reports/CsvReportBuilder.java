package com.kfarms.reports;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CsvReportBuilder {

    private CsvReportBuilder() {
    }

    public static <T> InputStream buildReport(List<T> dataList, List<ReportColumn<T>> columns) {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("Columns cannot be null or empty");
        }

        StringBuilder sb = new StringBuilder();
        appendRow(sb, columns.stream().map(ReportColumn::header).toArray());

        if (dataList != null) {
            for (T item : dataList) {
                appendRow(
                        sb,
                        columns.stream()
                                .map(column -> column.valueFor(item))
                                .toArray()
                );
            }
        }

        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void appendRow(StringBuilder sb, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                sb.append(',');
            }
            sb.append(csvValue(values[index]));
        }
        sb.append('\n');
    }

    private static String csvValue(Object value) {
        if (value == null) {
            return "";
        }

        String text = String.valueOf(value).replace("\"", "\"\"");
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return '"' + text + '"';
        }
        return text;
    }
}
