package com.kfarms.reports;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ReportValueFormatter {

    private static final DateTimeFormatter EXPORT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DecimalFormat MONEY_FORMAT =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    private ReportValueFormatter() {
    }

    public static String format(Object value, ReportValueType valueType) {
        if (value == null) {
            return "";
        }

        ReportValueType safeType = valueType == null ? ReportValueType.TEXT : valueType;

        return switch (safeType) {
            case DATE -> formatDate(value);
            case MONEY -> formatMoney(value);
            case TEXT -> String.valueOf(value);
        };
    }

    private static String formatDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return EXPORT_DATE_FORMAT.format(localDate);
        }
        if (value instanceof LocalDateTime localDateTime) {
            return EXPORT_DATE_FORMAT.format(localDateTime.toLocalDate());
        }
        return String.valueOf(value);
    }

    private static String formatMoney(Object value) {
        BigDecimal amount = toBigDecimal(value);
        if (amount == null) {
            return String.valueOf(value);
        }

        return MONEY_FORMAT.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }
}
