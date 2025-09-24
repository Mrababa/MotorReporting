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

class QuoteDataLoaderCsvSeparatorDirectiveTest {

    @TempDir
    Path tempDir;

    @Test
    void skipsExcelSeparatorDirectiveRows() throws IOException {
        Path csvFile = tempDir.resolve("quotes_sep.csv");
        List<String> lines = List.of(
                "sep=,",
                "QuoteRequestedOn,Status,QuotationNo",
                "2024-01-05 08:15,Success,Q-123"
        );
        Files.write(csvFile, lines, StandardCharsets.UTF_8);

        List<QuoteRecord> records = QuoteDataLoader.load(csvFile);

        assertEquals(1, records.size(), "Expected one record after ignoring the separator directive");

        QuoteRecord record = records.get(0);
        Map<String, String> raw = record.getRawValues();
        assertTrue(raw.containsKey("QuoteRequestedOn"));
        assertEquals("2024-01-05 08:15", raw.get("QuoteRequestedOn"));
        assertEquals("Success", record.getStatus());
        assertEquals("Q-123", raw.get("QuotationNo"));
    }
}
