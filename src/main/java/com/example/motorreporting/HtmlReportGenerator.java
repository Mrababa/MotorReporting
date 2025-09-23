package com.example.motorreporting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Builds the simplified HTML quote report.
 */
public class HtmlReportGenerator {

    private static final String LOGO_URL = "https://www.shory.com/imgs/master/logo.svg";
    private static final DecimalFormat INTEGER_FORMAT;
    private static final DecimalFormat PERCENT_FORMAT;
    private static final DateTimeFormatter[] QUOTE_REQUESTED_ON_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };
    private static final DateTimeFormatter HEADER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);
    private static final String QUOTE_REQUESTED_ON_KEY_NORMALIZED =
            normalizeHeaderKey("QuoteRequestedOn");
    private static final String SPEC_GCC_LABEL = "GCC";
    private static final String SPEC_NON_GCC_LABEL = "Non GCC";
    private static final String SPEC_UNKNOWN_LABEL = "Unknown";
    private static final QuoteStatistics.OutcomeBreakdown EMPTY_OUTCOME =
            new QuoteStatistics.OutcomeBreakdown(0, 0);

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        INTEGER_FORMAT = new DecimalFormat("#,##0", symbols);
        PERCENT_FORMAT = new DecimalFormat("#0.0%", symbols);
    }

    public void generate(Path outputPath, QuoteStatistics statistics, List<QuoteRecord> records) throws IOException {
        Objects.requireNonNull(outputPath, "outputPath");
        Objects.requireNonNull(statistics, "statistics");
        Objects.requireNonNull(records, "records");

        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String html = buildHtml(statistics, records);
        Files.writeString(outputPath, html, StandardCharsets.UTF_8);
    }

    private String buildHtml(QuoteStatistics statistics, List<QuoteRecord> records) {
        QuoteGroupStats tplStats = statistics.getTplStats();
        QuoteGroupStats compStats = statistics.getComprehensiveStats();
        Map<String, QuoteStatistics.OutcomeBreakdown> tplSpecificationSummary =
                aggregateSpecificationOutcomes(statistics.getTplSpecificationOutcomes());
        Map<String, QuoteStatistics.OutcomeBreakdown> compSpecificationSummary =
                aggregateSpecificationOutcomes(statistics.getComprehensiveSpecificationOutcomes());

        long totalQuotes = statistics.getOverallTotalQuotes();
        long successCount = statistics.getOverallPassCount();
        long failCount = statistics.getOverallFailCount();
        long uniqueChassisTotal = statistics.getUniqueChassisCount();
        long uniqueChassisSuccess = statistics.getUniqueChassisSuccessCount();
        long uniqueChassisFail = statistics.getUniqueChassisFailCount();
        long tplUniqueChassisSuccess = statistics.getTplUniqueChassisSuccessCount();
        long tplUniqueChassisFail = statistics.getTplUniqueChassisFailCount();
        long compUniqueChassisSuccess = statistics.getComprehensiveUniqueChassisSuccessCount();
        long compUniqueChassisFail = statistics.getComprehensiveUniqueChassisFailCount();
        String headerText = buildHeaderText(records);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"utf-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        html.append("    <title>Quote Generation Summary</title>\n");
        html.append("    <style>\n");
        html.append("        :root {\n");
        html.append("            color-scheme: light;\n");
        html.append("        }\n");
        html.append("        body {\n");
        html.append("            margin: 0;\n");
        html.append("            font-family: 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;\n");
        html.append("            background-color: #f4f6fb;\n");
        html.append("            color: #1f2937;\n");
        html.append("        }\n");
        html.append("        main {\n");
        html.append("            max-width: 960px;\n");
        html.append("            margin: 0 auto;\n");
        html.append("            padding: 3.5rem 1.5rem 4rem;\n");
        html.append("        }\n");
        html.append("        .page-header {\n");
        html.append("            text-align: center;\n");
        html.append("            margin-bottom: 3rem;\n");
        html.append("        }\n");
        html.append("        .company-logo {\n");
        html.append("            width: clamp(140px, 18vw, 180px);\n");
        html.append("            height: auto;\n");
        html.append("            margin: 0 auto 1.75rem;\n");
        html.append("            display: block;\n");
        html.append("        }\n");
        html.append("        .page-header h1 {\n");
        html.append("            margin: 0 0 0.75rem;\n");
        html.append("            font-size: clamp(1.75rem, 2.5vw + 1rem, 2.5rem);\n");
        html.append("            font-weight: 700;\n");
        html.append("            color: #0d1b3e;\n");
        html.append("        }\n");
        html.append("        .page-header p {\n");
        html.append("            margin: 0 auto;\n");
        html.append("            max-width: 640px;\n");
        html.append("            color: #6b7280;\n");
        html.append("            font-size: 1rem;\n");
        html.append("        }\n");
        html.append("        .summary-grid {\n");
        html.append("            display: grid;\n");
        html.append("            gap: 1.5rem;\n");
        html.append("            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));\n");
        html.append("        }\n");
        html.append("        .summary-section {\n");
        html.append("            margin-top: 3rem;\n");
        html.append("        }\n");
        html.append("        .section-title {\n");
        html.append("            margin: 0 0 1.5rem;\n");
        html.append("            font-size: 1.35rem;\n");
        html.append("            font-weight: 700;\n");
        html.append("            color: #0d1b3e;\n");
        html.append("        }\n");
        html.append("        .summary-card {\n");
        html.append("            background: #ffffff;\n");
        html.append("            border-radius: 18px;\n");
        html.append("            padding: 1.75rem;\n");
        html.append("            box-shadow: 0 20px 45px -20px rgba(15, 23, 42, 0.35);\n");
        html.append("            position: relative;\n");
        html.append("            overflow: hidden;\n");
        html.append("        }\n");
        html.append("        .summary-card::after {\n");
        html.append("            content: '';\n");
        html.append("            position: absolute;\n");
        html.append("            width: 160px;\n");
        html.append("            height: 160px;\n");
        html.append("            border-radius: 50%;\n");
        html.append("            top: -80px;\n");
        html.append("            right: -60px;\n");
        html.append("            opacity: 0.18;\n");
        html.append("            background: var(--accent, #2563eb);\n");
        html.append("        }\n");
        html.append("        .summary-label {\n");
        html.append("            text-transform: uppercase;\n");
        html.append("            letter-spacing: 0.08em;\n");
        html.append("            font-weight: 600;\n");
        html.append("            font-size: 0.85rem;\n");
        html.append("            color: #6b7280;\n");
        html.append("            margin-bottom: 0.75rem;\n");
        html.append("        }\n");
        html.append("        .summary-value {\n");
        html.append("            font-size: clamp(2rem, 3vw + 1rem, 2.75rem);\n");
        html.append("            font-weight: 700;\n");
        html.append("            color: #0d1b3e;\n");
        html.append("        }\n");
        html.append("        .charts {\n");
        html.append("            margin-top: 3rem;\n");
        html.append("        }\n");
        html.append("        .chart-grid {\n");
        html.append("            display: grid;\n");
        html.append("            gap: 1.5rem;\n");
        html.append("            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));\n");
        html.append("        }\n");
        html.append("        .chart-card {\n");
        html.append("            background: #ffffff;\n");
        html.append("            border-radius: 18px;\n");
        html.append("            padding: 1.5rem 1.75rem;\n");
        html.append("            box-shadow: 0 20px 45px -20px rgba(15, 23, 42, 0.28);\n");
        html.append("        }\n");
        html.append("        .chart-card h2 {\n");
        html.append("            margin: 0 0 1rem;\n");
        html.append("            font-size: 1.1rem;\n");
        html.append("            font-weight: 700;\n");
        html.append("            color: #0d1b3e;\n");
        html.append("        }\n");
        html.append("        .page-nav {\n");
        html.append("            display: flex;\n");
        html.append("            justify-content: center;\n");
        html.append("            gap: 1rem;\n");
        html.append("            flex-wrap: wrap;\n");
        html.append("            margin-bottom: 2.5rem;\n");
        html.append("        }\n");
        html.append("        .nav-button {\n");
        html.append("            border: none;\n");
        html.append("            background: #e0e7ff;\n");
        html.append("            color: #1e3a8a;\n");
        html.append("            padding: 0.65rem 1.25rem;\n");
        html.append("            border-radius: 999px;\n");
        html.append("            font-weight: 600;\n");
        html.append("            cursor: pointer;\n");
        html.append("            transition: background-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;\n");
        html.append("        }\n");
        html.append("        .nav-button:hover {\n");
        html.append("            background: #c7d2fe;\n");
        html.append("        }\n");
        html.append("        .nav-button.active {\n");
        html.append("            background: #1e3a8a;\n");
        html.append("            color: #ffffff;\n");
        html.append("            box-shadow: 0 12px 30px -12px rgba(30, 58, 138, 0.6);\n");
        html.append("        }\n");
        html.append("        .page-section {\n");
        html.append("            display: none;\n");
        html.append("        }\n");
        html.append("        .page-section.active {\n");
        html.append("            display: block;\n");
        html.append("        }\n");
        html.append("        .table-card {\n");
        html.append("            background: #ffffff;\n");
        html.append("            border-radius: 18px;\n");
        html.append("            padding: 1.5rem 1.75rem;\n");
        html.append("            box-shadow: 0 20px 45px -20px rgba(15, 23, 42, 0.28);\n");
        html.append("            margin-top: 2rem;\n");
        html.append("            overflow-x: auto;\n");
        html.append("        }\n");
        html.append("        .table-card h3 {\n");
        html.append("            margin: 0 0 1rem;\n");
        html.append("            font-size: 1.1rem;\n");
        html.append("            font-weight: 700;\n");
        html.append("            color: #0d1b3e;\n");
        html.append("        }\n");
        html.append("        .table-card table {\n");
        html.append("            width: 100%;\n");
        html.append("            border-collapse: collapse;\n");
        html.append("        }\n");
        html.append("        .table-card th, .table-card td {\n");
        html.append("            padding: 0.75rem 0.5rem;\n");
        html.append("            border-bottom: 1px solid #e5e7eb;\n");
        html.append("            text-align: left;\n");
        html.append("            font-size: 0.95rem;\n");
        html.append("        }\n");
        html.append("        .table-card th {\n");
        html.append("            font-weight: 600;\n");
        html.append("            color: #0f172a;\n");
        html.append("        }\n");
        html.append("        .table-card tbody tr:last-child td {\n");
        html.append("            border-bottom: none;\n");
        html.append("        }\n");
        html.append("        .numeric {\n");
        html.append("            text-align: right;\n");
        html.append("        }\n");
        html.append("        .empty-state {\n");
        html.append("            margin: 0;\n");
        html.append("            font-style: italic;\n");
        html.append("            color: #6b7280;\n");
        html.append("        }\n");
        html.append("        canvas {\n");
        html.append("            width: 100%;\n");
        html.append("            max-width: 100%;\n");
        html.append("            height: 320px;\n");
        html.append("        }\n");
        html.append("        @media (max-width: 600px) {\n");
        html.append("            main {\n");
        html.append("                padding: 2.5rem 1.25rem 3rem;\n");
        html.append("            }\n");
        html.append("            canvas {\n");
        html.append("                height: 260px;\n");
        html.append("            }\n");
        html.append("        }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<main>\n");
        html.append("  <div class=\"page-header\">\n");
        html.append("    <img src=\"")
                .append(escapeHtml(LOGO_URL))
                .append("\" alt=\"Shory company logo\" class=\"company-logo\" loading=\"lazy\">\n");
        html.append("    <h1>")
                .append(escapeHtml(headerText))
                .append("</h1>\n");
        html.append("    <p>Overview of quote requests with pass and fail counts for each insurance type.</p>\n");
        html.append("  </div>\n");
        html.append("  <nav class=\"page-nav\">\n");
        html.append("    <button type=\"button\" class=\"nav-button active\" data-target=\"overview\">Overview</button>\n");
        html.append("    <button type=\"button\" class=\"nav-button\" data-target=\"tpl-analysis\">TPL Analysis</button>\n");
        html.append("    <button type=\"button\" class=\"nav-button\" data-target=\"comprehensive-analysis\">Comprehensive Analysis</button>\n");
        html.append("  </nav>\n");
        html.append("  <section id=\"overview\" class=\"page-section active\">\n");
        html.append("    <div class=\"summary-grid\">\n");
        appendSummaryCard(html, "Total Quotes Requested", totalQuotes, "#2563eb");
        appendSummaryCard(html, "Successful Quotes", successCount, "#16a34a");
        appendSummaryCard(html, "Failed Quotes", failCount, "#dc2626");
        html.append("    </div>\n");
        html.append("    <div class=\"summary-section\">\n");
        html.append("      <h2 class=\"section-title\">Unique Chassis Overview</h2>\n");
        html.append("      <div class=\"summary-grid\">\n");
        appendSummaryCard(html, "Unique Chassis Requested", uniqueChassisTotal, "#2563eb");
        appendSummaryCard(html, "Unique Chassis Success", uniqueChassisSuccess, "#16a34a");
        appendSummaryCard(html, "Unique Chassis Failed", uniqueChassisFail, "#dc2626");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"charts\">\n");
        html.append("      <div class=\"chart-grid\">\n");
        html.append("        <div class=\"chart-card\">\n");
        html.append("          <h2>TPL Success vs Failed</h2>\n");
        html.append("          <canvas id=\"tplOutcomesChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"chart-card\">\n");
        html.append("          <h2>Comprehensive Success vs Failed</h2>\n");
        html.append("          <canvas id=\"compOutcomesChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"chart-card\">\n");
        html.append("          <h2>TPL Success vs Failed (Unique Chassis)</h2>\n");
        html.append("          <canvas id=\"tplUniqueChassisChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"chart-card\">\n");
        html.append("          <h2>Comprehensive Success vs Failed (Unique Chassis)</h2>\n");
        html.append("          <canvas id=\"compUniqueChassisChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"chart-card\">\n");
        html.append("          <h2>Manufacture Year Trend</h2>\n");
        html.append("          <canvas id=\"overallManufactureYearChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"chart-card\">\n");
        html.append("          <h2>Customer Age Trend</h2>\n");
        html.append("          <canvas id=\"overallCustomerAgeChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        appendCategoryCountTable(html, "Unique Chassis by Insurance Purpose", "Insurance Purpose",
                statistics.getUniqueChassisByInsurancePurpose());
        appendCategoryCountTable(html, "Unique Chassis by Body Type", "Body Type",
                statistics.getUniqueChassisByBodyType());
        appendMakeModelTable(html, "Top 20 Make & Model by Unique Chassis",
                statistics.getTopRequestedMakeModelsByUniqueChassis());
        html.append("  </section>\n");
        html.append("  <section id=\"tpl-analysis\" class=\"page-section\">\n");
        html.append("    <div class=\"summary-section\">\n");
        html.append("      <h2 class=\"section-title\">TPL Summary</h2>\n");
        html.append("      <div class=\"summary-grid\">\n");
        appendSummaryCard(html, "TPL Requests", tplStats.getTotalQuotes(), "#2563eb");
        appendSummaryCard(html, "TPL Success", tplStats.getPassCount(), "#16a34a");
        appendSummaryCard(html, "TPL Failed", tplStats.getFailCount(), "#dc2626");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"charts\">\n");
        html.append("      <h2 class=\"section-title\">TPL Requests by Body Category</h2>\n");
        html.append("      <div class=\"chart-grid\">\n");
        html.append("        <div class=\"chart-card\">\n");
        html.append("          <h2>Successful Requests</h2>\n");
        html.append("          <canvas id=\"tplBodySuccessChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"chart-card\">\n");
        html.append("          <h2>Failed Requests</h2>\n");
        html.append("          <canvas id=\"tplBodyFailureChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        appendOutcomeTable(html, "Body Category Outcomes", statistics.getTplBodyCategoryOutcomes());
        html.append("    <div class=\"charts\">\n");
        html.append("      <h2 class=\"section-title\">TPL Specification Outcomes</h2>\n");
        html.append("      <div class=\"chart-grid\">\n");
        html.append("        <div class=\"chart-card\">\n");
        html.append("          <h2>GCC</h2>\n");
        html.append("          <canvas id=\"tplSpecGccChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"chart-card\">\n");
        html.append("          <h2>Non-GCC</h2>\n");
        html.append("          <canvas id=\"tplSpecNonGccChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        appendOutcomeTable(html, "Specification Outcomes (GCC vs Non-GCC)", tplSpecificationSummary);
        appendMakeModelTable(html, "Top 20 Make & Model by Unique Chassis (TPL)",
                statistics.getTplTopRequestedMakeModelsByUniqueChassis());
        html.append("    <div class=\"charts\">\n");
        html.append("      <div class=\"chart-card\">\n");
        html.append("        <h2>Success vs Failure Ratio by Age Range</h2>\n");
        html.append("        <canvas id=\"tplAgeRatioChart\"></canvas>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"charts\">\n");
        html.append("      <div class=\"chart-card\">\n");
        html.append("        <h2>Success vs Failure Ratio by Manufacture Year</h2>\n");
        html.append("        <canvas id=\"tplManufactureYearChart\"></canvas>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        appendModelChassisTable(html, "Top 10 Rejected Models (Unique Chassis)",
                statistics.getTplTopRejectedModelsByUniqueChassis());
        appendErrorTable(html, "TPL Error Counts", statistics.getTplErrorCounts());
        html.append("  </section>\n");
        html.append("  <section id=\"comprehensive-analysis\" class=\"page-section\">\n");
        html.append("    <div class=\"summary-section\">\n");
        html.append("      <h2 class=\"section-title\">Comprehensive Summary</h2>\n");
        html.append("      <div class=\"summary-grid\">\n");
        appendSummaryCard(html, "Comprehensive Requests", compStats.getTotalQuotes(), "#2563eb");
        appendSummaryCard(html, "Comprehensive Success", compStats.getPassCount(), "#16a34a");
        appendSummaryCard(html, "Comprehensive Failed", compStats.getFailCount(), "#dc2626");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"charts\">\n");
        html.append("      <h2 class=\"section-title\">Comprehensive Requests by Body Category</h2>\n");
        html.append("      <div class=\"chart-grid\">\n");
        html.append("        <div class=\"chart-card\">\n");
        html.append("          <h2>Successful Requests</h2>\n");
        html.append("          <canvas id=\"compBodySuccessChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"chart-card\">\n");
        html.append("          <h2>Failed Requests</h2>\n");
        html.append("          <canvas id=\"compBodyFailureChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        appendOutcomeTable(html, "Comprehensive Body Category Outcomes", statistics.getComprehensiveBodyCategoryOutcomes());
        html.append("    <div class=\"charts\">\n");
        html.append("      <h2 class=\"section-title\">Comprehensive Specification Outcomes</h2>\n");
        html.append("      <div class=\"chart-grid\">\n");
        html.append("        <div class=\"chart-card\">\n");
        html.append("          <h2>GCC</h2>\n");
        html.append("          <canvas id=\"compSpecGccChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"chart-card\">\n");
        html.append("          <h2>Non-GCC</h2>\n");
        html.append("          <canvas id=\"compSpecNonGccChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        appendOutcomeTable(html, "Comprehensive Specification Outcomes (GCC vs Non-GCC)", compSpecificationSummary);
        appendMakeModelTable(html, "Top 20 Make & Model by Unique Chassis (Comprehensive)",
                statistics.getComprehensiveTopRequestedMakeModelsByUniqueChassis());
        html.append("    <div class=\"charts\">\n");
        html.append("      <div class=\"chart-card\">\n");
        html.append("        <h2>Success vs Failure Ratio by Age Range</h2>\n");
        html.append("        <canvas id=\"compAgeRatioChart\"></canvas>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"charts\">\n");
        html.append("      <div class=\"chart-card\">\n");
        html.append("        <h2>Success vs Failure Ratio by Manufacture Year</h2>\n");
        html.append("        <canvas id=\"compManufactureYearChart\"></canvas>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"charts\">\n");
        html.append("      <div class=\"chart-card\">\n");
        html.append("        <h2>Success vs Failure Ratio by Estimated Value</h2>\n");
        html.append("        <canvas id=\"compEstimatedValueChart\"></canvas>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        appendErrorTable(html, "Comprehensive Error Counts", statistics.getComprehensiveErrorCounts());
        html.append("  </section>\n");
        html.append("</main>\n");
        html.append(buildScripts(statistics, tplSpecificationSummary, compSpecificationSummary));
        html.append("</body>\n");
        html.append("</html>\n");
        return html.toString();
    }

    private String buildHeaderText(List<QuoteRecord> records) {
        Optional<DateRange> dateRange = findQuoteRequestedOnRange(records);
        if (dateRange.isPresent()) {
            DateRange range = dateRange.get();
            return "Overview of Quote requests Analysis from "
                    + formatDateForHeader(range.getStart())
                    + " to "
                    + formatDateForHeader(range.getEnd());
        }
        return "Overview of Quote requests Analysis from N/A to N/A";
    }

    private Optional<DateRange> findQuoteRequestedOnRange(List<QuoteRecord> records) {
        LocalDateTime min = null;
        LocalDateTime max = null;
        for (QuoteRecord record : records) {
            Optional<LocalDateTime> requestedOn = extractQuoteRequestedOn(record);
            if (requestedOn.isEmpty()) {
                continue;
            }
            LocalDateTime value = requestedOn.get();
            if (min == null || value.isBefore(min)) {
                min = value;
            }
            if (max == null || value.isAfter(max)) {
                max = value;
            }
        }
        if (min == null || max == null) {
            return Optional.empty();
        }
        return Optional.of(new DateRange(min, max));
    }

    private Optional<LocalDateTime> extractQuoteRequestedOn(QuoteRecord record) {
        for (Map.Entry<String, String> entry : record.getRawValues().entrySet()) {
            String key = entry.getKey();
            if (isQuoteRequestedOnKey(key)) {
                String value = entry.getValue();
                if (value == null) {
                    return Optional.empty();
                }
                return parseQuoteRequestedOn(value.trim());
            }
        }
        return Optional.empty();
    }

    private static boolean isQuoteRequestedOnKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        String normalized = normalizeHeaderKey(key);
        return normalized.equals(QUOTE_REQUESTED_ON_KEY_NORMALIZED)
                || normalized.contains(QUOTE_REQUESTED_ON_KEY_NORMALIZED);
    }

    private Optional<LocalDateTime> parseQuoteRequestedOn(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        for (DateTimeFormatter formatter : QUOTE_REQUESTED_ON_FORMATS) {
            try {
                return Optional.of(LocalDateTime.parse(trimmed, formatter));
            } catch (DateTimeParseException ex) {
                // Ignore and try next formatter
            }
        }
        try {
            LocalDate date = LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
            return Optional.of(date.atStartOfDay());
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    private String formatDateForHeader(LocalDateTime dateTime) {
        return dateTime.toLocalDate().format(HEADER_DATE_FORMAT);
    }

    private static String normalizeHeaderKey(String key) {
        if (key == null) {
            return "";
        }
        StringBuilder normalized = new StringBuilder(key.length());
        for (int i = 0; i < key.length(); i++) {
            char ch = key.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                normalized.append(Character.toLowerCase(ch));
            }
        }
        return normalized.toString();
    }

    private static final class DateRange {
        private final LocalDateTime start;
        private final LocalDateTime end;

        private DateRange(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }

        private LocalDateTime getStart() {
            return start;
        }

        private LocalDateTime getEnd() {
            return end;
        }
    }

    private void appendSummaryCard(StringBuilder html, String label, long value, String accentColor) {
        html.append("    <div class=\"summary-card\" style=\"--accent: ")
                .append(escapeHtml(accentColor))
                .append(";\">\n");
        html.append("      <div class=\"summary-label\">")
                .append(escapeHtml(label))
                .append("</div>\n");
        html.append("      <div class=\"summary-value\">")
                .append(escapeHtml(formatInteger(value)))
                .append("</div>\n");
        html.append("    </div>\n");
    }

    private Map<String, QuoteStatistics.OutcomeBreakdown> aggregateSpecificationOutcomes(
            Map<String, QuoteStatistics.OutcomeBreakdown> original) {
        long gccSuccess = 0L;
        long gccFailure = 0L;
        long nonGccSuccess = 0L;
        long nonGccFailure = 0L;
        long unknownSuccess = 0L;
        long unknownFailure = 0L;

        for (Map.Entry<String, QuoteStatistics.OutcomeBreakdown> entry : original.entrySet()) {
            QuoteStatistics.OutcomeBreakdown breakdown = entry.getValue();
            if (breakdown == null) {
                continue;
            }
            SpecificationCategory category = classifySpecificationLabel(entry.getKey());
            switch (category) {
                case GCC:
                    gccSuccess += breakdown.getSuccessCount();
                    gccFailure += breakdown.getFailureCount();
                    break;
                case NON_GCC:
                    nonGccSuccess += breakdown.getSuccessCount();
                    nonGccFailure += breakdown.getFailureCount();
                    break;
                default:
                    unknownSuccess += breakdown.getSuccessCount();
                    unknownFailure += breakdown.getFailureCount();
                    break;
            }
        }

        LinkedHashMap<String, QuoteStatistics.OutcomeBreakdown> aggregated = new LinkedHashMap<>();
        aggregated.put(SPEC_GCC_LABEL, new QuoteStatistics.OutcomeBreakdown(gccSuccess, gccFailure));
        aggregated.put(SPEC_NON_GCC_LABEL, new QuoteStatistics.OutcomeBreakdown(nonGccSuccess, nonGccFailure));
        if (unknownSuccess > 0 || unknownFailure > 0) {
            aggregated.put(SPEC_UNKNOWN_LABEL, new QuoteStatistics.OutcomeBreakdown(unknownSuccess, unknownFailure));
        }
        return aggregated;
    }

    private static SpecificationCategory classifySpecificationLabel(String label) {
        if (label == null) {
            return SpecificationCategory.UNKNOWN;
        }
        String normalized = label.trim();
        if (normalized.isEmpty()) {
            return SpecificationCategory.UNKNOWN;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if ("unknown".equals(lower)) {
            return SpecificationCategory.UNKNOWN;
        }
        String condensed = lower.replace("-", "").replace(" ", "");
        switch (condensed) {
            case "gcc":
            case "gccspec":
            case "gccspecification":
            case "true":
            case "1":
            case "yes":
                return SpecificationCategory.GCC;
            case "nongcc":
            case "nonegcc":
            case "nongccspec":
            case "false":
            case "0":
            case "no":
                return SpecificationCategory.NON_GCC;
            default:
                return SpecificationCategory.UNKNOWN;
        }
    }

    private enum SpecificationCategory {
        GCC,
        NON_GCC,
        UNKNOWN
    }

    private void appendOutcomeTable(StringBuilder html,
                                    String heading,
                                    Map<String, QuoteStatistics.OutcomeBreakdown> breakdown) {
        html.append("    <div class=\"table-card\">\n");
        html.append("      <h3>")
                .append(escapeHtml(heading))
                .append("</h3>\n");
        if (breakdown.isEmpty()) {
            html.append("      <p class=\"empty-state\">No data available.</p>\n");
            html.append("    </div>\n");
            return;
        }
        html.append("      <table>\n");
        html.append("        <thead>\n");
        html.append("          <tr>\n");
        html.append("            <th scope=\"col\">Category</th>\n");
        html.append("            <th scope=\"col\" class=\"numeric\">Success</th>\n");
        html.append("            <th scope=\"col\" class=\"numeric\">Failed</th>\n");
        html.append("            <th scope=\"col\" class=\"numeric\">Total</th>\n");
        html.append("            <th scope=\"col\" class=\"numeric\">Success Rate</th>\n");
        html.append("          </tr>\n");
        html.append("        </thead>\n");
        html.append("        <tbody>\n");
        for (Map.Entry<String, QuoteStatistics.OutcomeBreakdown> entry : breakdown.entrySet()) {
            QuoteStatistics.OutcomeBreakdown data = entry.getValue();
            long success = data.getSuccessCount();
            long failure = data.getFailureCount();
            long total = data.getProcessedTotal();
            html.append("          <tr>\n");
            html.append("            <td>")
                    .append(escapeHtml(entry.getKey()))
                    .append("</td>\n");
            html.append("            <td class=\"numeric\">")
                    .append(escapeHtml(formatInteger(success)))
                    .append("</td>\n");
            html.append("            <td class=\"numeric\">")
                    .append(escapeHtml(formatInteger(failure)))
                    .append("</td>\n");
            html.append("            <td class=\"numeric\">")
                    .append(escapeHtml(formatInteger(total)))
                    .append("</td>\n");
            html.append("            <td class=\"numeric\">")
                    .append(escapeHtml(formatPercentage(success, total)))
                    .append("</td>\n");
            html.append("          </tr>\n");
        }
        html.append("        </tbody>\n");
        html.append("      </table>\n");
        html.append("    </div>\n");
    }

    private void appendCategoryCountTable(StringBuilder html,
                                          String heading,
                                          String labelHeader,
                                          List<QuoteStatistics.CategoryCount> counts) {
        html.append("    <div class=\"table-card\">\n");
        html.append("      <h3>")
                .append(escapeHtml(heading))
                .append("</h3>\n");
        if (counts.isEmpty()) {
            html.append("      <p class=\"empty-state\">No data recorded.</p>\n");
            html.append("    </div>\n");
            return;
        }
        html.append("      <table>\n");
        html.append("        <thead>\n");
        html.append("          <tr>\n");
        html.append("            <th scope=\"col\">")
                .append(escapeHtml(labelHeader))
                .append("</th>\n");
        html.append("            <th scope=\"col\" class=\"numeric\">Unique Chassis</th>\n");
        html.append("          </tr>\n");
        html.append("        </thead>\n");
        html.append("        <tbody>\n");
        for (QuoteStatistics.CategoryCount count : counts) {
            html.append("          <tr>\n");
            html.append("            <td>")
                    .append(escapeHtml(count.getLabel()))
                    .append("</td>\n");
            html.append("            <td class=\"numeric\">")
                    .append(escapeHtml(formatInteger(count.getCount())))
                    .append("</td>\n");
            html.append("          </tr>\n");
        }
        html.append("        </tbody>\n");
        html.append("      </table>\n");
        html.append("    </div>\n");
    }

    private void appendModelChassisTable(StringBuilder html,
                                          String heading,
                                          List<QuoteStatistics.ModelChassisSummary> models) {
        html.append("    <div class=\"table-card\">\n");
        html.append("      <h3>")
                .append(escapeHtml(heading))
                .append("</h3>\n");
        if (models.isEmpty()) {
            html.append("      <p class=\"empty-state\">No rejected models recorded.</p>\n");
            html.append("    </div>\n");
            return;
        }
        html.append("      <table>\n");
        html.append("        <thead>\n");
        html.append("          <tr>\n");
        html.append("            <th scope=\"col\">Model</th>\n");
        html.append("            <th scope=\"col\" class=\"numeric\">Unique Chassis Count</th>\n");
        html.append("          </tr>\n");
        html.append("        </thead>\n");
        html.append("        <tbody>\n");
        for (QuoteStatistics.ModelChassisSummary summary : models) {
            html.append("          <tr>\n");
            html.append("            <td>")
                    .append(escapeHtml(summary.getModel()))
                    .append("</td>\n");
            html.append("            <td class=\"numeric\">")
                    .append(escapeHtml(formatInteger(summary.getUniqueChassisCount())))
                    .append("</td>\n");
            html.append("          </tr>\n");
        }
        html.append("        </tbody>\n");
        html.append("      </table>\n");
        html.append("    </div>\n");
    }

    private void appendMakeModelTable(StringBuilder html,
                                      String heading,
                                      List<QuoteStatistics.MakeModelChassisSummary> data) {
        html.append("    <div class=\"table-card\">\n");
        html.append("      <h3>")
                .append(escapeHtml(heading))
                .append("</h3>\n");
        if (data.isEmpty()) {
            html.append("      <p class=\"empty-state\">No make and model information available.</p>\n");
            html.append("    </div>\n");
            return;
        }
        html.append("      <table>\n");
        html.append("        <thead>\n");
        html.append("          <tr>\n");
        html.append("            <th scope=\"col\">Make</th>\n");
        html.append("            <th scope=\"col\">Model</th>\n");
        html.append("            <th scope=\"col\" class=\"numeric\">Unique Chassis Count</th>\n");
        html.append("            <th scope=\"col\" class=\"numeric\">Success Ratio</th>\n");
        html.append("            <th scope=\"col\" class=\"numeric\">Failure Ratio</th>\n");
        html.append("          </tr>\n");
        html.append("        </thead>\n");
        html.append("        <tbody>\n");
        for (QuoteStatistics.MakeModelChassisSummary summary : data) {
            html.append("          <tr>\n");
            html.append("            <td>")
                    .append(escapeHtml(summary.getMake()))
                    .append("</td>\n");
            html.append("            <td>")
                    .append(escapeHtml(summary.getModel()))
                    .append("</td>\n");
            html.append("            <td class=\"numeric\">")
                    .append(escapeHtml(formatInteger(summary.getUniqueChassisCount())))
                    .append("</td>\n");
            html.append("            <td class=\"numeric\">")
                    .append(escapeHtml(formatPercentage(
                            summary.getSuccessfulUniqueChassisCount(),
                            summary.getUniqueChassisCount())))
                    .append("</td>\n");
            html.append("            <td class=\"numeric\">")
                    .append(escapeHtml(formatPercentage(
                            summary.getFailedUniqueChassisCount(),
                            summary.getUniqueChassisCount())))
                    .append("</td>\n");
            html.append("          </tr>\n");
        }
        html.append("        </tbody>\n");
        html.append("      </table>\n");
        html.append("    </div>\n");
    }

    private void appendErrorTable(StringBuilder html, String heading, Map<String, Long> errorCounts) {
        html.append("    <div class=\"table-card\">\n");
        html.append("      <h3>")
                .append(escapeHtml(heading))
                .append("</h3>\n");
        if (errorCounts.isEmpty()) {
            html.append("      <p class=\"empty-state\">No errors recorded.</p>\n");
            html.append("    </div>\n");
            return;
        }
        html.append("      <table>\n");
        html.append("        <thead>\n");
        html.append("          <tr>\n");
        html.append("            <th scope=\"col\">Error</th>\n");
        html.append("            <th scope=\"col\" class=\"numeric\">Count</th>\n");
        html.append("          </tr>\n");
        html.append("        </thead>\n");
        html.append("        <tbody>\n");
        for (Map.Entry<String, Long> entry : errorCounts.entrySet()) {
            html.append("          <tr>\n");
            html.append("            <td>")
                    .append(escapeHtml(entry.getKey()))
                    .append("</td>\n");
            html.append("            <td class=\"numeric\">")
                    .append(escapeHtml(formatInteger(entry.getValue())))
                    .append("</td>\n");
            html.append("          </tr>\n");
        }
        html.append("        </tbody>\n");
        html.append("      </table>\n");
        html.append("    </div>\n");
    }

    private String buildScripts(QuoteStatistics statistics,
                                Map<String, QuoteStatistics.OutcomeBreakdown> tplSpecSummary,
                                Map<String, QuoteStatistics.OutcomeBreakdown> compSpecSummary) {
        QuoteGroupStats tplStats = statistics.getTplStats();
        QuoteGroupStats compStats = statistics.getComprehensiveStats();
        long tplUniqueChassisSuccess = statistics.getTplUniqueChassisSuccessCount();
        long tplUniqueChassisFail = statistics.getTplUniqueChassisFailCount();
        long compUniqueChassisSuccess = statistics.getComprehensiveUniqueChassisSuccessCount();
        long compUniqueChassisFail = statistics.getComprehensiveUniqueChassisFailCount();

        Map<String, QuoteStatistics.OutcomeBreakdown> tplBodyOutcomes = statistics.getTplBodyCategoryOutcomes();
        Map<String, QuoteStatistics.OutcomeBreakdown> compBodyOutcomes = statistics.getComprehensiveBodyCategoryOutcomes();
        List<QuoteStatistics.AgeRangeStats> tplAgeStats = statistics.getTplAgeRangeStats();
        List<QuoteStatistics.AgeRangeStats> compAgeStats = statistics.getComprehensiveAgeRangeStats();
        List<QuoteStatistics.ManufactureYearStats> tplManufactureYearStats =
                statistics.getTplManufactureYearStats();
        List<QuoteStatistics.ManufactureYearStats> compManufactureYearStats =
                statistics.getComprehensiveManufactureYearStats();
        List<QuoteStatistics.ValueRangeStats> compValueStats = statistics.getComprehensiveEstimatedValueStats();
        List<QuoteStatistics.CategoryCount> overallManufactureYearTrend = statistics.getManufactureYearTrend();
        List<QuoteStatistics.CategoryCount> overallCustomerAgeTrend = statistics.getCustomerAgeTrend();

        QuoteStatistics.OutcomeBreakdown tplGccBreakdown =
                tplSpecSummary.getOrDefault(SPEC_GCC_LABEL, EMPTY_OUTCOME);
        QuoteStatistics.OutcomeBreakdown tplNonGccBreakdown =
                tplSpecSummary.getOrDefault(SPEC_NON_GCC_LABEL, EMPTY_OUTCOME);
        QuoteStatistics.OutcomeBreakdown compGccBreakdown =
                compSpecSummary.getOrDefault(SPEC_GCC_LABEL, EMPTY_OUTCOME);
        QuoteStatistics.OutcomeBreakdown compNonGccBreakdown =
                compSpecSummary.getOrDefault(SPEC_NON_GCC_LABEL, EMPTY_OUTCOME);
        long tplSpecGccSuccess = tplGccBreakdown.getSuccessCount();
        long tplSpecGccFailure = tplGccBreakdown.getFailureCount();
        long tplSpecNonGccSuccess = tplNonGccBreakdown.getSuccessCount();
        long tplSpecNonGccFailure = tplNonGccBreakdown.getFailureCount();
        long compSpecGccSuccess = compGccBreakdown.getSuccessCount();
        long compSpecGccFailure = compGccBreakdown.getFailureCount();
        long compSpecNonGccSuccess = compNonGccBreakdown.getSuccessCount();
        long compSpecNonGccFailure = compNonGccBreakdown.getFailureCount();

        List<String> tplBodyLabels = new ArrayList<>(tplBodyOutcomes.keySet());
        List<Long> tplBodySuccessValues = new ArrayList<>();
        List<Long> tplBodyFailureValues = new ArrayList<>();
        if (tplBodyLabels.isEmpty()) {
            tplBodyLabels.add("No Data");
            tplBodySuccessValues.add(0L);
            tplBodyFailureValues.add(0L);
        } else {
            for (QuoteStatistics.OutcomeBreakdown breakdown : tplBodyOutcomes.values()) {
                tplBodySuccessValues.add(breakdown.getSuccessCount());
                tplBodyFailureValues.add(breakdown.getFailureCount());
            }
        }

        List<String> compBodyLabels = new ArrayList<>(compBodyOutcomes.keySet());
        List<Long> compBodySuccessValues = new ArrayList<>();
        List<Long> compBodyFailureValues = new ArrayList<>();
        if (compBodyLabels.isEmpty()) {
            compBodyLabels.add("No Data");
            compBodySuccessValues.add(0L);
            compBodyFailureValues.add(0L);
        } else {
            for (QuoteStatistics.OutcomeBreakdown breakdown : compBodyOutcomes.values()) {
                compBodySuccessValues.add(breakdown.getSuccessCount());
                compBodyFailureValues.add(breakdown.getFailureCount());
            }
        }

        List<String> tplAgeLabels = new ArrayList<>();
        List<Double> tplAgeSuccessRatios = new ArrayList<>();
        List<Double> tplAgeFailureRatios = new ArrayList<>();
        if (tplAgeStats.isEmpty()) {
            tplAgeLabels.add("No Data");
            tplAgeSuccessRatios.add(0.0);
            tplAgeFailureRatios.add(0.0);
        } else {
            for (QuoteStatistics.AgeRangeStats stat : tplAgeStats) {
                tplAgeLabels.add(stat.getLabel());
                tplAgeSuccessRatios.add(stat.getSuccessRatio());
                tplAgeFailureRatios.add(stat.getFailureRatio());
            }
        }

        List<String> compAgeLabels = new ArrayList<>();
        List<Double> compAgeSuccessRatios = new ArrayList<>();
        List<Double> compAgeFailureRatios = new ArrayList<>();
        if (compAgeStats.isEmpty()) {
            compAgeLabels.add("No Data");
            compAgeSuccessRatios.add(0.0);
            compAgeFailureRatios.add(0.0);
        } else {
            for (QuoteStatistics.AgeRangeStats stat : compAgeStats) {
                compAgeLabels.add(stat.getLabel());
                compAgeSuccessRatios.add(stat.getSuccessRatio());
                compAgeFailureRatios.add(stat.getFailureRatio());
            }
        }

        List<String> compValueLabels = new ArrayList<>();
        List<Double> compValueSuccessRatios = new ArrayList<>();
        List<Double> compValueFailureRatios = new ArrayList<>();
        boolean hasCompValueData = false;
        for (QuoteStatistics.ValueRangeStats stat : compValueStats) {
            if (stat.getProcessedTotal() == 0) {
                continue;
            }
            hasCompValueData = true;
            compValueLabels.add(stat.getLabel());
            compValueSuccessRatios.add(stat.getSuccessRatio());
            compValueFailureRatios.add(stat.getFailureRatio());
        }
        if (!hasCompValueData) {
            compValueLabels.add("No Data");
            compValueSuccessRatios.add(0.0);
            compValueFailureRatios.add(0.0);
        }

        List<String> tplManufactureYearLabels = new ArrayList<>();
        List<Double> tplManufactureYearSuccessRatios = new ArrayList<>();
        List<Double> tplManufactureYearFailureRatios = new ArrayList<>();
        if (tplManufactureYearStats.isEmpty()) {
            tplManufactureYearLabels.add("No Data");
            tplManufactureYearSuccessRatios.add(0.0);
            tplManufactureYearFailureRatios.add(0.0);
        } else {
            for (QuoteStatistics.ManufactureYearStats stat : tplManufactureYearStats) {
                tplManufactureYearLabels.add(stat.getLabel());
                tplManufactureYearSuccessRatios.add(stat.getSuccessRatio());
                tplManufactureYearFailureRatios.add(stat.getFailureRatio());
            }
        }

        List<String> compManufactureYearLabels = new ArrayList<>();
        List<Double> compManufactureYearSuccessRatios = new ArrayList<>();
        List<Double> compManufactureYearFailureRatios = new ArrayList<>();
        if (compManufactureYearStats.isEmpty()) {
            compManufactureYearLabels.add("No Data");
            compManufactureYearSuccessRatios.add(0.0);
            compManufactureYearFailureRatios.add(0.0);
        } else {
            for (QuoteStatistics.ManufactureYearStats stat : compManufactureYearStats) {
                compManufactureYearLabels.add(stat.getLabel());
                compManufactureYearSuccessRatios.add(stat.getSuccessRatio());
                compManufactureYearFailureRatios.add(stat.getFailureRatio());
            }
        }

        List<String> overallManufactureYearLabels = new ArrayList<>();
        List<Long> overallManufactureYearCounts = new ArrayList<>();
        if (overallManufactureYearTrend.isEmpty()) {
            overallManufactureYearLabels.add("No Data");
            overallManufactureYearCounts.add(0L);
        } else {
            for (QuoteStatistics.CategoryCount point : overallManufactureYearTrend) {
                overallManufactureYearLabels.add(point.getLabel());
                overallManufactureYearCounts.add(point.getCount());
            }
        }

        List<String> overallCustomerAgeLabels = new ArrayList<>();
        List<Long> overallCustomerAgeCounts = new ArrayList<>();
        if (overallCustomerAgeTrend.isEmpty()) {
            overallCustomerAgeLabels.add("No Data");
            overallCustomerAgeCounts.add(0L);
        } else {
            for (QuoteStatistics.CategoryCount point : overallCustomerAgeTrend) {
                overallCustomerAgeLabels.add(point.getLabel());
                overallCustomerAgeCounts.add(point.getCount());
            }
        }

        StringBuilder script = new StringBuilder();
        script.append("<script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js\"></script>\n");
        script.append("<script>\n");
        script.append("  const sharedOptions = {\n");
        script.append("    responsive: true,\n");
        script.append("    scales: {\n");
        script.append("      y: {\n");
        script.append("        beginAtZero: true,\n");
        script.append("        ticks: { precision: 0 }\n");
        script.append("      }\n");
        script.append("    },\n");
        script.append("    plugins: {\n");
        script.append("      legend: { display: false },\n");
        script.append("      tooltip: { callbacks: { label: ctx => `${ctx.dataset.label}: ${ctx.raw.toLocaleString()}` } }\n");
        script.append("    }\n");
        script.append("  };\n");
        script.append("  const ratioChartOptions = {\n");
        script.append("    responsive: true,\n");
        script.append("    interaction: { mode: 'index', intersect: false },\n");
        script.append("    scales: { y: { beginAtZero: true, max: 100, ticks: { callback: value => `${value}%` } } },\n");
        script.append("    plugins: { tooltip: { callbacks: { label: ctx => `${ctx.dataset.label}: ${ctx.raw.toFixed(1)}%` } } }\n");
        script.append("  };\n");
        script.append("  const trendLineOptions = {\n");
        script.append("    responsive: true,\n");
        script.append("    interaction: { mode: 'index', intersect: false },\n");
        script.append("    scales: { y: { beginAtZero: true, ticks: { callback: value => value.toLocaleString() } } },\n");
        script.append("    plugins: { tooltip: { callbacks: { label: ctx => `${ctx.dataset.label}: ${ctx.raw.toLocaleString()}` } } }\n");
        script.append("  };\n");
        script.append("  const tplData = {\n");
        script.append("    labels: ['Success', 'Failed'],\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'Quotes',\n");
        script.append("      data: [").append(tplStats.getPassCount()).append(',').append(tplStats.getFailCount()).append("],\n");
        script.append("      backgroundColor: ['#16a34a', '#dc2626'],\n");
        script.append("      borderRadius: 8\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const compData = {\n");
        script.append("    labels: ['Success', 'Failed'],\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'Quotes',\n");
        script.append("      data: [").append(compStats.getPassCount()).append(',').append(compStats.getFailCount()).append("],\n");
        script.append("      backgroundColor: ['#16a34a', '#dc2626'],\n");
        script.append("      borderRadius: 8\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const overallManufactureYearData = {\n");
        script.append("    labels: ").append(toJsStringArray(overallManufactureYearLabels)).append(",\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'Quotes',\n");
        script.append("      data: ").append(toJsNumberArray(overallManufactureYearCounts)).append(",\n");
        script.append("      borderColor: '#2563eb',\n");
        script.append("      backgroundColor: 'rgba(37, 99, 235, 0.15)',\n");
        script.append("      tension: 0.35,\n");
        script.append("      fill: true\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const overallCustomerAgeData = {\n");
        script.append("    labels: ").append(toJsStringArray(overallCustomerAgeLabels)).append(",\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'Quotes',\n");
        script.append("      data: ").append(toJsNumberArray(overallCustomerAgeCounts)).append(",\n");
        script.append("      borderColor: '#f97316',\n");
        script.append("      backgroundColor: 'rgba(249, 115, 22, 0.15)',\n");
        script.append("      tension: 0.35,\n");
        script.append("      fill: true\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const tplUniqueChassisData = {\n");
        script.append("    labels: ['Success', 'Failed'],\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'Unique chassis',\n");
        script.append("      data: [").append(tplUniqueChassisSuccess).append(',').append(tplUniqueChassisFail).append("],\n");
        script.append("      backgroundColor: ['#16a34a', '#dc2626'],\n");
        script.append("      borderRadius: 8\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const compUniqueChassisData = {\n");
        script.append("    labels: ['Success', 'Failed'],\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'Unique chassis',\n");
        script.append("      data: [").append(compUniqueChassisSuccess).append(',').append(compUniqueChassisFail).append("],\n");
        script.append("      backgroundColor: ['#16a34a', '#dc2626'],\n");
        script.append("      borderRadius: 8\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const tplBodyLabels = ").append(toJsStringArray(tplBodyLabels)).append(";\n");
        script.append("  const tplBodySuccessData = {\n");
        script.append("    labels: tplBodyLabels,\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'Successful Requests',\n");
        script.append("      data: ").append(toJsNumberArray(tplBodySuccessValues)).append(",\n");
        script.append("      backgroundColor: '#16a34a',\n");
        script.append("      borderRadius: 8\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const tplBodyFailureData = {\n");
        script.append("    labels: tplBodyLabels,\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'Failed Requests',\n");
        script.append("      data: ").append(toJsNumberArray(tplBodyFailureValues)).append(",\n");
        script.append("      backgroundColor: '#dc2626',\n");
        script.append("      borderRadius: 8\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const compBodyLabels = ").append(toJsStringArray(compBodyLabels)).append(";\n");
        script.append("  const compBodySuccessData = {\n");
        script.append("    labels: compBodyLabels,\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'Successful Requests',\n");
        script.append("      data: ").append(toJsNumberArray(compBodySuccessValues)).append(",\n");
        script.append("      backgroundColor: '#16a34a',\n");
        script.append("      borderRadius: 8\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const compBodyFailureData = {\n");
        script.append("    labels: compBodyLabels,\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'Failed Requests',\n");
        script.append("      data: ").append(toJsNumberArray(compBodyFailureValues)).append(",\n");
        script.append("      backgroundColor: '#dc2626',\n");
        script.append("      borderRadius: 8\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const tplSpecGccData = {\n");
        script.append("    labels: ['Success', 'Failed'],\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'GCC',\n");
        script.append("      data: [").append(tplSpecGccSuccess).append(',').append(tplSpecGccFailure).append("],\n");
        script.append("      backgroundColor: ['#16a34a', '#dc2626'],\n");
        script.append("      borderRadius: 8\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const tplSpecNonGccData = {\n");
        script.append("    labels: ['Success', 'Failed'],\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'Non GCC',\n");
        script.append("      data: [").append(tplSpecNonGccSuccess).append(',').append(tplSpecNonGccFailure).append("],\n");
        script.append("      backgroundColor: ['#16a34a', '#dc2626'],\n");
        script.append("      borderRadius: 8\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const compSpecGccData = {\n");
        script.append("    labels: ['Success', 'Failed'],\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'GCC',\n");
        script.append("      data: [").append(compSpecGccSuccess).append(',').append(compSpecGccFailure).append("],\n");
        script.append("      backgroundColor: ['#16a34a', '#dc2626'],\n");
        script.append("      borderRadius: 8\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const compSpecNonGccData = {\n");
        script.append("    labels: ['Success', 'Failed'],\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'Non GCC',\n");
        script.append("      data: [").append(compSpecNonGccSuccess).append(',').append(compSpecNonGccFailure).append("],\n");
        script.append("      backgroundColor: ['#16a34a', '#dc2626'],\n");
        script.append("      borderRadius: 8\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const tplManufactureYearData = {\n");
        script.append("    labels: ").append(toJsStringArray(tplManufactureYearLabels)).append(",\n");
        script.append("    datasets: [\n");
        script.append("      {\n");
        script.append("        label: 'Success %',\n");
        script.append("        data: ").append(toJsDoubleArray(tplManufactureYearSuccessRatios)).append(",\n");
        script.append("        borderColor: '#16a34a',\n");
        script.append("        backgroundColor: 'rgba(22, 163, 74, 0.15)',\n");
        script.append("        tension: 0.35,\n");
        script.append("        fill: true\n");
        script.append("      },\n");
        script.append("      {\n");
        script.append("        label: 'Failure %',\n");
        script.append("        data: ").append(toJsDoubleArray(tplManufactureYearFailureRatios)).append(",\n");
        script.append("        borderColor: '#dc2626',\n");
        script.append("        backgroundColor: 'rgba(220, 38, 38, 0.15)',\n");
        script.append("        tension: 0.35,\n");
        script.append("        fill: true\n");
        script.append("      }\n");
        script.append("    ]\n");
        script.append("  };\n");
        script.append("  const compManufactureYearData = {\n");
        script.append("    labels: ").append(toJsStringArray(compManufactureYearLabels)).append(",\n");
        script.append("    datasets: [\n");
        script.append("      {\n");
        script.append("        label: 'Success %',\n");
        script.append("        data: ").append(toJsDoubleArray(compManufactureYearSuccessRatios)).append(",\n");
        script.append("        borderColor: '#16a34a',\n");
        script.append("        backgroundColor: 'rgba(22, 163, 74, 0.15)',\n");
        script.append("        tension: 0.35,\n");
        script.append("        fill: true\n");
        script.append("      },\n");
        script.append("      {\n");
        script.append("        label: 'Failure %',\n");
        script.append("        data: ").append(toJsDoubleArray(compManufactureYearFailureRatios)).append(",\n");
        script.append("        borderColor: '#dc2626',\n");
        script.append("        backgroundColor: 'rgba(220, 38, 38, 0.15)',\n");
        script.append("        tension: 0.35,\n");
        script.append("        fill: true\n");
        script.append("      }\n");
        script.append("    ]\n");
        script.append("  };\n");
        script.append("  const tplAgeRatioData = {\n");
        script.append("    labels: ").append(toJsStringArray(tplAgeLabels)).append(",\n");
        script.append("    datasets: [\n");
        script.append("      {\n");
        script.append("        label: 'Success %',\n");
        script.append("        data: ").append(toJsDoubleArray(tplAgeSuccessRatios)).append(",\n");
        script.append("        borderColor: '#16a34a',\n");
        script.append("        backgroundColor: 'rgba(22, 163, 74, 0.15)',\n");
        script.append("        tension: 0.35,\n");
        script.append("        fill: true\n");
        script.append("      },\n");
        script.append("      {\n");
        script.append("        label: 'Failure %',\n");
        script.append("        data: ").append(toJsDoubleArray(tplAgeFailureRatios)).append(",\n");
        script.append("        borderColor: '#dc2626',\n");
        script.append("        backgroundColor: 'rgba(220, 38, 38, 0.15)',\n");
        script.append("        tension: 0.35,\n");
        script.append("        fill: true\n");
        script.append("      }\n");
        script.append("    ]\n");
        script.append("  };\n");
        script.append("  const compAgeRatioData = {\n");
        script.append("    labels: ").append(toJsStringArray(compAgeLabels)).append(",\n");
        script.append("    datasets: [\n");
        script.append("      {\n");
        script.append("        label: 'Success %',\n");
        script.append("        data: ").append(toJsDoubleArray(compAgeSuccessRatios)).append(",\n");
        script.append("        borderColor: '#16a34a',\n");
        script.append("        backgroundColor: 'rgba(22, 163, 74, 0.15)',\n");
        script.append("        tension: 0.35,\n");
        script.append("        fill: true\n");
        script.append("      },\n");
        script.append("      {\n");
        script.append("        label: 'Failure %',\n");
        script.append("        data: ").append(toJsDoubleArray(compAgeFailureRatios)).append(",\n");
        script.append("        borderColor: '#dc2626',\n");
        script.append("        backgroundColor: 'rgba(220, 38, 38, 0.15)',\n");
        script.append("        tension: 0.35,\n");
        script.append("        fill: true\n");
        script.append("      }\n");
        script.append("    ]\n");
        script.append("  };\n");
        script.append("  const compEstimatedValueData = {\n");
        script.append("    labels: ").append(toJsStringArray(compValueLabels)).append(",\n");
        script.append("    datasets: [\n");
        script.append("      {\n");
        script.append("        label: 'Success %',\n");
        script.append("        data: ").append(toJsDoubleArray(compValueSuccessRatios)).append(",\n");
        script.append("        borderColor: '#2563eb',\n");
        script.append("        backgroundColor: 'rgba(37, 99, 235, 0.15)',\n");
        script.append("        tension: 0.35,\n");
        script.append("        fill: true\n");
        script.append("      },\n");
        script.append("      {\n");
        script.append("        label: 'Failure %',\n");
        script.append("        data: ").append(toJsDoubleArray(compValueFailureRatios)).append(",\n");
        script.append("        borderColor: '#f97316',\n");
        script.append("        backgroundColor: 'rgba(249, 115, 22, 0.15)',\n");
        script.append("        tension: 0.35,\n");
        script.append("        fill: true\n");
        script.append("      }\n");
        script.append("    ]\n");
        script.append("  };\n");
        script.append("  new Chart(document.getElementById('tplOutcomesChart'), { type: 'bar', data: tplData, options: sharedOptions });\n");
        script.append("  new Chart(document.getElementById('compOutcomesChart'), { type: 'bar', data: compData, options: sharedOptions });\n");
        script.append("  new Chart(document.getElementById('tplUniqueChassisChart'), { type: 'bar', data: tplUniqueChassisData, options: sharedOptions });\n");
        script.append("  new Chart(document.getElementById('compUniqueChassisChart'), { type: 'bar', data: compUniqueChassisData, options: sharedOptions });\n");
        script.append("  new Chart(document.getElementById('tplBodySuccessChart'), { type: 'bar', data: tplBodySuccessData, options: sharedOptions });\n");
        script.append("  new Chart(document.getElementById('tplBodyFailureChart'), { type: 'bar', data: tplBodyFailureData, options: sharedOptions });\n");
        script.append("  new Chart(document.getElementById('tplSpecGccChart'), { type: 'bar', data: tplSpecGccData, options: sharedOptions });\n");
        script.append("  new Chart(document.getElementById('tplSpecNonGccChart'), { type: 'bar', data: tplSpecNonGccData, options: sharedOptions });\n");
        script.append("  new Chart(document.getElementById('compBodySuccessChart'), { type: 'bar', data: compBodySuccessData, options: sharedOptions });\n");
        script.append("  new Chart(document.getElementById('compBodyFailureChart'), { type: 'bar', data: compBodyFailureData, options: sharedOptions });\n");
        script.append("  new Chart(document.getElementById('compSpecGccChart'), { type: 'bar', data: compSpecGccData, options: sharedOptions });\n");
        script.append("  new Chart(document.getElementById('compSpecNonGccChart'), { type: 'bar', data: compSpecNonGccData, options: sharedOptions });\n");
        script.append("  new Chart(document.getElementById('tplManufactureYearChart'), {\n");
        script.append("    type: 'line',\n");
        script.append("    data: tplManufactureYearData,\n");
        script.append("    options: ratioChartOptions\n");
        script.append("  });\n");
        script.append("  new Chart(document.getElementById('compManufactureYearChart'), {\n");
        script.append("    type: 'line',\n");
        script.append("    data: compManufactureYearData,\n");
        script.append("    options: ratioChartOptions\n");
        script.append("  });\n");
        script.append("  new Chart(document.getElementById('tplAgeRatioChart'), {\n");
        script.append("    type: 'line',\n");
        script.append("    data: tplAgeRatioData,\n");
        script.append("    options: ratioChartOptions\n");
        script.append("  });\n");
        script.append("  new Chart(document.getElementById('compAgeRatioChart'), {\n");
        script.append("    type: 'line',\n");
        script.append("    data: compAgeRatioData,\n");
        script.append("    options: ratioChartOptions\n");
        script.append("  });\n");
        script.append("  new Chart(document.getElementById('compEstimatedValueChart'), {\n");
        script.append("    type: 'line',\n");
        script.append("    data: compEstimatedValueData,\n");
        script.append("    options: ratioChartOptions\n");
        script.append("  });\n");
        script.append("  new Chart(document.getElementById('overallManufactureYearChart'), {\n");
        script.append("    type: 'line',\n");
        script.append("    data: overallManufactureYearData,\n");
        script.append("    options: trendLineOptions\n");
        script.append("  });\n");
        script.append("  new Chart(document.getElementById('overallCustomerAgeChart'), {\n");
        script.append("    type: 'line',\n");
        script.append("    data: overallCustomerAgeData,\n");
        script.append("    options: trendLineOptions\n");
        script.append("  });\n");
        script.append("  document.querySelectorAll('.nav-button').forEach(button => {\n");
        script.append("    button.addEventListener('click', () => {\n");
        script.append("      document.querySelectorAll('.nav-button').forEach(btn => btn.classList.remove('active'));\n");
        script.append("      document.querySelectorAll('.page-section').forEach(section => section.classList.remove('active'));\n");
        script.append("      button.classList.add('active');\n");
        script.append("      const target = button.getAttribute('data-target');\n");
        script.append("      const section = document.getElementById(target);\n");
        script.append("      if (section) {\n");
        script.append("        section.classList.add('active');\n");
        script.append("        section.scrollIntoView({ behavior: 'smooth', block: 'start' });\n");
        script.append("      }\n");
        script.append("    });\n");
        script.append("  });\n");
        script.append("</script>\n");
        return script.toString();
    }

    private static String formatPercentage(long numerator, long denominator) {
        if (denominator == 0) {
            return PERCENT_FORMAT.format(0);
        }
        double ratio = (double) numerator / denominator;
        return PERCENT_FORMAT.format(ratio);
    }

    private static String toJsStringLiteral(String value) {
        if (value == null) {
            return "''";
        }
        StringBuilder builder = new StringBuilder("'");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\'':
                    builder.append("\\'");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    builder.append(ch);
            }
        }
        builder.append("'");
        return builder.toString();
    }

    private static String toJsStringArray(Collection<String> values) {
        return values.stream()
                .map(HtmlReportGenerator::toJsStringLiteral)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String toJsNumberArray(List<? extends Number> values) {
        return values.stream()
                .map(number -> number == null ? "0" : number.toString())
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String toJsDoubleArray(List<Double> values) {
        return values.stream()
                .map(value -> String.format(Locale.US, "%.1f", value))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            switch (ch) {
                case '&':
                    builder.append("&amp;");
                    break;
                case '<':
                    builder.append("&lt;");
                    break;
                case '>':
                    builder.append("&gt;");
                    break;
                case '"':
                    builder.append("&quot;");
                    break;
                case '\'':
                    builder.append("&#39;");
                    break;
                default:
                    builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static String formatInteger(long value) {
        return INTEGER_FORMAT.format(value);
    }
}
