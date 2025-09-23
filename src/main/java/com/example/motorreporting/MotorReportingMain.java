package com.example.motorreporting;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Convenience entry point for running the report generator from code.
 */
public final class MotorReportingMain {

    private MotorReportingMain() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: MotorReportingMain <input-file>");
            System.err.println("Place the data file in the 'source data' directory or provide an absolute path.");
            System.exit(1);
            return;
        }

        try {
            Path outputPath = run(args[0]);
            System.out.println("Report generated at: " + outputPath.toAbsolutePath());
        } catch (IOException ex) {
            System.err.println("Failed to generate report: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(2);
        }
    }

    public static Path run(String fileName) throws IOException {
        Objects.requireNonNull(fileName, "fileName");
        Path inputPath = QuoteReportGenerator.resolveInputPath(fileName);
        return QuoteReportGenerator.generateReport(inputPath);
    }
}
