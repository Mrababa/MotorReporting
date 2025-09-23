package com.example.motorreporting;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Persists cleaned quote data into the ready data directory for downstream consumption.
 */
public final class ReadyDataWriter {

    private static final String SOURCE_DATA_DIRECTORY = "source data";
    private static final String READY_DATA_DIRECTORY = "ready data";

    private ReadyDataWriter() {
    }

    /**
     * Writes the cleaned quote data to the ready data directory.
     *
     * @param originalInput the source file that was cleaned
     * @param records       the cleaned quote records to serialise
     * @return the path to the ready data file containing the cleaned records
     * @throws IOException if the ready data file cannot be written
     */
    public static Path writeCleanedData(Path originalInput, List<QuoteRecord> records) throws IOException {
        Objects.requireNonNull(originalInput, "originalInput");
        Objects.requireNonNull(records, "records");

        Path readyDirectory = Paths.get(SOURCE_DATA_DIRECTORY).resolve(READY_DATA_DIRECTORY);
        Files.createDirectories(readyDirectory);

        String fileName = determineReadyFileName(originalInput);
        Path readyDataPath = readyDirectory.resolve(fileName);

        if (records.isEmpty()) {
            try (BufferedWriter writer = Files.newBufferedWriter(readyDataPath, StandardCharsets.UTF_8)) {
                writer.write("");
            }
            return readyDataPath;
        }

        List<String> headers = determineHeaders(records);
        try (BufferedWriter writer = Files.newBufferedWriter(readyDataPath, StandardCharsets.UTF_8)) {
            writeRow(writer, headers);
            for (QuoteRecord record : records) {
                Map<String, String> values = record.getRawValues();
                List<String> row = new ArrayList<>(headers.size());
                for (String header : headers) {
                    row.add(values.getOrDefault(header, ""));
                }
                writeRow(writer, row);
            }
        }
        return readyDataPath;
    }

    private static List<String> determineHeaders(List<QuoteRecord> records) {
        SortedMap<String, String> headersByKey = new TreeMap<>();
        for (QuoteRecord record : records) {
            for (String header : record.getRawValues().keySet()) {
                if (header == null || header.isBlank()) {
                    continue;
                }
                String normalized = header.toLowerCase(Locale.ROOT);
                headersByKey.putIfAbsent(normalized, header);
            }
        }

        List<String> orderedHeaders = new ArrayList<>();
        addIfPresent(headersByKey, orderedHeaders, "status");
        addIfPresent(headersByKey, orderedHeaders, "quotationno");
        addIfPresent(headersByKey, orderedHeaders, "errortext");
        addIfPresent(headersByKey, orderedHeaders, "overrideisgccspec");

        for (String header : headersByKey.values()) {
            if (!orderedHeaders.contains(header)) {
                orderedHeaders.add(header);
            }
        }
        return orderedHeaders;
    }

    private static String determineReadyFileName(Path originalInput) {
        String rawName = originalInput.getFileName() != null
                ? originalInput.getFileName().toString()
                : "cleaned_data.csv";
        int extensionIndex = rawName.lastIndexOf('.');
        if (extensionIndex <= 0) {
            return rawName + ".csv";
        }
        String extension = rawName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
        if ("csv".equals(extension)) {
            return rawName;
        }
        return rawName.substring(0, extensionIndex) + ".csv";
    }

    private static void addIfPresent(SortedMap<String, String> headersByKey,
                                     List<String> orderedHeaders,
                                     String lookupKey) {
        String value = headersByKey.get(lookupKey);
        if (value != null) {
            orderedHeaders.add(value);
        }
    }

    private static void writeRow(BufferedWriter writer, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(escapeCsvValue(values.get(i)));
        }
        writer.newLine();
    }

    private static String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        boolean containsQuote = value.contains("\"");
        boolean needsQuoting = containsQuote
                || value.contains(",")
                || value.contains("\n")
                || value.contains("\r");
        String sanitized = containsQuote ? value.replace("\"", "\"\"") : value;
        if (needsQuoting) {
            return '"' + sanitized + '"';
        }
        return sanitized;
    }
}
