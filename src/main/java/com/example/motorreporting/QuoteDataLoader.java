package com.example.motorreporting;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Loads quote data from either Excel or CSV files.
 */
public final class QuoteDataLoader {

    private QuoteDataLoader() {
    }

    public static List<QuoteRecord> load(Path filePath) throws IOException {
        Objects.requireNonNull(filePath, "filePath");
        String fileName = filePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return loadFromExcel(filePath);
        }
        if (fileName.endsWith(".csv")) {
            return loadFromCsv(filePath);
        }
        throw new IllegalArgumentException("Unsupported file format: " + filePath);
    }

    private static List<QuoteRecord> loadFromExcel(Path filePath) throws IOException {
        List<QuoteRecord> records = new ArrayList<>();
        try (InputStream inputStream = Files.newInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return records;
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return records;
            }
            Map<Integer, String> headerIndex = extractHeaders(headerRow, formatter);

            int firstRow = sheet.getFirstRowNum() + 1;
            int lastRow = sheet.getLastRowNum();
            for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isRowEmpty(row, formatter)) {
                    continue;
                }
                Map<String, String> values = extractRowValues(row, headerIndex, formatter);
                records.add(QuoteRecord.fromValues(values));
            }
        } catch (Exception ex) {
            throw new IOException("Unable to read Excel file: " + filePath, ex);
        }
        return records;
    }

    private static Map<Integer, String> extractHeaders(Row headerRow, DataFormatter formatter) {
        Map<Integer, String> headerIndex = new HashMap<>();
        for (Cell cell : headerRow) {
            String header = formatter.formatCellValue(cell).trim();
            if (!header.isEmpty()) {
                headerIndex.put(cell.getColumnIndex(), header);
            }
        }
        return headerIndex;
    }

    private static boolean isRowEmpty(Row row, DataFormatter formatter) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, String> extractRowValues(Row row,
                                                        Map<Integer, String> headerIndex,
                                                        DataFormatter formatter) {
        Map<String, String> values = new HashMap<>();
        headerIndex.forEach((columnIndex, headerName) -> {
            Cell cell = row.getCell(columnIndex);
            String value = cell == null ? "" : formatter.formatCellValue(cell);
            values.put(headerName, value == null ? "" : value.trim());
        });
        return values;
    }

    private static List<QuoteRecord> loadFromCsv(Path filePath) throws IOException {
        List<QuoteRecord> records = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(filePath);
             CSVReader csvReader = new CSVReaderBuilder(reader)
                     .withCSVParser(new CSVParserBuilder().withSeparator(',').build())
                     .build()) {
            String[] headers = csvReader.readNext();
            if (headers == null) {
                return records;
            }
            Map<Integer, String> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                if (headers[i] != null && !headers[i].trim().isEmpty()) {
                    headerIndex.put(i, headers[i].trim());
                }
            }

            String[] row;
            while ((row = csvReader.readNext()) != null) {
                if (isRowEmpty(row)) {
                    continue;
                }
                Map<String, String> values = new HashMap<>();
                for (Map.Entry<Integer, String> entry : headerIndex.entrySet()) {
                    int index = entry.getKey();
                    String header = entry.getValue();
                    String value = index < row.length && row[index] != null ? row[index].trim() : "";
                    values.put(header, value);
                }
                records.add(QuoteRecord.fromValues(values));
            }
        } catch (Exception ex) {
            throw new IOException("Unable to read CSV file: " + filePath, ex);
        }
        return records;
    }

    private static boolean isRowEmpty(String[] row) {
        if (row == null) {
            return true;
        }
        for (String value : row) {
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
