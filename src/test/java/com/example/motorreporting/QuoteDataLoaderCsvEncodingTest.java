package com.example.motorreporting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuoteDataLoaderCsvEncodingTest {

    @TempDir
    Path tempDir;

    @Test
    void loadSupportsUtf16EncodedCsvFiles() throws IOException {
        Path csvPath = tempDir.resolve("quotes_utf16.csv");
        List<String> lines = List.of(
                "QuoteNumber,Status,InsuranceType",
                "Q-123,Success,Comprehensive"
        );
        Files.write(csvPath, lines, StandardCharsets.UTF_16);

        List<QuoteRecord> records = QuoteDataLoader.load(csvPath);

        assertEquals(1, records.size(), "Expected a single record to be loaded");
        QuoteRecord record = records.get(0);
        assertEquals("Success", record.getStatus());
        assertEquals("Q-123", record.getQuoteNumber().orElseThrow());
        assertTrue(record.getRawValues().containsKey("QuoteNumber"));
    }

    @Test
    void loadSupportsUtf8CsvFilesWithBom() throws IOException {
        Path csvPath = tempDir.resolve("quotes_utf8_bom.csv");
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = (
                "QuoteNumber,Status\n" +
                        "Q-456,Failed\n"
        ).getBytes(StandardCharsets.UTF_8);
        try (OutputStream outputStream = Files.newOutputStream(csvPath)) {
            outputStream.write(bom);
            outputStream.write(content);
        }

        List<QuoteRecord> records = QuoteDataLoader.load(csvPath);

        assertEquals(1, records.size(), "Expected a single record to be loaded");
        QuoteRecord record = records.get(0);
        assertEquals("Failed", record.getStatus());
        assertEquals("Q-456", record.getQuoteNumber().orElseThrow());
        assertTrue(record.getRawValues().containsKey("QuoteNumber"));
    }
}

