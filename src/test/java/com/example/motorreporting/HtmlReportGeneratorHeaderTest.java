package com.example.motorreporting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlReportGeneratorHeaderTest {

    @Test
    void headerUsesQuoteRequestedOnRangeWhenHeaderContainsSpaces(@TempDir Path tempDir) throws IOException {
        Map<String, String> firstRow = new HashMap<>();
        firstRow.put("Quote Requested On", "2024-01-05 08:15");
        firstRow.put("InsuranceType", "TPL");
        firstRow.put("Status", "Success");
        firstRow.put("QuotationNo", "QTN-0001");

        Map<String, String> secondRow = new HashMap<>();
        secondRow.put("Quote Requested On", "2024-01-11 16:05");
        secondRow.put("InsuranceType", "Comprehensive");
        secondRow.put("Status", "Failed");
        secondRow.put("ErrorText", "Vehicle inspection required");

        List<QuoteRecord> records = List.of(
                QuoteRecord.fromValues(firstRow),
                QuoteRecord.fromValues(secondRow)
        );

        QuoteStatistics statistics = QuoteStatisticsCalculator.calculate(records);

        HtmlReportGenerator generator = new HtmlReportGenerator();
        Path output = tempDir.resolve("report.html");
        generator.generate(output, statistics, records);

        String html = Files.readString(output);
        assertTrue(html.contains("Overview of Quote requests Analysis from 2024-01-05 to 2024-01-11"));
    }
}
