package com.example.motorreporting;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuoteDataLoaderExcelSheetSelectionTest {

    @TempDir
    Path tempDir;

    @Test
    void readsSheetWithRecognizedColumnsRegardlessOfOrder() throws IOException {
        Path excelFile = tempDir.resolve("quotes.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet instructions = workbook.createSheet("Instructions");
            Row instructionsHeader = instructions.createRow(0);
            instructionsHeader.createCell(0).setCellValue("Notes");
            Row instructionsRow = instructions.createRow(1);
            instructionsRow.createCell(0).setCellValue("This sheet should be ignored");

            Sheet dataSheet = workbook.createSheet("Quotes");
            Row headerRow = dataSheet.createRow(0);
            String[] headers = {
                    "RandomColumn",
                    "QuotationNo",
                    "Status",
                    "InsuranceType",
                    "ErrorText",
                    "ChassisNumber"
            };
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            Row dataRow = dataSheet.createRow(1);
            dataRow.createCell(0).setCellValue("Extra information");
            dataRow.createCell(1).setCellValue("Q-12345");
            dataRow.createCell(2).setCellValue("fail");
            dataRow.createCell(3).setCellValue("Comprehensive");
            dataRow.createCell(4).setCellValue("Mismatch details");
            dataRow.createCell(5).setCellValue("JH4TB2H26CC000000");

            try (OutputStream outputStream = Files.newOutputStream(excelFile)) {
                workbook.write(outputStream);
            }
        }

        List<QuoteRecord> records = QuoteDataLoader.load(excelFile);
        assertEquals(1, records.size());

        QuoteRecord record = records.get(0);
        assertEquals("Failed", record.getStatus());
        assertEquals("Comprehensive", record.getInsuranceType());
        assertEquals("Q-12345", record.getQuoteNumber().orElseThrow());
        assertTrue(record.getChassisNumber().isPresent());
        assertEquals("Extra information", record.getRawValues().get("RandomColumn"));
        assertEquals("Mismatch details", record.getFailureReason());
    }
}

