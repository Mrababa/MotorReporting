package com.example.motorreporting;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class QuoteDataCleanerHeaderNormalizationTest {

    @TempDir
    Path tempDir;

    @Test
    void writesValuesForHeadersWithSpacesOrUnderscores() throws IOException, CsvValidationException {
        Map<String, String> values = new HashMap<>();
        values.put("Quote Requested On", "2024-02-02 12:15");
        values.put("Status", "success");
        values.put("Insurance Purpose", "Personal");
        values.put("Shory_Make_En", "MakeA");
        values.put("Shory Model En", "ModelX");
        values.put("License Issue Date", "2024-03-03");
        values.put("Chassis_Number", "ABC123456789");
        values.put("InsuranceType", "TPL");
        values.put("QuotationNo", "Q-123");
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

            assertFalse(indexByHeader.containsKey("Quote Requested On"));
            assertEquals("2024-02-02 12:15", row[indexByHeader.get("QuoteRequestedOn")]);
            assertEquals("Success", row[indexByHeader.get("Status")]);
            assertEquals("Personal", row[indexByHeader.get("InsurancePurpose")]);
            assertEquals("MakeA", row[indexByHeader.get("ShoryMakeEn")]);
            assertEquals("ModelX", row[indexByHeader.get("ShoryModelEn")]);
            assertEquals("2024-03-03 00:00:00", row[indexByHeader.get("LicenseIssueDate")]);
            assertEquals("ABC123456789", row[indexByHeader.get("ChassisNumber")]);
        }
    }
}

