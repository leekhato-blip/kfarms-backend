package com.kfarms.reports;

import java.util.Objects;
import java.util.function.Function;

public record ReportColumn<T>(String header, Function<T, ?> valueExtractor) {

    public ReportColumn {
        header = Objects.requireNonNull(header, "header");
        valueExtractor = Objects.requireNonNull(valueExtractor, "valueExtractor");
    }

    public static <T> ReportColumn<T> of(String header, Function<T, ?> valueExtractor) {
        return new ReportColumn<>(header, valueExtractor);
    }

    public Object valueFor(T row) {
        return valueExtractor.apply(row);
    }
}
