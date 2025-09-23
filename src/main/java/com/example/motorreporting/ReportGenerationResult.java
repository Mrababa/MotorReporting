package com.example.motorreporting;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Holds references to all generated artifacts for a report run.
 */
public final class ReportGenerationResult {

    private final Path cleanedDataPath;
    private final Path htmlReportPath;
    private final Path pdfReportPath;

    public ReportGenerationResult(Path cleanedDataPath, Path htmlReportPath, Path pdfReportPath) {
        this.cleanedDataPath = Objects.requireNonNull(cleanedDataPath, "cleanedDataPath");
        this.htmlReportPath = Objects.requireNonNull(htmlReportPath, "htmlReportPath");
        this.pdfReportPath = Objects.requireNonNull(pdfReportPath, "pdfReportPath");
    }

    public Path getCleanedDataPath() {
        return cleanedDataPath;
    }

    public Path getHtmlReportPath() {
        return htmlReportPath;
    }

    public Path getPdfReportPath() {
        return pdfReportPath;
    }
}
