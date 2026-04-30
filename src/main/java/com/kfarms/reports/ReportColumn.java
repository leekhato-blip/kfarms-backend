package com.kfarms.reports;

import java.util.Objects;
import java.util.function.Function;

public record ReportColumn<T>(
        String header,
        Function<T, ?> valueExtractor,
        ReportValueType valueType
) {

    public ReportColumn {
        header = Objects.requireNonNull(header, "header");
        valueExtractor = Objects.requireNonNull(valueExtractor, "valueExtractor");
        valueType = valueType == null ? ReportValueType.TEXT : valueType;
    }

    public static <T> ReportColumn<T> of(String header, Function<T, ?> valueExtractor) {
        return new ReportColumn<>(header, valueExtractor, ReportValueType.TEXT);
    }

    public static <T> ReportColumn<T> of(
            String header,
            Function<T, ?> valueExtractor,
            ReportValueType valueType
    ) {
        return new ReportColumn<>(header, valueExtractor, valueType);
    }

    public Object valueFor(T row) {
        return valueExtractor.apply(row);
    }

    public String formattedValueFor(T row) {
        return ReportValueFormatter.format(valueFor(row), valueType);
    }
}
