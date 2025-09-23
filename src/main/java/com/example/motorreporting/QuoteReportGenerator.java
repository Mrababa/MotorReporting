package com.example.motorreporting;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Application entry point.
 */
public final class QuoteReportGenerator {

    private static final String DEFAULT_SOURCE_DIRECTORY = "source data";

    private QuoteReportGenerator() {
    }

    public static void main(String[] args) {
        Path inputPath;
        try {
            String fileName = args.length > 0 ? args[0] : null;
            inputPath = resolveInputPath(fileName);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.err.println("Usage: java -jar motor-reporting.jar <input-file>");
            System.exit(1);
            return;
        } catch (IOException ex) {
            System.err.println("Unable to access input file: " + ex.getMessage());
            System.exit(1);
            return;
        }

        try {
            Path outputPath = generateReport(inputPath);
            System.out.println("Report generated at: " + outputPath.toAbsolutePath());
        } catch (IOException ex) {
            System.err.println("Failed to generate report: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(2);
        }
    }

    public static Path resolveInputPath(String fileName) throws IOException {
        if (fileName != null && !fileName.isBlank()) {
            Path provided = Paths.get(fileName);
            if (Files.exists(provided)) {
                return provided;
            }
            Path fromSourceDirectory = Paths.get(DEFAULT_SOURCE_DIRECTORY).resolve(fileName);
            if (Files.exists(fromSourceDirectory)) {
                return fromSourceDirectory;
            }
            throw new IllegalArgumentException("Input file not found: " + provided);
        }
        return findSingleFileInSourceDirectory();
    }

    public static Path generateReport(Path inputPath) throws IOException {
        List<QuoteRecord> records = QuoteDataLoader.load(inputPath);
        if (records.isEmpty()) {
            System.err.println("No records found in file: " + inputPath);
        }

        QuoteStatistics statistics = QuoteStatisticsCalculator.calculate(records);
        Path outputPath = inputPath.toAbsolutePath().getParent() != null
                ? inputPath.toAbsolutePath().getParent().resolve("quote_generation_report.html")
                : Paths.get("quote_generation_report.html");

        HtmlReportGenerator htmlReportGenerator = new HtmlReportGenerator();
        htmlReportGenerator.generate(outputPath, statistics, records);
        return outputPath;
    }

    private static Path findSingleFileInSourceDirectory() throws IOException {
        Path sourceDirectory = Paths.get(DEFAULT_SOURCE_DIRECTORY);
        if (!Files.isDirectory(sourceDirectory)) {
            throw new IllegalArgumentException("Source data directory not found: "
                    + sourceDirectory.toAbsolutePath());
        }

        List<Path> candidates = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDirectory)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry) && isSupportedDataFile(entry)) {
                    candidates.add(entry);
                }
            }
        }

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("No supported data files found in source data directory: "
                    + sourceDirectory.toAbsolutePath());
        }
        if (candidates.size() > 1) {
            throw new IllegalArgumentException("Multiple data files found in source data directory: "
                    + sourceDirectory.toAbsolutePath() + ". Please specify which file to use.");
        }
        return candidates.get(0);
    }

    private static boolean isSupportedDataFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".xlsx") || fileName.endsWith(".xls") || fileName.endsWith(".csv");
    }
}
