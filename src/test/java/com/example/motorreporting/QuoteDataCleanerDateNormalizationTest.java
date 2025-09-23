package com.example.motorreporting;

import com.opencsv.CSVReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class QuoteDataCleanerDateNormalizationTest {

    @TempDir
    Path tempDir;

    @Test
    void writesDateColumnsInConsistentFormat() throws IOException {
        Map<String, String> values = new HashMap<>();
        values.put("Start_Date", "03-21-2024");
        values.put("CreatedDate", "2024/03/22 5:45");
        values.put("Name", "Charlie");
        values.put("Status", "");
        values.put("QuotationNo", "");
        values.put("ErrorText", "Null");

        QuoteRecord record = QuoteRecord.fromValues(values);

        Path cleanedFile = QuoteDataCleaner.writeCleanFile(tempDir, List.of(record));

        try (Reader reader = Files.newBufferedReader(cleanedFile, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {
            String[] header = csvReader.readNext();
            String[] row = csvReader.readNext();
            assertNotNull(header);
            assertNotNull(row);

            Map<String, Integer> indexByHeader = new HashMap<>();
            for (int i = 0; i < header.length; i++) {
                indexByHeader.put(header[i], i);
            }

            assertEquals("2024-03-21 00:00:00", row[indexByHeader.get("Start_Date")]);
            assertEquals("2024-03-22 05:45:00", row[indexByHeader.get("CreatedDate")]);
            assertEquals("Charlie", row[indexByHeader.get("Name")]);
        }
    }
}
