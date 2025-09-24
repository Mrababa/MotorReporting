package com.example.motorreporting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuoteDataLoaderCsvDelimiterDetectionTest {

    @TempDir
    Path tempDir;

    @Test
    void detectsSemicolonSeparatedFiles() throws IOException {
        Path csvFile = tempDir.resolve("quotes_semicolon.csv");
        List<String> lines = List.of(
                "QuoteRequestedOn;Status;QuotationNo;EstimatedValue",
                "2024-01-05 08:15;Success;Q-123;1500",
                "2024-01-06 10:30;Failed;Q-456;2000"
        );
        Files.write(csvFile, lines, StandardCharsets.UTF_8);

        List<QuoteRecord> records = QuoteDataLoader.load(csvFile);

        assertEquals(2, records.size(), "Expected to load records from a semicolon separated CSV");

        QuoteRecord first = records.get(0);
        Map<String, String> raw = first.getRawValues();

        assertTrue(raw.containsKey("QuoteRequestedOn"));
        assertEquals("2024-01-05 08:15", raw.get("QuoteRequestedOn"));
        assertEquals("Success", first.getStatus());
        assertEquals("Q-123", raw.get("QuotationNo"));
        assertEquals("1500", raw.get("EstimatedValue"));
    }
}

