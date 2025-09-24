package com.example.motorreporting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents an optional QuoteRequestedOn filter range.
 */
public final class ReportDateRange {

    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Optional<LocalDate> startDate;
    private final Optional<LocalDate> endDate;

    private ReportDateRange(Optional<LocalDate> startDate, Optional<LocalDate> endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static ReportDateRange fromEnvironment() {
        Optional<LocalDate> start = parseDate(resolveConfiguredValue("report.startDate", "REPORT_START_DATE"));
        Optional<LocalDate> end = parseDate(resolveConfiguredValue("report.endDate", "REPORT_END_DATE"));
        if (start.isPresent() && end.isPresent() && end.get().isBefore(start.get())) {
            throw new IllegalArgumentException("Configured report end date must be on or after the start date.");
        }
        return new ReportDateRange(start, end);
    }

    public static ReportDateRange of(Optional<LocalDate> start, Optional<LocalDate> end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (start.isPresent() && end.isPresent() && end.get().isBefore(start.get())) {
            throw new IllegalArgumentException("End date must not be before start date.");
        }
        return new ReportDateRange(start, end);
    }

    public List<QuoteRecord> filter(List<QuoteRecord> records) {
        Objects.requireNonNull(records, "records");
        if (!hasSelection()) {
            return records;
        }
        return records.stream()
                .filter(record -> record.getQuoteRequestedOn()
                        .filter(this::isWithinRange)
                        .isPresent())
                .collect(Collectors.toList());
    }

    public Optional<LocalDate> getStartDate() {
        return startDate;
    }

    public Optional<LocalDate> getEndDate() {
        return endDate;
    }

    public boolean hasSelection() {
        return startDate.isPresent() || endDate.isPresent();
    }

    private boolean isWithinRange(LocalDateTime value) {
        LocalDate date = value.toLocalDate();
        if (startDate.isPresent() && date.isBefore(startDate.get())) {
            return false;
        }
        if (endDate.isPresent() && date.isAfter(endDate.get())) {
            return false;
        }
        return true;
    }

    private static Optional<LocalDate> parseDate(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(trimmed, INPUT_FORMATTER));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Invalid report date '" + value + "'. Expected format yyyy-MM-dd.", ex);
        }
    }

    private static String resolveConfiguredValue(String propertyKey, String environmentKey) {
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }
        String envValue = System.getenv(environmentKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return null;
    }
}
