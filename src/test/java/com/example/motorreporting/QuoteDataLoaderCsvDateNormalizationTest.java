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

class QuoteDataLoaderCsvDateNormalizationTest {

    @TempDir
    Path tempDir;

    @Test
    void normalizesDateColumnsWithoutTouchingOthers() throws IOException {
        Path csvFile = tempDir.resolve("quotes.csv");
        List<String> lines = List.of(
                "Name,CreatedDate,Start_Date,Amount",
                "Alice,2023-03-01T14:30:00Z,15/11/2023,100",
                "Bob,00:00.0,45205,200"
        );
        Files.write(csvFile, lines, StandardCharsets.UTF_8);

        List<QuoteRecord> records = QuoteDataLoader.load(csvFile);
        assertEquals(2, records.size());

        Map<String, String> firstRow = records.get(0).getRawValues();
        assertEquals("2023-03-01 14:30:00", firstRow.get("CreatedDate"));
        assertEquals("2023-11-15 00:00:00", firstRow.get("Start_Date"));
        assertEquals("Alice", firstRow.get("Name"));
        assertEquals("100", firstRow.get("Amount"));

        Map<String, String> secondRow = records.get(1).getRawValues();
        assertEquals("", secondRow.get("CreatedDate"));
        assertEquals("2023-10-06 00:00:00", secondRow.get("Start_Date"));
        assertEquals("Bob", secondRow.get("Name"));
        assertEquals("200", secondRow.get("Amount"));

        assertTrue(secondRow.containsKey("Status"));
    }
}
