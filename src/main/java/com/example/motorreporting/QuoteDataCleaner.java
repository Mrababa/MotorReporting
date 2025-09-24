package com.example.motorreporting;

import com.opencsv.CSVWriter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Writes a cleaned representation of the input dataset.
 */
public final class QuoteDataCleaner {

    private static final List<String> DEFAULT_COLUMN_ORDER = List.of(
            "QuoteRequestedOn",
            "Status",
            "ReferenceNumber",
            "InsurancePurpose",
            "ICName",
            "ShoryMakeEn",
            "ShoryModelEn",
            "OverrideIsGccSpec",
            "Age",
            "LicenseIssueDate",
            "BodyCategory",
            "ChassisNumber",
            "InsuranceType",
            "ManufactureYear",
            "RegistrationDate",
            "QuotationNo",
            "EstimatedValue",
            "InsuranceExpiryDate",
            "ErrorText"
    );

    private QuoteDataCleaner() {
    }

    public static Path writeCleanFile(Path outputDirectory, List<QuoteRecord> records) throws IOException {
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Objects.requireNonNull(records, "records");

        Path resolvedDirectory = outputDirectory.toAbsolutePath();
        Files.createDirectories(resolvedDirectory);

        Path cleanedFile = resolvedDirectory.resolve("quote_generation_cleaned.csv");
        List<String> headers = determineHeaders(records);

        try (Writer writer = Files.newBufferedWriter(cleanedFile, StandardCharsets.UTF_8);
             CSVWriter csvWriter = new CSVWriter(writer,
                     CSVWriter.DEFAULT_SEPARATOR,
                     CSVWriter.DEFAULT_QUOTE_CHARACTER,
                     CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                     CSVWriter.DEFAULT_LINE_END)) {
            csvWriter.writeNext(headers.toArray(new String[0]), false);
            for (QuoteRecord record : records) {
                String[] row = new String[headers.size()];
                Map<String, String> values = record.getRawValues();
                for (int i = 0; i < headers.size(); i++) {
                    row[i] = findValue(values, headers.get(i));
                }
                csvWriter.writeNext(row, false);
            }
        }

        return cleanedFile;
    }

    private static List<String> determineHeaders(List<QuoteRecord> records) {
        Map<String, String> headersByNormalizedKey = new LinkedHashMap<>();
        for (String defaultHeader : DEFAULT_COLUMN_ORDER) {
            if (defaultHeader == null || defaultHeader.isEmpty()) {
                continue;
            }
            headersByNormalizedKey.putIfAbsent(normalizeHeaderKey(defaultHeader), defaultHeader);
        }
        for (QuoteRecord record : records) {
            for (String header : record.getRawValues().keySet()) {
                if (header == null || header.isEmpty()) {
                    continue;
                }
                headersByNormalizedKey.putIfAbsent(normalizeHeaderKey(header), header);
            }
        }
        return new ArrayList<>(headersByNormalizedKey.values());
    }

    private static String findValue(Map<String, String> values, String header) {
        if (header == null || header.isEmpty()) {
            return "";
        }
        String normalizedHeader = normalizeHeaderKey(header);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            if (normalizeHeaderKey(key).equals(normalizedHeader)) {
                String value = entry.getValue();
                if (DateNormalizer.isDateColumn(header)) {
                    return DateNormalizer.normalize(value);
                }
                return value == null ? "" : value;
            }
        }
        return "";
    }

    private static String normalizeHeaderKey(String header) {
        if (header == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(header.length());
        for (int i = 0; i < header.length(); i++) {
            char ch = header.charAt(i);
            if (Character.isWhitespace(ch) || ch == '_') {
                continue;
            }
            builder.append(Character.toLowerCase(ch));
        }
        return builder.toString();
    }
}
