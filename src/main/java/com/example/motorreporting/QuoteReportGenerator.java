package com.example.motorreporting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Application entry point.
 */
public final class QuoteReportGenerator {

    private QuoteReportGenerator() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java -jar motor-reporting.jar <input-file>");
            System.exit(1);
        }

        Path inputPath = Paths.get(args[0]);
        if (!Files.exists(inputPath)) {
            System.err.println("Input file not found: " + inputPath);
            System.exit(1);
        }

        try {
            List<QuoteRecord> records = QuoteDataLoader.load(inputPath);
            if (records.isEmpty()) {
                System.err.println("No records found in file: " + inputPath);
            }

            QuoteStatistics statistics = QuoteStatisticsCalculator.calculate(records);
            Path outputPath = inputPath.toAbsolutePath().getParent() != null
                    ? inputPath.toAbsolutePath().getParent().resolve("quote_generation_report.pdf")
                    : Paths.get("quote_generation_report.pdf");

            PdfReportGenerator pdfReportGenerator = new PdfReportGenerator();
            pdfReportGenerator.generate(outputPath, statistics);
            System.out.println("Report generated at: " + outputPath.toAbsolutePath());
        } catch (IOException ex) {
            System.err.println("Failed to generate report: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(2);
        }
    }
}
