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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Loads quote data from either Excel or CSV files.
 */
public final class QuoteDataLoader {

    private QuoteDataLoader() {
    }

    private static final Set<String> LOGIC_COLUMN_KEYS = buildLogicColumnKeys();

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
            SheetSelection selection = findSheetSelection(workbook, formatter);
            if (selection == null) {
                return records;
            }

            int firstRow = selection.headerRowIndex + 1;
            int lastRow = selection.sheet.getLastRowNum();
            for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++) {
                Row row = selection.sheet.getRow(rowIndex);
                if (row == null || isRowEmpty(row, formatter)) {
                    continue;
                }
                Map<String, String> values = extractRowValues(row, selection.headerIndex, formatter);
                records.add(QuoteRecord.fromValues(values));
            }
        } catch (Exception ex) {
            throw new IOException("Unable to read Excel file: " + filePath, ex);
        }
        return records;
    }

    private static SheetSelection findSheetSelection(Workbook workbook, DataFormatter formatter) {
        SheetSelection bestSelection = null;
        int sheetCount = workbook.getNumberOfSheets();
        for (int i = 0; i < sheetCount; i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                continue;
            }
            SheetSelection candidate = analyzeSheet(sheet, formatter);
            if (candidate == null) {
                continue;
            }
            if (bestSelection == null || candidate.logicColumnCount > bestSelection.logicColumnCount) {
                bestSelection = candidate;
            }
        }
        return bestSelection;
    }

    private static SheetSelection analyzeSheet(Sheet sheet, DataFormatter formatter) {
        SheetSelection bestCandidate = null;
        int firstRowNum = sheet.getFirstRowNum();
        int lastRowNum = sheet.getLastRowNum();
        for (int rowIndex = firstRowNum; rowIndex <= lastRowNum; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isRowEmpty(row, formatter)) {
                continue;
            }
            Map<Integer, String> headerIndex = extractHeaders(row, formatter);
            if (headerIndex.isEmpty()) {
                continue;
            }
            int logicColumnCount = countLogicColumns(headerIndex);
            SheetSelection candidate = new SheetSelection(sheet, headerIndex, rowIndex, logicColumnCount);
            if (bestCandidate == null || candidate.logicColumnCount > bestCandidate.logicColumnCount) {
                bestCandidate = candidate;
            }
        }
        return bestCandidate;
    }

    private static int countLogicColumns(Map<Integer, String> headerIndex) {
        int count = 0;
        for (String header : headerIndex.values()) {
            if (LOGIC_COLUMN_KEYS.contains(normalizeHeaderKey(header))) {
                count++;
            }
        }
        return count;
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

    private static Set<String> buildLogicColumnKeys() {
        Set<String> keys = new HashSet<>();
        String[] columns = {
                "QuoteRequestedOn",
                "Status",
                "ReferenceNumber",
                "InsurancePurpose",
                "ICName",
                "ShoryMakeEn",
                "ShoryModelEn",
                "OverrideIsGccSpec",
                "Age",
                "LicenseIssueDate",
                "BodyCategory",
                "ChassisNumber",
                "ChassisNo",
                "Chassis No",
                "VehicleIdentificationNumber",
                "VIN",
                "InsuranceType",
                "ManufactureYear",
                "RegistrationDate",
                "QuotationNo",
                "QuotationNumber",
                "QuoteNumber",
                "QuoteNo",
                "Quote #",
                "Quotation #",
                "EstimatedValue",
                "InsuranceExpiryDate",
                "ErrorText"
        };
        for (String column : columns) {
            keys.add(normalizeHeaderKey(column));
        }
        return keys;
    }

    private static String normalizeHeaderKey(String header) {
        if (header == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(header.length());
        for (int i = 0; i < header.length(); i++) {
            char ch = header.charAt(i);
            if (Character.isWhitespace(ch) || ch == '_') {
                continue;
            }
            builder.append(Character.toLowerCase(ch));
        }
        return builder.toString();
    }

    private static final class SheetSelection {
        private final Sheet sheet;
        private final Map<Integer, String> headerIndex;
        private final int headerRowIndex;
        private final int logicColumnCount;

        private SheetSelection(Sheet sheet, Map<Integer, String> headerIndex, int headerRowIndex, int logicColumnCount) {
            this.sheet = sheet;
            this.headerIndex = headerIndex;
            this.headerRowIndex = headerRowIndex;
            this.logicColumnCount = logicColumnCount;
        }
    }

    private static List<QuoteRecord> loadFromCsv(Path filePath) throws IOException {
        List<Charset> charsets = buildCsvCharsetCandidates(filePath);
        IOException decodingFailure = null;
        for (Charset charset : charsets) {
            try {
                return parseCsv(filePath, charset);
            } catch (IOException ex) {
                if (ex instanceof CharacterCodingException) {
                    decodingFailure = new IOException(
                            "Failed to decode CSV using charset " + charset.displayName(Locale.ROOT), ex);
                    continue;
                }
                throw ex;
            }
        }
        if (decodingFailure != null) {
            throw new IOException("Unable to read CSV file: " + filePath, decodingFailure);
        }
        return parseCsv(filePath, StandardCharsets.UTF_8);
    }

    private static List<QuoteRecord> parseCsv(Path filePath, Charset charset) throws IOException {
        try (Reader reader = Files.newBufferedReader(filePath, charset);
             CSVReader csvReader = new CSVReaderBuilder(reader)
                     .withCSVParser(new CSVParserBuilder().withSeparator(',').build())
                     .build()) {
            return readCsvRecords(csvReader);
        } catch (CharacterCodingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Unable to read CSV file: " + filePath, ex);
        }
    }

    private static List<QuoteRecord> readCsvRecords(CSVReader csvReader) throws IOException {
        List<QuoteRecord> records = new ArrayList<>();
        String[] headers = csvReader.readNext();
        if (headers == null) {
            return records;
        }
        Map<Integer, String> headerIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            if (header != null) {
                header = stripBom(header).trim();
            }
            if (header != null && !header.isEmpty()) {
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
                String rawValue = index < row.length ? row[index] : null;
                String value = rawValue == null ? "" : stripBom(rawValue).trim();
                if (DateNormalizer.isDateColumn(header)) {
                    value = DateNormalizer.normalize(value);
                }
                values.put(header, value);
            }
            records.add(QuoteRecord.fromValues(values));
        }
        return records;
    }

    private static List<Charset> buildCsvCharsetCandidates(Path filePath) throws IOException {
        LinkedHashSet<Charset> candidates = new LinkedHashSet<>();
        Charset bomCharset = detectBomCharset(filePath);
        if (bomCharset != null) {
            candidates.add(bomCharset);
        }
        candidates.add(StandardCharsets.UTF_8);
        candidates.add(StandardCharsets.UTF_16);
        candidates.add(StandardCharsets.UTF_16LE);
        candidates.add(StandardCharsets.UTF_16BE);
        candidates.add(StandardCharsets.ISO_8859_1);
        return new ArrayList<>(candidates);
    }

    private static Charset detectBomCharset(Path filePath) throws IOException {
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(filePath))) {
            int first = inputStream.read();
            if (first == -1) {
                return null;
            }
            int second = inputStream.read();
            if (second == -1) {
                return null;
            }
            if (first == 0xEF && second == 0xBB) {
                int third = inputStream.read();
                if (third == 0xBF) {
                    return StandardCharsets.UTF_8;
                }
            } else if (first == 0xFE && second == 0xFF) {
                return StandardCharsets.UTF_16;
            } else if (first == 0xFF && second == 0xFE) {
                return StandardCharsets.UTF_16;
            }
            return null;
        }
    }

    private static String stripBom(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.charAt(0) == '\ufeff') {
            return value.substring(1);
        }
        return value;
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
