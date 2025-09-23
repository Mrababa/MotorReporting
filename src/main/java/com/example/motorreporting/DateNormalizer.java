package com.example.motorreporting;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalizes date-like values into a consistent format.
 */
final class DateNormalizer {

    private static final Pattern TIME_ONLY_PATTERN = Pattern.compile("^\\d{1,2}:\\d{2}(:\\d{2})?(\\.\\d+)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXCEL_SERIAL_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withLocale(Locale.ROOT);
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 31);
    private static final double SECONDS_PER_DAY = 24d * 60d * 60d;

    private static final List<DateTimeFormatter> DATE_FORMATTERS = buildDateFormatters();
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = buildDateTimeFormatters();

    private DateNormalizer() {
    }

    static boolean isDateColumn(String header) {
        return header != null && header.toLowerCase(Locale.ROOT).contains("date");
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (TIME_ONLY_PATTERN.matcher(trimmed).matches()) {
            return "";
        }

        LocalDateTime dateTime = parseDateTime(trimmed);
        if (dateTime != null) {
            return OUTPUT_FORMAT.format(dateTime);
        }

        LocalDate date = parseDate(trimmed);
        if (date != null) {
            return OUTPUT_FORMAT.format(date.atStartOfDay());
        }

        dateTime = parseExcelSerial(trimmed);
        if (dateTime != null) {
            return OUTPUT_FORMAT.format(dateTime);
        }

        return "";
    }

    private static LocalDateTime parseDateTime(String value) {
        try {
            Instant instant = Instant.parse(adjustIsoString(value));
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        } catch (DateTimeParseException ex) {
            // ignore
        }

        try {
            return LocalDateTime.ofInstant(java.time.OffsetDateTime.parse(value).toInstant(), ZoneOffset.UTC);
        } catch (DateTimeParseException ex) {
            // ignore
        }

        try {
            return LocalDateTime.ofInstant(java.time.ZonedDateTime.parse(value).toInstant(), ZoneOffset.UTC);
        } catch (DateTimeParseException ex) {
            // ignore
        }

        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ex) {
                // try next
            }
        }
        return null;
    }

    private static String adjustIsoString(String value) {
        if (value.indexOf(' ') > 0 && value.endsWith("Z")) {
            return value.replace(' ', 'T');
        }
        return value;
    }

    private static LocalDate parseDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ex) {
                // try next
            }
        }
        return null;
    }

    private static LocalDateTime parseExcelSerial(String value) {
        if (!EXCEL_SERIAL_PATTERN.matcher(value).matches()) {
            return null;
        }
        double numericValue;
        try {
            numericValue = Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
        if (!Double.isFinite(numericValue) || numericValue < 1) {
            return null;
        }
        long wholeDays = (long) Math.floor(numericValue);
        double fraction = numericValue - wholeDays;
        if (wholeDays >= 60) {
            wholeDays -= 1;
        }
        LocalDate date = EXCEL_EPOCH.plusDays(wholeDays);
        long seconds = Math.round(fraction * SECONDS_PER_DAY);
        return date.atStartOfDay().plusSeconds(seconds);
    }

    private static List<DateTimeFormatter> buildDateFormatters() {
        List<DateTimeFormatter> formatters = new ArrayList<>();
        formatters.add(DateTimeFormatter.ISO_LOCAL_DATE);
        formatters.add(DateTimeFormatter.BASIC_ISO_DATE);
        String[] patterns = {
                "uuuu-M-d",
                "uuuu/MM/dd",
                "uuuu/M/d",
                "uuuu.MM.dd",
                "uuuu.M.d",
                "d/M/uuuu",
                "dd/MM/uuuu",
                "M/d/uuuu",
                "MM/dd/uuuu",
                "d-M-uuuu",
                "dd-MM-uuuu",
                "M-d-uuuu",
                "MM-dd-uuuu",
                "d.M.uuuu",
                "dd.MM.uuuu",
                "M.d.uuuu",
                "MM.dd.uuuu"
        };
        for (String pattern : patterns) {
            formatters.add(createFormatter(pattern));
        }
        return formatters;
    }

    private static List<DateTimeFormatter> buildDateTimeFormatters() {
        List<DateTimeFormatter> formatters = new ArrayList<>();
        formatters.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        formatters.add(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        formatters.add(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        String[] datePatterns = {
                "uuuu-MM-dd",
                "uuuu-M-d",
                "uuuu/MM/dd",
                "uuuu/M/d",
                "uuuu.MM.dd",
                "uuuu.M.d",
                "dd/MM/uuuu",
                "d/M/uuuu",
                "MM/dd/uuuu",
                "M/d/uuuu",
                "dd-MM-uuuu",
                "d-M-uuuu",
                "MM-dd-uuuu",
                "M-d-uuuu",
                "dd.MM.uuuu",
                "d.M.uuuu",
                "MM.dd.uuuu",
                "M.d.uuuu"
        };
        char[] separators = {' ', 'T'};
        for (String pattern : datePatterns) {
            for (char separator : separators) {
                formatters.add(buildDateTimeFormatter(pattern, separator));
            }
        }
        return formatters;
    }

    private static DateTimeFormatter createFormatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ROOT)
                .withResolverStyle(ResolverStyle.SMART);
    }

    private static DateTimeFormatter buildDateTimeFormatter(String datePattern, char separator) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(datePattern)
                .appendLiteral(separator)
                .appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2);
        builder.optionalStart()
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2);
        builder.optionalStart()
                .appendLiteral('.')
                .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false);
        builder.optionalEnd();
        builder.optionalEnd();
        return builder.toFormatter(Locale.ROOT).withResolverStyle(ResolverStyle.SMART);
    }
}
