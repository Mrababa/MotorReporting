package com.example.motorreporting;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import java.util.Set;

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
            Sheet sheet = selectRelevantSheet(workbook);
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

    private static Sheet selectRelevantSheet(Workbook workbook) {
        if (workbook == null || workbook.getNumberOfSheets() == 0) {
            return null;
        }

        List<String> preferredNames = List.of("Source", "Soure", "Source Data");
        for (String name : preferredNames) {
            Sheet sheet = getSheetIgnoreCase(workbook, name);
            if (sheet != null) {
                return sheet;
            }
        }

        return workbook.getSheetAt(0);
    }

    private static Sheet getSheetIgnoreCase(Workbook workbook, String sheetName) {
        if (sheetName == null) {
            return null;
        }
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet candidate = workbook.getSheetAt(i);
            if (candidate != null) {
                String currentName = candidate.getSheetName();
                if (currentName != null && currentName.trim().equalsIgnoreCase(sheetName)) {
                    return candidate;
                }
            }
        }
        return null;
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
        List<Charset> candidateCharsets = buildCandidateCharsets(filePath);
        CharacterCodingException lastCodingException = null;
        IOException lastIoException = null;
        for (Charset charset : candidateCharsets) {
            try {
                return readCsvWithCharset(filePath, charset);
            } catch (CharacterCodingException ex) {
                lastCodingException = ex;
            } catch (IOException ex) {
                lastIoException = ex;
                break;
            }
        }
        if (lastCodingException != null) {
            throw new IOException("Unable to read CSV file: " + filePath, lastCodingException);
        }
        if (lastIoException != null) {
            throw lastIoException;
        }
        throw new IOException("Unable to read CSV file: " + filePath);
    }

    private static List<QuoteRecord> readCsvWithCharset(Path filePath, Charset charset) throws IOException {
        List<QuoteRecord> records = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(filePath, charset);
             CSVReader csvReader = new CSVReaderBuilder(reader)
                     .withCSVParser(new CSVParserBuilder().withSeparator(',').build())
                     .build()) {
            try {
                String[] headers = csvReader.readNext();
                if (headers == null) {
                    return records;
                }
                Map<Integer, String> headerIndex = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    if (headers[i] != null && !headers[i].trim().isEmpty()) {
                        String header = headers[i].trim();
                        headerIndex.put(i, header);
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
                        if (DateNormalizer.isDateColumn(header)) {
                            value = DateNormalizer.normalize(value);
                        }
                        values.put(header, value);
                    }
                    records.add(QuoteRecord.fromValues(values));
                }
            } catch (CsvValidationException ex) {
                throw new IOException("Unable to read CSV file: " + filePath, ex);
            }
        } catch (RuntimeException ex) {
            throw new IOException("Unable to read CSV file: " + filePath, ex);
        }
        return records;
    }

    private static List<Charset> buildCandidateCharsets(Path filePath) {
        LinkedHashSet<Charset> candidates = new LinkedHashSet<>();
        Charset bomCharset = detectBomCharset(filePath);
        if (bomCharset != null) {
            candidates.add(bomCharset);
        }
        candidates.add(StandardCharsets.UTF_8);
        candidates.add(StandardCharsets.UTF_16LE);
        candidates.add(StandardCharsets.UTF_16BE);
        addCharsetIfAvailable(candidates, "windows-1252");
        candidates.add(StandardCharsets.ISO_8859_1);
        return new ArrayList<>(candidates);
    }

    private static Charset detectBomCharset(Path filePath) {
        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(filePath))) {
            inputStream.mark(3);
            int first = inputStream.read();
            int second = inputStream.read();
            int third = inputStream.read();
            if (first == 0xFF && second == 0xFE) {
                return StandardCharsets.UTF_16LE;
            }
            if (first == 0xFE && second == 0xFF) {
                return StandardCharsets.UTF_16BE;
            }
            if (first == 0xEF && second == 0xBB && third == 0xBF) {
                return StandardCharsets.UTF_8;
            }
        } catch (IOException ex) {
            return null;
        }
        return null;
    }

    private static void addCharsetIfAvailable(Set<Charset> target, String charsetName) {
        try {
            target.add(Charset.forName(charsetName));
        } catch (IllegalArgumentException ex) {
            // Ignore unsupported charsets.
        }
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
