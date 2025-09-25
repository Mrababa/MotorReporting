package com.example.motorreporting;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private static final DecimalFormat CURRENCY_FORMAT;
    private static final DateTimeFormatter HEADER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);
    private static final String SPEC_GCC_LABEL = "GCC";
    private static final String SPEC_NON_GCC_LABEL = "Non GCC";
    private static final String SPEC_UNKNOWN_LABEL = "Unknown";
    private static final QuoteStatistics.OutcomeBreakdown EMPTY_OUTCOME =
            new QuoteStatistics.OutcomeBreakdown(0, 0);

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        INTEGER_FORMAT = new DecimalFormat("#,##0", symbols);
        PERCENT_FORMAT = new DecimalFormat("#0.0%", symbols);
        CURRENCY_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    public void generate(Path outputPath,
                         QuoteStatistics statistics,
                         List<QuoteRecord> records,
                         ReportDateRange reportDateRange) throws IOException {
        Objects.requireNonNull(outputPath, "outputPath");
        Objects.requireNonNull(statistics, "statistics");
        Objects.requireNonNull(records, "records");
        Objects.requireNonNull(reportDateRange, "reportDateRange");

        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String html = buildHtml(statistics, records, reportDateRange);
        Files.writeString(outputPath, html, StandardCharsets.UTF_8);
    }

    public void generate(Path outputPath,
                         QuoteStatistics statistics,
                         List<QuoteRecord> records) throws IOException {
        generate(outputPath, statistics, records,
                ReportDateRange.of(Optional.empty(), Optional.empty()));
    }

    private String buildHtml(QuoteStatistics statistics,
                             List<QuoteRecord> records,
                             ReportDateRange reportDateRange) {
        QuoteGroupStats tplStats = statistics.getTplStats();
        QuoteGroupStats compStats = statistics.getComprehensiveStats();
        QuoteStatistics.UniqueRequestSummary overallUniqueRequests = statistics.getOverallUniqueRequests();
        Map<String, QuoteStatistics.OutcomeBreakdown> tplSpecificationSummary =
                aggregateSpecificationOutcomes(statistics.getTplSpecificationOutcomes());
        Map<String, QuoteStatistics.OutcomeBreakdown> compSpecificationSummary =
                aggregateSpecificationOutcomes(statistics.getComprehensiveSpecificationOutcomes());

        long totalQuotes = overallUniqueRequests.getTotalRequests();
        long successCount = overallUniqueRequests.getSuccessCount();
        long failCount = overallUniqueRequests.getFailureCount();
        long uniqueChassisTotal = statistics.getUniqueChassisCount();
        QuoteStatistics.EidChassisSummary tplEidChassisSummary = statistics.getTplEidChassisSummary();
        long tplEidChassisTotal = tplEidChassisSummary.getTotalRequests();
        long tplEidChassisUnique = tplEidChassisSummary.getUniqueRequests();
        long tplEidChassisDuplicates = tplEidChassisSummary.getDuplicateRequests();
        String headerText = buildHeaderText(records, reportDateRange);
        Optional<String> insuranceCompanyName = findInsuranceCompanyName(records);

        long tplPoliciesSold = statistics.getTplPoliciesSold();
        BigDecimal tplTotalPremium = statistics.getTplTotalPremium();
        double tplChineseSalesRatio = statistics.getTplChineseSalesRatio();
        double tplElectricSalesRatio = statistics.getTplElectricSalesRatio();
        List<QuoteStatistics.SalesPremiumBreakdown> tplBodyPremiumBreakdowns =
                statistics.getTplBodyTypePremiums();
        List<QuoteStatistics.MakeModelPremiumSummary> tplTopModelsByPremium =
                statistics.getTplTopModelsByPremium();
        long compPoliciesSold = statistics.getComprehensivePoliciesSold();
        BigDecimal compTotalPremium = statistics.getComprehensiveTotalPremium();
        double compChineseSalesRatio = statistics.getComprehensiveChineseSalesRatio();
        double compElectricSalesRatio = statistics.getComprehensiveElectricSalesRatio();
        List<QuoteStatistics.SalesPremiumBreakdown> compBodyPremiumBreakdowns =
                statistics.getComprehensiveBodyTypePremiums();
        List<QuoteStatistics.MakeModelPremiumSummary> compTopModelsByPremium =
                statistics.getComprehensiveTopModelsByPremium();

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
        html.append("        .company-identity {\n");
        html.append("            display: flex;\n");
        html.append("            align-items: center;\n");
        html.append("            justify-content: center;\n");
        html.append("            gap: 1.25rem;\n");
        html.append("            flex-wrap: wrap;\n");
        html.append("            margin-bottom: 1.75rem;\n");
        html.append("        }\n");
        html.append("        .company-logo {\n");
        html.append("            width: clamp(140px, 18vw, 180px);\n");
        html.append("            height: auto;\n");
        html.append("        }\n");
        html.append("        .company-name-wrapper {\n");
        html.append("            text-align: left;\n");
        html.append("        }\n");
        html.append("        .company-label {\n");
        html.append("            display: block;\n");
        html.append("            text-transform: uppercase;\n");
        html.append("            letter-spacing: 0.08em;\n");
        html.append("            font-size: 0.75rem;\n");
        html.append("            font-weight: 600;\n");
        html.append("            color: #6b7280;\n");
        html.append("            margin-bottom: 0.35rem;\n");
        html.append("        }\n");
        html.append("        .company-name {\n");
        html.append("            display: block;\n");
        html.append("            font-size: 1.2rem;\n");
        html.append("            font-weight: 600;\n");
        html.append("            color: #0d1b3e;\n");
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
        html.append("        .chart-card.chart-card--wide {\n");
        html.append("            grid-column: 1 / -1;\n");
        html.append("        }\n");
        html.append("        .chart-card.chart-card--wide canvas {\n");
        html.append("            height: 420px;\n");
        html.append("        }\n");
        html.append("        .chart-card.chart-card--split {\n");
        html.append("            display: flex;\n");
        html.append("            flex-wrap: wrap;\n");
        html.append("            gap: 1.75rem;\n");
        html.append("            align-items: stretch;\n");
        html.append("        }\n");
        html.append("        .chart-card__chart {\n");
        html.append("            flex: 2 1 360px;\n");
        html.append("        }\n");
        html.append("        .chart-card__chart canvas {\n");
        html.append("            height: 360px;\n");
        html.append("        }\n");
        html.append("        .chart-card__table {\n");
        html.append("            flex: 1 1 260px;\n");
        html.append("            display: flex;\n");
        html.append("            flex-direction: column;\n");
        html.append("        }\n");
        html.append("        .chart-card__table h3 {\n");
        html.append("            margin: 0 0 0.85rem;\n");
        html.append("            font-size: 1rem;\n");
        html.append("            font-weight: 600;\n");
        html.append("            color: #1f2937;\n");
        html.append("        }\n");
        html.append("        .conversion-table {\n");
        html.append("            width: 100%;\n");
        html.append("            border-collapse: collapse;\n");
        html.append("        }\n");
        html.append("        .conversion-table th,\n");
        html.append("        .conversion-table td {\n");
        html.append("            padding: 0.55rem 0.75rem;\n");
        html.append("            border-bottom: 1px solid #e5e7eb;\n");
        html.append("            font-size: 0.9rem;\n");
        html.append("            text-align: left;\n");
        html.append("        }\n");
        html.append("        .conversion-table th.numeric,\n");
        html.append("        .conversion-table td.numeric {\n");
        html.append("            text-align: right;\n");
        html.append("        }\n");
        html.append("        .conversion-table tbody tr:last-child td {\n");
        html.append("            border-bottom: none;\n");
        html.append("        }\n");
        html.append("        .chart-card h2 {\n");
        html.append("            margin: 0 0 1rem;\n");
        html.append("            font-size: 1.1rem;\n");
        html.append("            font-weight: 700;\n");
        html.append("            color: #0d1b3e;\n");
        html.append("        }\n");
        html.append("        .chart-card__header {\n");
        html.append("            display: flex;\n");
        html.append("            align-items: center;\n");
        html.append("            justify-content: space-between;\n");
        html.append("            gap: 1rem;\n");
        html.append("            margin-bottom: 1rem;\n");
        html.append("        }\n");
        html.append("        .chart-card__header h2 {\n");
        html.append("            margin: 0;\n");
        html.append("        }\n");
        html.append("        .chart-toggle {\n");
        html.append("            display: inline-flex;\n");
        html.append("            background-color: #e5e7eb;\n");
        html.append("            border-radius: 9999px;\n");
        html.append("            padding: 0.25rem;\n");
        html.append("        }\n");
        html.append("        .chart-toggle__button {\n");
        html.append("            border: none;\n");
        html.append("            background: transparent;\n");
        html.append("            padding: 0.35rem 0.9rem;\n");
        html.append("            border-radius: 9999px;\n");
        html.append("            font-size: 0.85rem;\n");
        html.append("            font-weight: 600;\n");
        html.append("            color: #1f2937;\n");
        html.append("            cursor: pointer;\n");
        html.append("            transition: background-color 0.2s ease, color 0.2s ease;\n");
        html.append("        }\n");
        html.append("        .chart-toggle__button.active {\n");
        html.append("            background-color: #2563eb;\n");
        html.append("            color: #ffffff;\n");
        html.append("        }\n");
        html.append("        .chart-toggle__button:focus-visible {\n");
        html.append("            outline: 2px solid #1d4ed8;\n");
        html.append("            outline-offset: 2px;\n");
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
        html.append("        .table-grid {\n");
        html.append("            display: grid;\n");
        html.append("            gap: 1.5rem;\n");
        html.append("            margin-top: 2rem;\n");
        html.append("            grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));\n");
        html.append("        }\n");
        html.append("        .table-subtext {\n");
        html.append("            display: block;\n");
        html.append("            margin-top: 0.35rem;\n");
        html.append("            font-size: 0.85rem;\n");
        html.append("            color: #6b7280;\n");
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
        html.append("            .chart-card.chart-card--split {\n");
        html.append("                flex-direction: column;\n");
        html.append("            }\n");
        html.append("            .chart-card__chart,\n");
        html.append("            .chart-card__table {\n");
        html.append("                flex: 1 1 auto;\n");
        html.append("            }\n");
        html.append("            canvas {\n");
        html.append("                height: 260px;\n");
        html.append("            }\n");
        html.append("            .chart-card.chart-card--wide canvas {\n");
        html.append("                height: 320px;\n");
        html.append("            }\n");
        html.append("        }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<main>\n");
        html.append("  <div class=\"page-header\">\n");
        html.append("    <div class=\"company-identity\">\n");
        html.append("      <img src=\"")
                .append(escapeHtml(LOGO_URL))
                .append("\" alt=\"Shory company logo\" class=\"company-logo\" loading=\"lazy\">\n");
        if (insuranceCompanyName.isPresent()) {
            html.append("      <div class=\"company-name-wrapper\">\n");
            html.append("        <span class=\"company-label\">Insurance Company</span>\n");
            html.append("        <span class=\"company-name\">")
                    .append(escapeHtml(insuranceCompanyName.get()))
                    .append("</span>\n");
            html.append("      </div>\n");
        }
        html.append("    </div>\n");
        html.append("    <h1>")
                .append(escapeHtml(headerText))
                .append("</h1>\n");
        html.append("    <p>Overview of quote requests with pass and fail counts for each insurance type.</p>\n");
        html.append("  </div>\n");
        html.append("  <nav class=\"page-nav\">\n");
        html.append("    <button type=\"button\" class=\"nav-button active\" data-target=\"overview\">Overview</button>\n");
        html.append("    <button type=\"button\" class=\"nav-button\" data-target=\"tpl-analysis\">TPL Analysis</button>\n");
        html.append("    <button type=\"button\" class=\"nav-button\" data-target=\"comprehensive-analysis\">Comprehensive Analysis</button>\n");
        html.append("    <button type=\"button\" class=\"nav-button\" data-target=\"tpl-sales\">TPL Sales</button>\n");
        html.append("    <button type=\"button\" class=\"nav-button\" data-target=\"comprehensive-sales\">Comprehensive Sales</button>\n");
        html.append("  </nav>\n");
        html.append("  <section id=\"overview\" class=\"page-section active\">\n");
        html.append("    <div class=\"summary-grid\">\n");
        appendSummaryCard(html, "Total Quotes Requested", totalQuotes, "#2563eb");
        appendSummaryCard(html, "Successful Quotes", successCount, "#16a34a");
        appendSummaryCard(html, "Failed Quotes", failCount, "#dc2626");
        appendSummaryCard(html, "Unique Chassis Requested", uniqueChassisTotal, "#0891b2");
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
        html.append("          <h2>Unique Chassis by Specification</h2>\n");
        html.append("          <canvas id=\"overallSpecUniqueChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"chart-card chart-card--wide\">\n");
        html.append("          <div class=\"chart-card__header\">\n");
        html.append("            <h2>Manufacture Year Trend</h2>\n");
        html.append("            <div class=\"chart-toggle\" data-chart-toggle=\"overallManufactureYearChart\">\n");
        html.append("              <button type=\"button\" class=\"chart-toggle__button active\" data-series=\"quoted\">Quoted</button>\n");
        html.append("              <button type=\"button\" class=\"chart-toggle__button\" data-series=\"failed\">Failed</button>\n");
        html.append("            </div>\n");
        html.append("          </div>\n");
        html.append("          <canvas id=\"overallManufactureYearChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"chart-card chart-card--wide\">\n");
        html.append("          <div class=\"chart-card__header\">\n");
        html.append("            <h2>Customer Age Trend</h2>\n");
        html.append("            <div class=\"chart-toggle\" data-chart-toggle=\"overallCustomerAgeChart\">\n");
        html.append("              <button type=\"button\" class=\"chart-toggle__button active\" data-series=\"quoted\">Quoted</button>\n");
        html.append("              <button type=\"button\" class=\"chart-toggle__button\" data-series=\"failed\">Failed</button>\n");
        html.append("            </div>\n");
        html.append("          </div>\n");
        html.append("          <canvas id=\"overallCustomerAgeChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        appendCategoryCountTable(html, "Unique Chassis by Insurance Purpose", "Insurance Purpose",
                statistics.getUniqueChassisByInsurancePurpose());
        appendCategoryCountTable(html, "Unique Chassis by Body Type", "Body Type",
                statistics.getUniqueChassisByBodyType());
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
        html.append("    <div class=\"summary-section\">\n");
        html.append("      <h2 class=\"section-title\">Unique Case Analysis</h2>\n");
        html.append("      <div class=\"summary-grid\">\n");
        appendSummaryCard(html, "Requests with EID + Chassis", tplEidChassisTotal, "#2563eb");
        appendSummaryCard(html, "Unique Requests", tplEidChassisUnique, "#16a34a");
        appendSummaryCard(html, "Duplicate Requests", tplEidChassisDuplicates, "#dc2626");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"charts\">\n");
        html.append("      <div class=\"chart-card\">\n");
        html.append("        <h2>Total vs Unique Requests (EID + Chassis)</h2>\n");
        html.append("        <canvas id=\"tplEidChassisDedupChart\"></canvas>\n");
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
        html.append("      <h2 class=\"section-title\">Success Ratios by Nationality & Fuel Type</h2>\n");
        html.append("      <div class=\"chart-grid\">\n");
        html.append("        <div class=\"chart-card\">\n");
        html.append("          <h2>Chinese vs Non-Chinese</h2>\n");
        html.append("          <canvas id=\"tplChineseOutcomeChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"chart-card\">\n");
        html.append("          <h2>Electric vs Non-Electric</h2>\n");
        html.append("          <canvas id=\"tplElectricOutcomeChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"chart-card chart-card--wide\">\n");
        html.append("          <h2>Chinese & Fuel Type Segments</h2>\n");
        html.append("          <canvas id=\"tplChineseElectricSegmentChart\"></canvas>\n");
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
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
        appendEidChassisSummaryTable(html, "EID + Chassis Deduplication Details", tplEidChassisSummary);
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
        appendMakeModelTable(html, "Top 20 Rejected Models (Unique Chassis)",
                statistics.getComprehensiveTopRejectedModelsByUniqueChassis());
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
        html.append("  <section id=\"tpl-sales\" class=\"page-section\">\n");
        html.append("    <div class=\"summary-section\">\n");
        html.append("      <h2 class=\"section-title\">TPL Sales Conversion</h2>\n");
        html.append("      <p>Track how successful quotes translate into issued policies by customer segment.</p>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"summary-grid\">\n");
        appendSummaryCard(html, "Total Policies Sold", formatInteger(tplPoliciesSold), "#0f766e");
        appendSummaryCard(html, "Total Premium", formatCurrency(tplTotalPremium), "#f59e0b");
        appendSummaryCard(html, "Chinese Market Conversion", PERCENT_FORMAT.format(tplChineseSalesRatio), "#2563eb");
        appendSummaryCard(html, "Electric Vehicles Conversion", PERCENT_FORMAT.format(tplElectricSalesRatio), "#7c3aed");
        html.append("    </div>\n");
        html.append("    <div class=\"table-grid\">\n");
        appendBodyTypePremiumTable(html, "Sales Breakdown by Body Type", tplBodyPremiumBreakdowns);
        appendTopModelsByPremiumTable(html, "Top 10 Performing Models", tplTopModelsByPremium);
        html.append("    </div>\n");
        html.append("    <div class=\"charts\">\n");
        html.append("      <div class=\"chart-card chart-card--wide\">\n");
        html.append("        <h2>Body Type Premium & Policy Mix</h2>\n");
        html.append("        <canvas id=\"tplSalesBodyPremiumChart\"></canvas>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"charts\">\n");
        html.append("      <div class=\"chart-card chart-card--wide chart-card--split\">\n");
        html.append("        <div class=\"chart-card__chart\">\n");
        html.append("          <h2>Sales by Body Type</h2>\n");
        html.append("          <canvas id=\"tplSalesBodyChart\"></canvas>\n");
        html.append("        </div>\n");
        appendSalesConversionTable(html, "Body Type Conversion", tplSalesByBody);
        html.append("      </div>\n");
        html.append("      <div class=\"chart-card chart-card--wide\">\n");
        html.append("        <h2>Sales by Age Group</h2>\n");
        html.append("        <canvas id=\"tplSalesAgeChart\"></canvas>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"charts\">\n");
        html.append("      <div class=\"chart-card chart-card--wide\">\n");
        html.append("        <h2>Chinese Quotes Conversion</h2>\n");
        html.append("        <canvas id=\"tplSalesChineseChart\"></canvas>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"chart-card chart-card--wide\">\n");
        html.append("        <h2>Electric Vehicles Conversion</h2>\n");
        html.append("        <canvas id=\"tplSalesFuelChart\"></canvas>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("  </section>\n");
        html.append("  <section id=\"comprehensive-sales\" class=\"page-section\">\n");
        html.append("    <div class=\"summary-section\">\n");
        html.append("      <h2 class=\"section-title\">Comprehensive Sales Conversion</h2>\n");
        html.append("      <p>Understand how comprehensive quotes convert into policies by key demographic groups.</p>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"summary-grid\">\n");
        appendSummaryCard(html, "Total Policies Sold", formatInteger(compPoliciesSold), "#0f766e");
        appendSummaryCard(html, "Total Premium", formatCurrency(compTotalPremium), "#f59e0b");
        appendSummaryCard(html, "Chinese Market Conversion", PERCENT_FORMAT.format(compChineseSalesRatio), "#2563eb");
        appendSummaryCard(html, "Electric Vehicles Conversion", PERCENT_FORMAT.format(compElectricSalesRatio), "#7c3aed");
        html.append("    </div>\n");
        html.append("    <div class=\"table-grid\">\n");
        appendBodyTypePremiumTable(html, "Sales Breakdown by Body Type", compBodyPremiumBreakdowns);
        appendTopModelsByPremiumTable(html, "Top 10 Performing Models", compTopModelsByPremium);
        html.append("    </div>\n");
        html.append("    <div class=\"charts\">\n");
        html.append("      <div class=\"chart-card chart-card--wide\">\n");
        html.append("        <h2>Body Type Premium & Policy Mix</h2>\n");
        html.append("        <canvas id=\"compSalesBodyPremiumChart\"></canvas>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"charts\">\n");
        html.append("      <div class=\"chart-card chart-card--wide chart-card--split\">\n");
        html.append("        <div class=\"chart-card__chart\">\n");
        html.append("          <h2>Sales by Body Type</h2>\n");
        html.append("          <canvas id=\"compSalesBodyChart\"></canvas>\n");
        html.append("        </div>\n");
        appendSalesConversionTable(html, "Body Type Conversion", compSalesByBody);
        html.append("      </div>\n");
        html.append("      <div class=\"chart-card chart-card--wide\">\n");
        html.append("        <h2>Sales by Age Group</h2>\n");
        html.append("        <canvas id=\"compSalesAgeChart\"></canvas>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"charts\">\n");
        html.append("      <div class=\"chart-card chart-card--wide\">\n");
        html.append("        <h2>Chinese Quotes Conversion</h2>\n");
        html.append("        <canvas id=\"compSalesChineseChart\"></canvas>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"chart-card chart-card--wide\">\n");
        html.append("        <h2>Electric Vehicles Conversion</h2>\n");
        html.append("        <canvas id=\"compSalesFuelChart\"></canvas>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("  </section>\n");
        html.append("</main>\n");
        html.append(buildScripts(statistics, tplSpecificationSummary, compSpecificationSummary));
        html.append("</body>\n");
        html.append("</html>\n");
        return html.toString();
    }

    private String buildHeaderText(List<QuoteRecord> records, ReportDateRange reportDateRange) {
        if (reportDateRange.hasSelection()) {
            String startText = reportDateRange.getStartDate()
                    .map(this::formatDateForHeader)
                    .orElse("N/A");
            String endText = reportDateRange.getEndDate()
                    .map(this::formatDateForHeader)
                    .orElse("N/A");
            return "Overview of Quote requests Analysis from " + startText + " to " + endText;
        }
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
            Optional<LocalDateTime> requestedOn = record.getQuoteRequestedOn();
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

    private Optional<String> findInsuranceCompanyName(List<QuoteRecord> records) {
        LinkedHashSet<String> uniqueNames = new LinkedHashSet<>();
        for (QuoteRecord record : records) {
            record.getInsuranceCompanyName()
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .ifPresent(uniqueNames::add);
        }
        if (uniqueNames.isEmpty()) {
            return Optional.empty();
        }
        if (uniqueNames.size() == 1) {
            return Optional.of(uniqueNames.iterator().next());
        }
        return Optional.of(String.join(" / ", uniqueNames));
    }

    private String formatDateForHeader(LocalDateTime dateTime) {
        return dateTime.toLocalDate().format(HEADER_DATE_FORMAT);
    }

    private String formatDateForHeader(LocalDate date) {
        return date.format(HEADER_DATE_FORMAT);
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
        appendSummaryCard(html, label, formatInteger(value), accentColor);
    }

    private void appendSummaryCard(StringBuilder html, String label, String formattedValue, String accentColor) {
        html.append("    <div class=\"summary-card\" style=\"--accent: ")
                .append(escapeHtml(accentColor))
                .append(";\">\n");
        html.append("      <div class=\"summary-label\">")
                .append(escapeHtml(label))
                .append("</div>\n");
        html.append("      <div class=\"summary-value\">")
                .append(escapeHtml(formattedValue))
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

    private void appendBodyTypePremiumTable(StringBuilder html,
                                            String heading,
                                            List<QuoteStatistics.SalesPremiumBreakdown> breakdowns) {
        html.append("    <div class=\"table-card\">\n");
        html.append("      <h3>")
                .append(escapeHtml(heading))
                .append("</h3>\n");
        if (breakdowns.isEmpty()) {
            html.append("      <p class=\"empty-state\">No sales recorded.</p>\n");
            html.append("    </div>\n");
            return;
        }
        html.append("      <table>\n");
        html.append("        <thead>\n");
        html.append("          <tr>\n");
        html.append("            <th scope=\"col\">Body Type</th>\n");
        html.append("            <th scope=\"col\" class=\"numeric\">Total Policies</th>\n");
        html.append("            <th scope=\"col\" class=\"numeric\">Total Premium</th>\n");
        html.append("          </tr>\n");
        html.append("        </thead>\n");
        html.append("        <tbody>\n");
        for (QuoteStatistics.SalesPremiumBreakdown breakdown : breakdowns) {
            html.append("          <tr>\n");
            html.append("            <td>")
                    .append(escapeHtml(breakdown.getLabel()))
                    .append("</td>\n");
            html.append("            <td class=\"numeric\">")
                    .append(escapeHtml(formatInteger(breakdown.getSoldPolicies())))
                    .append("</td>\n");
            html.append("            <td class=\"numeric\">")
                    .append(escapeHtml(formatCurrency(breakdown.getTotalPremium())))
                    .append("</td>\n");
            html.append("          </tr>\n");
        }
        html.append("        </tbody>\n");
        html.append("      </table>\n");
        html.append("    </div>\n");
    }

    private void appendSalesConversionTable(StringBuilder html,
                                            String heading,
                                            List<QuoteStatistics.SalesConversionStats> stats) {
        html.append("        <div class=\"chart-card__table\">\n");
        html.append("          <h3>")
                .append(escapeHtml(heading))
                .append("</h3>\n");
        html.append("          <table class=\"conversion-table\">\n");
        html.append("            <thead>\n");
        html.append("              <tr>\n");
        html.append("                <th scope=\"col\">Segment</th>\n");
        html.append("                <th scope=\"col\" class=\"numeric\">Policies Sold</th>\n");
        html.append("                <th scope=\"col\" class=\"numeric\">Quote → Policy</th>\n");
        html.append("              </tr>\n");
        html.append("            </thead>\n");
        html.append("            <tbody>\n");
        if (stats.isEmpty()) {
            html.append("              <tr>\n");
            html.append("                <td colspan=\"3\" class=\"empty-state\">No sales records available.</td>\n");
            html.append("              </tr>\n");
        } else {
            for (QuoteStatistics.SalesConversionStats stat : stats) {
                html.append("              <tr>\n");
                html.append("                <td>")
                        .append(escapeHtml(stat.getLabel()))
                        .append("</td>\n");
                html.append("                <td class=\"numeric\">")
                        .append(escapeHtml(formatInteger(stat.getSoldPolicies())))
                        .append("</td>\n");
                html.append("                <td class=\"numeric\">")
                        .append(escapeHtml(formatPercentage(stat.getSoldPolicies(), stat.getTotalRequests())))
                        .append("</td>\n");
                html.append("              </tr>\n");
            }
        }
        html.append("            </tbody>\n");
        html.append("          </table>\n");
        html.append("        </div>\n");
    }

    private void appendTopModelsByPremiumTable(StringBuilder html,
                                               String heading,
                                               List<QuoteStatistics.MakeModelPremiumSummary> summaries) {
        html.append("    <div class=\"table-card\">\n");
        html.append("      <h3>")
                .append(escapeHtml(heading))
                .append("</h3>\n");
        if (summaries.isEmpty()) {
            html.append("      <p class=\"empty-state\">No issued policies found for the selected coverage.</p>\n");
            html.append("    </div>\n");
            return;
        }
        html.append("      <table>\n");
        html.append("        <thead>\n");
        html.append("          <tr>\n");
        html.append("            <th scope=\"col\">Make / Model</th>\n");
        html.append("            <th scope=\"col\" class=\"numeric\">Total Premium</th>\n");
        html.append("          </tr>\n");
        html.append("        </thead>\n");
        html.append("        <tbody>\n");
        for (QuoteStatistics.MakeModelPremiumSummary summary : summaries) {
            html.append("          <tr>\n");
            html.append("            <td>")
                    .append(escapeHtml(summary.getMake()))
                    .append(" / ")
                    .append(escapeHtml(summary.getModel()))
                    .append("<span class=\"table-subtext\">")
                    .append(escapeHtml(formatInteger(summary.getSoldPolicies())))
                    .append(" policies sold</span>")
                    .append("</td>\n");
            html.append("            <td class=\"numeric\">")
                    .append(escapeHtml(formatCurrency(summary.getTotalPremium())))
                    .append("</td>\n");
            html.append("          </tr>\n");
        }
        html.append("        </tbody>\n");
        html.append("      </table>\n");
        html.append("    </div>\n");
    }


    private void appendEidChassisSummaryTable(StringBuilder html,
                                              String heading,
                                              QuoteStatistics.EidChassisSummary summary) {
        html.append("    <div class=\"table-card\">\n");
        html.append("      <h3>")
                .append(escapeHtml(heading))
                .append("</h3>\n");
        if (summary.getTotalRequests() == 0) {
            html.append("      <p class=\"empty-state\">No matching requests with both EID and chassis recorded.</p>\n");
            html.append("    </div>\n");
            return;
        }
        html.append("      <table>\n");
        html.append("        <thead>\n");
        html.append("          <tr>\n");
        html.append("            <th scope=\"col\">Metric</th>\n");
        html.append("            <th scope=\"col\" class=\"numeric\">Count</th>\n");
        html.append("          </tr>\n");
        html.append("        </thead>\n");
        html.append("        <tbody>\n");
        html.append("          <tr>\n");
        html.append("            <td>Requests with EID + Chassis</td>\n");
        html.append("            <td class=\"numeric\">")
                .append(escapeHtml(formatInteger(summary.getTotalRequests())))
                .append("</td>\n");
        html.append("          </tr>\n");
        html.append("          <tr>\n");
        html.append("            <td>Unique Requests</td>\n");
        html.append("            <td class=\"numeric\">")
                .append(escapeHtml(formatInteger(summary.getUniqueRequests())))
                .append("</td>\n");
        html.append("          </tr>\n");
        html.append("          <tr>\n");
        html.append("            <td>Duplicate Requests</td>\n");
        html.append("            <td class=\"numeric\">")
                .append(escapeHtml(formatInteger(summary.getDuplicateRequests())))
                .append("</td>\n");
        html.append("          </tr>\n");
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
        QuoteStatistics.UniqueRequestSummary tplUniqueRequests = statistics.getTplUniqueRequests();
        QuoteStatistics.UniqueRequestSummary compUniqueRequests = statistics.getComprehensiveUniqueRequests();
        QuoteStatistics.EidChassisSummary tplEidChassisSummary = statistics.getTplEidChassisSummary();
        long tplEidChassisTotal = tplEidChassisSummary.getTotalRequests();
        long tplEidChassisUnique = tplEidChassisSummary.getUniqueRequests();
        long tplUniqueSuccessCount = tplUniqueRequests.getSuccessCount();
        long tplUniqueFailureCount = tplUniqueRequests.getFailureCount();
        long compUniqueSuccessCount = compUniqueRequests.getSuccessCount();
        long compUniqueFailureCount = compUniqueRequests.getFailureCount();
        long compPoliciesSold = statistics.getComprehensivePoliciesSold();
        BigDecimal compTotalPremium = statistics.getComprehensiveTotalPremium();
        double compChineseSalesRatio = statistics.getComprehensiveChineseSalesRatio();
        double compElectricSalesRatio = statistics.getComprehensiveElectricSalesRatio();
        List<QuoteStatistics.SalesPremiumBreakdown> compBodyPremiumBreakdowns =
                statistics.getComprehensiveBodyTypePremiums();
        List<QuoteStatistics.MakeModelPremiumSummary> compTopModelsByPremium =
                statistics.getComprehensiveTopModelsByPremium();

        List<QuoteStatistics.SalesPremiumBreakdown> tplBodyPremiumBreakdowns =
                statistics.getTplBodyTypePremiums();

        Map<String, QuoteStatistics.OutcomeBreakdown> tplBodyOutcomes = statistics.getTplBodyCategoryOutcomes();
        Map<String, QuoteStatistics.OutcomeBreakdown> tplChineseOutcomes = statistics.getTplChineseOutcomeBreakdown();
        Map<String, QuoteStatistics.OutcomeBreakdown> tplElectricOutcomes = statistics.getTplElectricOutcomeBreakdown();
        Map<String, QuoteStatistics.OutcomeBreakdown> tplChineseElectricOutcomes =
                statistics.getTplChineseElectricOutcomeBreakdown();
        Map<String, QuoteStatistics.OutcomeBreakdown> compBodyOutcomes = statistics.getComprehensiveBodyCategoryOutcomes();
        List<QuoteStatistics.AgeRangeStats> tplAgeStats = statistics.getTplAgeRangeStats();
        List<QuoteStatistics.AgeRangeStats> compAgeStats = statistics.getComprehensiveAgeRangeStats();
        List<QuoteStatistics.ManufactureYearStats> tplManufactureYearStats =
                statistics.getTplManufactureYearStats();
        List<QuoteStatistics.ManufactureYearStats> compManufactureYearStats =
                statistics.getComprehensiveManufactureYearStats();
        List<QuoteStatistics.ValueRangeStats> compValueStats = statistics.getComprehensiveEstimatedValueStats();
        List<QuoteStatistics.SalesConversionStats> tplSalesByBody = statistics.getTplSalesByBodyType();
        List<QuoteStatistics.SalesConversionStats> tplSalesByAge = statistics.getTplSalesByAgeRange();
        List<QuoteStatistics.SalesConversionStats> tplSalesByChinese =
                statistics.getTplSalesByChineseClassification();
        List<QuoteStatistics.SalesConversionStats> tplSalesByFuel = statistics.getTplSalesByFuelType();
        List<QuoteStatistics.SalesConversionStats> compSalesByBody = statistics.getComprehensiveSalesByBodyType();
        List<QuoteStatistics.SalesConversionStats> compSalesByAge = statistics.getComprehensiveSalesByAgeRange();
        List<QuoteStatistics.SalesConversionStats> compSalesByChinese =
                statistics.getComprehensiveSalesByChineseClassification();
        List<QuoteStatistics.SalesConversionStats> compSalesByFuel = statistics.getComprehensiveSalesByFuelType();
        List<String> tplBodyPremiumLabels = new ArrayList<>();
        List<Long> tplBodyPremiumPolicies = new ArrayList<>();
        List<BigDecimal> tplBodyPremiumPremiums = new ArrayList<>();
        populatePremiumChartData(tplBodyPremiumBreakdowns, tplBodyPremiumLabels,
                tplBodyPremiumPolicies, tplBodyPremiumPremiums);
        List<String> compBodyPremiumLabels = new ArrayList<>();
        List<Long> compBodyPremiumPolicies = new ArrayList<>();
        List<BigDecimal> compBodyPremiumPremiums = new ArrayList<>();
        populatePremiumChartData(compBodyPremiumBreakdowns, compBodyPremiumLabels,
                compBodyPremiumPolicies, compBodyPremiumPremiums);
        List<QuoteStatistics.TrendPoint> overallManufactureYearTrend = statistics.getManufactureYearTrend();
        List<QuoteStatistics.TrendPoint> overallCustomerAgeTrend = statistics.getCustomerAgeTrend();

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

        List<QuoteStatistics.CategoryCount> overallSpecificationCounts =
                statistics.getUniqueChassisBySpecification();
        List<String> specificationLabels = new ArrayList<>();
        List<Long> specificationValues = new ArrayList<>();
        if (overallSpecificationCounts.isEmpty()) {
            specificationLabels.add("No Data");
            specificationValues.add(0L);
        } else {
            for (QuoteStatistics.CategoryCount count : overallSpecificationCounts) {
                specificationLabels.add(count.getLabel());
                specificationValues.add(count.getCount());
            }
        }

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

        List<String> tplChineseLabels = new ArrayList<>();
        List<Double> tplChineseSuccessRatios = new ArrayList<>();
        List<Double> tplChineseFailureRatios = new ArrayList<>();
        populateOutcomeRatioData(tplChineseOutcomes, tplChineseLabels, tplChineseSuccessRatios, tplChineseFailureRatios);

        List<String> tplElectricLabels = new ArrayList<>();
        List<Double> tplElectricSuccessRatios = new ArrayList<>();
        List<Double> tplElectricFailureRatios = new ArrayList<>();
        populateOutcomeRatioData(tplElectricOutcomes, tplElectricLabels, tplElectricSuccessRatios, tplElectricFailureRatios);

        List<String> tplChineseElectricLabels = new ArrayList<>();
        List<Double> tplChineseElectricSuccessRatios = new ArrayList<>();
        List<Double> tplChineseElectricFailureRatios = new ArrayList<>();
        populateOutcomeRatioData(tplChineseElectricOutcomes, tplChineseElectricLabels,
                tplChineseElectricSuccessRatios, tplChineseElectricFailureRatios);

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

        List<String> tplSalesBodyLabels = new ArrayList<>();
        List<Long> tplSalesBodyTotals = new ArrayList<>();
        List<Long> tplSalesBodySuccessCounts = new ArrayList<>();
        List<Long> tplSalesBodySoldCounts = new ArrayList<>();
        populateSalesChartData(tplSalesByBody, tplSalesBodyLabels, tplSalesBodyTotals,
                tplSalesBodySuccessCounts, tplSalesBodySoldCounts);

        List<String> tplSalesAgeLabels = new ArrayList<>();
        List<Long> tplSalesAgeTotals = new ArrayList<>();
        List<Long> tplSalesAgeSuccessCounts = new ArrayList<>();
        List<Long> tplSalesAgeSoldCounts = new ArrayList<>();
        populateSalesChartData(tplSalesByAge, tplSalesAgeLabels, tplSalesAgeTotals,
                tplSalesAgeSuccessCounts, tplSalesAgeSoldCounts);

        List<String> compSalesBodyLabels = new ArrayList<>();
        List<Long> compSalesBodyTotals = new ArrayList<>();
        List<Long> compSalesBodySuccessCounts = new ArrayList<>();
        List<Long> compSalesBodySoldCounts = new ArrayList<>();
        populateSalesChartData(compSalesByBody, compSalesBodyLabels, compSalesBodyTotals,
                compSalesBodySuccessCounts, compSalesBodySoldCounts);

        List<String> compSalesAgeLabels = new ArrayList<>();
        List<Long> compSalesAgeTotals = new ArrayList<>();
        List<Long> compSalesAgeSuccessCounts = new ArrayList<>();
        List<Long> compSalesAgeSoldCounts = new ArrayList<>();
        populateSalesChartData(compSalesByAge, compSalesAgeLabels, compSalesAgeTotals,
                compSalesAgeSuccessCounts, compSalesAgeSoldCounts);

        List<String> tplSalesChineseLabels = new ArrayList<>();
        List<Long> tplSalesChineseTotals = new ArrayList<>();
        List<Long> tplSalesChineseSuccessCounts = new ArrayList<>();
        List<Long> tplSalesChineseSoldCounts = new ArrayList<>();
        populateSalesChartData(tplSalesByChinese, tplSalesChineseLabels, tplSalesChineseTotals,
                tplSalesChineseSuccessCounts, tplSalesChineseSoldCounts);

        List<String> tplSalesFuelLabels = new ArrayList<>();
        List<Long> tplSalesFuelTotals = new ArrayList<>();
        List<Long> tplSalesFuelSuccessCounts = new ArrayList<>();
        List<Long> tplSalesFuelSoldCounts = new ArrayList<>();
        populateSalesChartData(tplSalesByFuel, tplSalesFuelLabels, tplSalesFuelTotals,
                tplSalesFuelSuccessCounts, tplSalesFuelSoldCounts);

        List<String> compSalesChineseLabels = new ArrayList<>();
        List<Long> compSalesChineseTotals = new ArrayList<>();
        List<Long> compSalesChineseSuccessCounts = new ArrayList<>();
        List<Long> compSalesChineseSoldCounts = new ArrayList<>();
        populateSalesChartData(compSalesByChinese, compSalesChineseLabels, compSalesChineseTotals,
                compSalesChineseSuccessCounts, compSalesChineseSoldCounts);

        List<String> compSalesFuelLabels = new ArrayList<>();
        List<Long> compSalesFuelTotals = new ArrayList<>();
        List<Long> compSalesFuelSuccessCounts = new ArrayList<>();
        List<Long> compSalesFuelSoldCounts = new ArrayList<>();
        populateSalesChartData(compSalesByFuel, compSalesFuelLabels, compSalesFuelTotals,
                compSalesFuelSuccessCounts, compSalesFuelSoldCounts);

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
        List<Long> overallManufactureYearQuotedCounts = new ArrayList<>();
        List<Long> overallManufactureYearFailedCounts = new ArrayList<>();
        if (overallManufactureYearTrend.isEmpty()) {
            overallManufactureYearLabels.add("No Data");
            overallManufactureYearQuotedCounts.add(0L);
            overallManufactureYearFailedCounts.add(0L);
        } else {
            for (QuoteStatistics.TrendPoint point : overallManufactureYearTrend) {
                overallManufactureYearLabels.add(point.getLabel());
                overallManufactureYearQuotedCounts.add(point.getQuotedCount());
                overallManufactureYearFailedCounts.add(point.getFailedCount());
            }
        }

        List<String> overallCustomerAgeLabels = new ArrayList<>();
        List<Long> overallCustomerAgeQuotedCounts = new ArrayList<>();
        List<Long> overallCustomerAgeFailedCounts = new ArrayList<>();
        if (overallCustomerAgeTrend.isEmpty()) {
            overallCustomerAgeLabels.add("No Data");
            overallCustomerAgeQuotedCounts.add(0L);
            overallCustomerAgeFailedCounts.add(0L);
        } else {
            for (QuoteStatistics.TrendPoint point : overallCustomerAgeTrend) {
                overallCustomerAgeLabels.add(point.getLabel());
                overallCustomerAgeQuotedCounts.add(point.getQuotedCount());
                overallCustomerAgeFailedCounts.add(point.getFailedCount());
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
        script.append("  const stackedRatioOptions = {\n");
        script.append("    responsive: true,\n");
        script.append("    interaction: { mode: 'index', intersect: false },\n");
        script.append("    scales: {\n");
        script.append("      x: { stacked: true },\n");
        script.append("      y: { beginAtZero: true, max: 100, stacked: true, ticks: { callback: value => `${value}%` } }\n");
        script.append("    },\n");
        script.append("    plugins: {\n");
        script.append("      legend: { display: true, position: 'bottom', labels: { usePointStyle: true } },\n");
        script.append("      tooltip: { callbacks: { label: ctx => `${ctx.dataset.label}: ${ctx.raw.toFixed(1)}%` } }\n");
        script.append("    }\n");
        script.append("  };\n");
        script.append("  const trendLineOptions = {\n");
        script.append("    responsive: true,\n");
        script.append("    interaction: { mode: 'index', intersect: false },\n");
        script.append("    scales: { y: { beginAtZero: true, ticks: { callback: value => value.toLocaleString() } } },\n");
        script.append("    plugins: { tooltip: { callbacks: { label: ctx => `${ctx.dataset.label}: ${ctx.raw.toLocaleString()}` } } }\n");
        script.append("  };\n");
        script.append("  const cloneDataset = dataset => ({\n");
        script.append("    ...dataset,\n");
        script.append("    data: Array.isArray(dataset.data) ? [...dataset.data] : dataset.data\n");
        script.append("  });\n");
        script.append("  const registerChartToggle = (toggleId, chart, seriesMap) => {\n");
        script.append("    const toggleElement = document.querySelector(`[data-chart-toggle='${toggleId}']`);\n");
        script.append("    if (!toggleElement) {\n");
        script.append("      return;\n");
        script.append("    }\n");
        script.append("    const buttons = Array.from(toggleElement.querySelectorAll('button[data-series]'));\n");
        script.append("    buttons.forEach(button => {\n");
        script.append("      button.addEventListener('click', () => {\n");
        script.append("        const key = button.getAttribute('data-series');\n");
        script.append("        const dataset = seriesMap[key];\n");
        script.append("        if (!dataset) {\n");
        script.append("          return;\n");
        script.append("        }\n");
        script.append("        chart.data.datasets = [cloneDataset(dataset)];\n");
        script.append("        chart.update();\n");
        script.append("        buttons.forEach(btn => btn.classList.toggle('active', btn === button));\n");
        script.append("      });\n");
        script.append("    });\n");
        script.append("  };\n");
        script.append("  const buildSalesStats = (labels, totals, successes, solds) => labels.map((label, index) => ({\n");
        script.append("    label,\n");
        script.append("    totalRequests: totals[index] == null ? 0 : totals[index],\n");
        script.append("    successfulQuotes: successes[index] == null ? 0 : successes[index],\n");
        script.append("    soldPolicies: solds[index] == null ? 0 : solds[index]\n");
        script.append("  }));\n");
        script.append("  const numberFormatter = new Intl.NumberFormat('en-US');\n");
        script.append("  const computeOverallConversion = stat => {\n");
        script.append("    if (!stat || !stat.totalRequests) {\n");
        script.append("      return 0;\n");
        script.append("    }\n");
        script.append("    return (stat.soldPolicies / stat.totalRequests) * 100;\n");
        script.append("  };\n");
        script.append("  const createBodySalesChart = (canvasId, stats) => {\n");
        script.append("    const canvas = document.getElementById(canvasId);\n");
        script.append("    if (!canvas) {\n");
        script.append("      return null;\n");
        script.append("    }\n");
        script.append("    return new Chart(canvas, {\n");
        script.append("      type: 'bar',\n");
        script.append("      data: {\n");
        script.append("        labels: stats.map(stat => stat.label),\n");
        script.append("        datasets: [{\n");
        script.append("          label: 'Policies Sold',\n");
        script.append("          data: stats.map(stat => stat.soldPolicies),\n");
        script.append("          backgroundColor: '#16a34a',\n");
        script.append("          borderRadius: 8\n");
        script.append("        }]\n");
        script.append("      },\n");
        script.append("      options: {\n");
        script.append("        ...sharedOptions,\n");
        script.append("        plugins: {\n");
        script.append("          ...sharedOptions.plugins,\n");
        script.append("          tooltip: {\n");
        script.append("            callbacks: {\n");
        script.append("              label: ctx => `Policies Sold: ${numberFormatter.format(ctx.raw ?? 0)}`,\n");
        script.append("              afterBody: items => {\n");
        script.append("                if (!items.length) {\n");
        script.append("                  return [];\n");
        script.append("                }\n");
        script.append("                const stat = stats[items[0].dataIndex];\n");
        script.append("                const conversion = computeOverallConversion(stat);\n");
        script.append("                return [\n");
        script.append("                  `Total Quotes: ${numberFormatter.format(stat.totalRequests)}`,\n");
        script.append("                  `Quote → Policy: ${conversion.toFixed(1)}%`\n");
        script.append("                ];\n");
        script.append("              }\n");
        script.append("            }\n");
        script.append("          },\n");
        script.append("          legend: { display: false }\n");
        script.append("        }\n");
        script.append("      }\n");
        script.append("    });\n");
        script.append("  };\n");
        script.append("  const createAgeSalesChart = (canvasId, stats) => {\n");
        script.append("    const canvas = document.getElementById(canvasId);\n");
        script.append("    if (!canvas) {\n");
        script.append("      return null;\n");
        script.append("    }\n");
        script.append("    const conversionSeries = stats.map(stat => Number(computeOverallConversion(stat).toFixed(1)));\n");
        script.append("    return new Chart(canvas, {\n");
        script.append("      type: 'bar',\n");
        script.append("      data: {\n");
        script.append("        labels: stats.map(stat => stat.label),\n");
        script.append("        datasets: [\n");
        script.append("          {\n");
        script.append("            type: 'bar',\n");
        script.append("            label: 'Policies Sold',\n");
        script.append("            data: stats.map(stat => stat.soldPolicies),\n");
        script.append("            backgroundColor: '#16a34a',\n");
        script.append("            borderRadius: 8,\n");
        script.append("            order: 2\n");
        script.append("          },\n");
        script.append("          {\n");
        script.append("            type: 'line',\n");
        script.append("            label: 'Quotes Generated',\n");
        script.append("            data: stats.map(stat => stat.totalRequests),\n");
        script.append("            borderColor: '#2563eb',\n");
        script.append("            backgroundColor: '#2563eb',\n");
        script.append("            tension: 0.35,\n");
        script.append("            fill: false,\n");
        script.append("            pointRadius: 4,\n");
        script.append("            pointHoverRadius: 6,\n");
        script.append("            order: 1,\n");
        script.append("            yAxisID: 'y'\n");
        script.append("          },\n");
        script.append("          {\n");
        script.append("            type: 'line',\n");
        script.append("            label: 'Conversion Rate',\n");
        script.append("            data: conversionSeries,\n");
        script.append("            borderColor: '#f97316',\n");
        script.append("            backgroundColor: '#f97316',\n");
        script.append("            borderDash: [4, 4],\n");
        script.append("            tension: 0.35,\n");
        script.append("            fill: false,\n");
        script.append("            pointRadius: 4,\n");
        script.append("            pointHoverRadius: 6,\n");
        script.append("            order: 0,\n");
        script.append("            yAxisID: 'y1'\n");
        script.append("          }\n");
        script.append("        ]\n");
        script.append("      },\n");
        script.append("      options: {\n");
        script.append("        responsive: true,\n");
        script.append("        interaction: { mode: 'index', intersect: false },\n");
        script.append("        scales: {\n");
        script.append("          y: {\n");
        script.append("            beginAtZero: true,\n");
        script.append("            title: { display: true, text: 'Policies / Quotes' },\n");
        script.append("            ticks: { callback: value => numberFormatter.format(value) }\n");
        script.append("          },\n");
        script.append("          y1: {\n");
        script.append("            beginAtZero: true,\n");
        script.append("            position: 'right',\n");
        script.append("            grid: { drawOnChartArea: false },\n");
        script.append("            ticks: { callback: value => `${value}%` },\n");
        script.append("            title: { display: true, text: 'Conversion Rate' }\n");
        script.append("          }\n");
        script.append("        },\n");
        script.append("        plugins: {\n");
        script.append("          legend: { display: true, position: 'bottom', labels: { usePointStyle: true } },\n");
        script.append("          tooltip: {\n");
        script.append("            callbacks: {\n");
        script.append("              label: ctx => {\n");
        script.append("                if (ctx.dataset.label === 'Conversion Rate') {\n");
        script.append("                  return `Conversion Rate: ${ctx.raw?.toFixed(1) ?? 0}%`;\n");
        script.append("                }\n");
        script.append("                return `${ctx.dataset.label}: ${numberFormatter.format(ctx.raw ?? 0)}`;\n");
        script.append("              },\n");
        script.append("              afterBody: items => {\n");
        script.append("                if (!items.length) {\n");
        script.append("                  return [];\n");
        script.append("                }\n");
        script.append("                const stat = stats[items[0].dataIndex];\n");
        script.append("                return [`Quote → Policy: ${computeOverallConversion(stat).toFixed(1)}%`];\n");
        script.append("              }\n");
        script.append("            }\n");
        script.append("          }\n");
        script.append("        }\n");
        script.append("      }\n");
        script.append("    });\n");
        script.append("  };\n");
        script.append("  const createQuotePolicyChart = (canvasId, stats) => {\n");
        script.append("    const canvas = document.getElementById(canvasId);\n");
        script.append("    if (!canvas) {\n");
        script.append("      return null;\n");
        script.append("    }\n");
        script.append("    return new Chart(canvas, {\n");
        script.append("      type: 'bar',\n");
        script.append("      data: {\n");
        script.append("        labels: stats.map(stat => stat.label),\n");
        script.append("        datasets: [\n");
        script.append("          {\n");
        script.append("            label: 'Quotes Generated',\n");
        script.append("            data: stats.map(stat => stat.totalRequests),\n");
        script.append("            backgroundColor: '#2563eb',\n");
        script.append("            borderRadius: 8\n");
        script.append("          },\n");
        script.append("          {\n");
        script.append("            label: 'Policies Sold',\n");
        script.append("            data: stats.map(stat => stat.soldPolicies),\n");
        script.append("            backgroundColor: '#16a34a',\n");
        script.append("            borderRadius: 8\n");
        script.append("          }\n");
        script.append("        ]\n");
        script.append("      },\n");
        script.append("      options: {\n");
        script.append("        ...sharedOptions,\n");
        script.append("        interaction: { mode: 'index', intersect: false },\n");
        script.append("        plugins: {\n");
        script.append("          ...sharedOptions.plugins,\n");
        script.append("          legend: { display: true, position: 'bottom', labels: { usePointStyle: true } },\n");
        script.append("          tooltip: {\n");
        script.append("            callbacks: {\n");
        script.append("              label: ctx => `${ctx.dataset.label}: ${numberFormatter.format(ctx.raw ?? 0)}`,\n");
        script.append("              afterBody: items => {\n");
        script.append("                if (!items.length) {\n");
        script.append("                  return [];\n");
        script.append("                }\n");
        script.append("                const stat = stats[items[0].dataIndex];\n");
        script.append("                return [`Quote → Policy: ${computeOverallConversion(stat).toFixed(1)}%`];\n");
        script.append("              }\n");
        script.append("            }\n");
        script.append("          }\n");
        script.append("        }\n");
        script.append("      }\n");
        script.append("    });\n");
        script.append("  };\n");
        script.append("  const tplData = {\n");
        script.append("    labels: ['Success', 'Failed'],\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'Quotes',\n");
        script.append("      data: [").append(tplUniqueSuccessCount).append(',').append(tplUniqueFailureCount).append("],\n");
        script.append("      backgroundColor: ['#16a34a', '#dc2626'],\n");
        script.append("      borderRadius: 8\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const compData = {\n");
        script.append("    labels: ['Success', 'Failed'],\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'Quotes',\n");
        script.append("      data: [").append(compUniqueSuccessCount).append(',').append(compUniqueFailureCount).append("],\n");
        script.append("      backgroundColor: ['#16a34a', '#dc2626'],\n");
        script.append("      borderRadius: 8\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const overallSpecificationData = {\n");
        script.append("    labels: ").append(toJsStringArray(specificationLabels)).append(",\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'Unique Chassis',\n");
        script.append("      data: ").append(toJsNumberArray(specificationValues)).append(",\n");
        script.append("      backgroundColor: '#0891b2',\n");
        script.append("      borderRadius: 8\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const overallManufactureYearSeries = {\n");
        script.append("    quoted: {\n");
        script.append("      label: 'Quoted',\n");
        script.append("      data: ").append(toJsNumberArray(overallManufactureYearQuotedCounts)).append(",\n");
        script.append("      borderColor: '#2563eb',\n");
        script.append("      backgroundColor: 'rgba(37, 99, 235, 0.15)',\n");
        script.append("      tension: 0.35,\n");
        script.append("      fill: true\n");
        script.append("    },\n");
        script.append("    failed: {\n");
        script.append("      label: 'Failed',\n");
        script.append("      data: ").append(toJsNumberArray(overallManufactureYearFailedCounts)).append(",\n");
        script.append("      borderColor: '#dc2626',\n");
        script.append("      backgroundColor: 'rgba(220, 38, 38, 0.15)',\n");
        script.append("      tension: 0.35,\n");
        script.append("      fill: true\n");
        script.append("    }\n");
        script.append("  };\n");
        script.append("  const overallCustomerAgeSeries = {\n");
        script.append("    quoted: {\n");
        script.append("      label: 'Quoted',\n");
        script.append("      data: ").append(toJsNumberArray(overallCustomerAgeQuotedCounts)).append(",\n");
        script.append("      borderColor: '#f97316',\n");
        script.append("      backgroundColor: 'rgba(249, 115, 22, 0.15)',\n");
        script.append("      tension: 0.35,\n");
        script.append("      fill: true\n");
        script.append("    },\n");
        script.append("    failed: {\n");
        script.append("      label: 'Failed',\n");
        script.append("      data: ").append(toJsNumberArray(overallCustomerAgeFailedCounts)).append(",\n");
        script.append("      borderColor: '#dc2626',\n");
        script.append("      backgroundColor: 'rgba(220, 38, 38, 0.15)',\n");
        script.append("      tension: 0.35,\n");
        script.append("      fill: true\n");
        script.append("    }\n");
        script.append("  };\n");
        script.append("  const tplEidChassisDedupData = {\n");
        script.append("    labels: ['Total Requests', 'Unique Requests'],\n");
        script.append("    datasets: [{\n");
        script.append("      label: 'Requests',\n");
        script.append("      data: [").append(tplEidChassisTotal).append(',').append(tplEidChassisUnique).append("],\n");
        script.append("      backgroundColor: ['#2563eb', '#16a34a'],\n");
        script.append("      borderRadius: 8\n");
        script.append("    }]\n");
        script.append("  };\n");
        script.append("  const tplBodyPremiumLabels = ").append(toJsStringArray(tplBodyPremiumLabels)).append(";\n");
        script.append("  const tplBodyPremiumPolicies = ").append(toJsNumberArray(tplBodyPremiumPolicies)).append(";\n");
        script.append("  const tplBodyPremiumPremiums = ").append(toJsNumberArray(tplBodyPremiumPremiums)).append(";\n");
        script.append("  const compBodyPremiumLabels = ").append(toJsStringArray(compBodyPremiumLabels)).append(";\n");
        script.append("  const compBodyPremiumPolicies = ").append(toJsNumberArray(compBodyPremiumPolicies)).append(";\n");
        script.append("  const compBodyPremiumPremiums = ").append(toJsNumberArray(compBodyPremiumPremiums)).append(";\n");
        script.append("  const tplSalesBodyLabels = ").append(toJsStringArray(tplSalesBodyLabels)).append(";\n");
        script.append("  const tplSalesBodyTotals = ").append(toJsNumberArray(tplSalesBodyTotals)).append(";\n");
        script.append("  const tplSalesBodySuccessCounts = ").append(toJsNumberArray(tplSalesBodySuccessCounts)).append(";\n");
        script.append("  const tplSalesBodySoldCounts = ").append(toJsNumberArray(tplSalesBodySoldCounts)).append(";\n");
        script.append("  const tplSalesAgeLabels = ").append(toJsStringArray(tplSalesAgeLabels)).append(";\n");
        script.append("  const tplSalesAgeTotals = ").append(toJsNumberArray(tplSalesAgeTotals)).append(";\n");
        script.append("  const tplSalesAgeSuccessCounts = ").append(toJsNumberArray(tplSalesAgeSuccessCounts)).append(";\n");
        script.append("  const tplSalesAgeSoldCounts = ").append(toJsNumberArray(tplSalesAgeSoldCounts)).append(";\n");
        script.append("  const compSalesBodyLabels = ").append(toJsStringArray(compSalesBodyLabels)).append(";\n");
        script.append("  const compSalesBodyTotals = ").append(toJsNumberArray(compSalesBodyTotals)).append(";\n");
        script.append("  const compSalesBodySuccessCounts = ").append(toJsNumberArray(compSalesBodySuccessCounts)).append(";\n");
        script.append("  const compSalesBodySoldCounts = ").append(toJsNumberArray(compSalesBodySoldCounts)).append(";\n");
        script.append("  const compSalesAgeLabels = ").append(toJsStringArray(compSalesAgeLabels)).append(";\n");
        script.append("  const compSalesAgeTotals = ").append(toJsNumberArray(compSalesAgeTotals)).append(";\n");
        script.append("  const compSalesAgeSuccessCounts = ").append(toJsNumberArray(compSalesAgeSuccessCounts)).append(";\n");
        script.append("  const compSalesAgeSoldCounts = ").append(toJsNumberArray(compSalesAgeSoldCounts)).append(";\n");
        script.append("  const tplSalesChineseLabels = ").append(toJsStringArray(tplSalesChineseLabels)).append(";\n");
        script.append("  const tplSalesChineseTotals = ").append(toJsNumberArray(tplSalesChineseTotals)).append(";\n");
        script.append("  const tplSalesChineseSuccessCounts = ").append(toJsNumberArray(tplSalesChineseSuccessCounts)).append(";\n");
        script.append("  const tplSalesChineseSoldCounts = ").append(toJsNumberArray(tplSalesChineseSoldCounts)).append(";\n");
        script.append("  const tplSalesFuelLabels = ").append(toJsStringArray(tplSalesFuelLabels)).append(";\n");
        script.append("  const tplSalesFuelTotals = ").append(toJsNumberArray(tplSalesFuelTotals)).append(";\n");
        script.append("  const tplSalesFuelSuccessCounts = ").append(toJsNumberArray(tplSalesFuelSuccessCounts)).append(";\n");
        script.append("  const tplSalesFuelSoldCounts = ").append(toJsNumberArray(tplSalesFuelSoldCounts)).append(";\n");
        script.append("  const compSalesChineseLabels = ").append(toJsStringArray(compSalesChineseLabels)).append(";\n");
        script.append("  const compSalesChineseTotals = ").append(toJsNumberArray(compSalesChineseTotals)).append(";\n");
        script.append("  const compSalesChineseSuccessCounts = ").append(toJsNumberArray(compSalesChineseSuccessCounts)).append(";\n");
        script.append("  const compSalesChineseSoldCounts = ").append(toJsNumberArray(compSalesChineseSoldCounts)).append(";\n");
        script.append("  const compSalesFuelLabels = ").append(toJsStringArray(compSalesFuelLabels)).append(";\n");
        script.append("  const compSalesFuelTotals = ").append(toJsNumberArray(compSalesFuelTotals)).append(";\n");
        script.append("  const compSalesFuelSuccessCounts = ").append(toJsNumberArray(compSalesFuelSuccessCounts)).append(";\n");
        script.append("  const compSalesFuelSoldCounts = ").append(toJsNumberArray(compSalesFuelSoldCounts)).append(";\n");
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
        script.append("  const tplChineseOutcomeData = {\n");
        script.append("    labels: ").append(toJsStringArray(tplChineseLabels)).append(",\n");
        script.append("    datasets: [\n");
        script.append("      {\n");
        script.append("        label: 'Success %',\n");
        script.append("        data: ").append(toJsDoubleArray(tplChineseSuccessRatios)).append(",\n");
        script.append("        backgroundColor: 'rgba(22, 163, 74, 0.85)',\n");
        script.append("        stack: 'ratio',\n");
        script.append("        borderRadius: 8\n");
        script.append("      },\n");
        script.append("      {\n");
        script.append("        label: 'Failure %',\n");
        script.append("        data: ").append(toJsDoubleArray(tplChineseFailureRatios)).append(",\n");
        script.append("        backgroundColor: 'rgba(220, 38, 38, 0.8)',\n");
        script.append("        stack: 'ratio',\n");
        script.append("        borderRadius: 8\n");
        script.append("      }\n");
        script.append("    ]\n");
        script.append("  };\n");
        script.append("  const tplElectricOutcomeData = {\n");
        script.append("    labels: ").append(toJsStringArray(tplElectricLabels)).append(",\n");
        script.append("    datasets: [\n");
        script.append("      {\n");
        script.append("        label: 'Success %',\n");
        script.append("        data: ").append(toJsDoubleArray(tplElectricSuccessRatios)).append(",\n");
        script.append("        backgroundColor: 'rgba(14, 116, 144, 0.85)',\n");
        script.append("        stack: 'ratio',\n");
        script.append("        borderRadius: 8\n");
        script.append("      },\n");
        script.append("      {\n");
        script.append("        label: 'Failure %',\n");
        script.append("        data: ").append(toJsDoubleArray(tplElectricFailureRatios)).append(",\n");
        script.append("        backgroundColor: 'rgba(234, 179, 8, 0.85)',\n");
        script.append("        stack: 'ratio',\n");
        script.append("        borderRadius: 8\n");
        script.append("      }\n");
        script.append("    ]\n");
        script.append("  };\n");
        
        script.append("  const tplBodyPremiumChartData = {\n");
        script.append("    labels: tplBodyPremiumLabels,\n");
        script.append("    datasets: [\n");
        script.append("      {\n");
        script.append("        type: 'bar',\n");
        script.append("        label: 'Policies Sold',\n");
        script.append("        data: tplBodyPremiumPolicies,\n");
        script.append("        backgroundColor: '#0ea5e9',\n");
        script.append("        borderRadius: 8,\n");
        script.append("        yAxisID: 'yPolicies'\n");
        script.append("      },\n");
        script.append("      {\n");
        script.append("        type: 'line',\n");
        script.append("        label: 'Total Premium',\n");
        script.append("        data: tplBodyPremiumPremiums,\n");
        script.append("        borderColor: '#f59e0b',\n");
        script.append("        backgroundColor: 'rgba(245, 158, 11, 0.25)',\n");
        script.append("        borderWidth: 3,\n");
        script.append("        tension: 0.35,\n");
        script.append("        fill: true,\n");
        script.append("        pointRadius: 4,\n");
        script.append("        pointHoverRadius: 6,\n");
        script.append("        yAxisID: 'yPremium'\n");
        script.append("      }\n");
        script.append("    ]\n");
        script.append("  };\n");
        script.append("  const compBodyPremiumChartData = {\n");
        script.append("    labels: compBodyPremiumLabels,\n");
        script.append("    datasets: [\n");
        script.append("      {\n");
        script.append("        type: 'bar',\n");
        script.append("        label: 'Policies Sold',\n");
        script.append("        data: compBodyPremiumPolicies,\n");
        script.append("        backgroundColor: '#2563eb',\n");
        script.append("        borderRadius: 8,\n");
        script.append("        yAxisID: 'yPolicies'\n");
        script.append("      },\n");
        script.append("      {\n");
        script.append("        type: 'line',\n");
        script.append("        label: 'Total Premium',\n");
        script.append("        data: compBodyPremiumPremiums,\n");
        script.append("        borderColor: '#f59e0b',\n");
        script.append("        backgroundColor: 'rgba(245, 158, 11, 0.25)',\n");
        script.append("        borderWidth: 3,\n");
        script.append("        tension: 0.35,\n");
        script.append("        fill: true,\n");
        script.append("        pointRadius: 4,\n");
        script.append("        pointHoverRadius: 6,\n");
        script.append("        yAxisID: 'yPremium'\n");
        script.append("      }\n");
        script.append("    ]\n");
        script.append("  };\n");
        script.append("  const bodyPremiumChartOptions = {\n");
        script.append("    responsive: true,\n");
        script.append("    interaction: { mode: 'index', intersect: false },\n");
        script.append("    scales: {\n");
        script.append("      yPolicies: {\n");
        script.append("        beginAtZero: true,\n");
        script.append("        position: 'left',\n");
        script.append("        ticks: { callback: value => Number(value).toLocaleString() }\n");
        script.append("      },\n");
        script.append("      yPremium: {\n");
        script.append("        beginAtZero: true,\n");
        script.append("        position: 'right',\n");
        script.append("        grid: { drawOnChartArea: false },\n");
        script.append("        ticks: { callback: value => `AED ${Number(value).toLocaleString(undefined, { minimumFractionDigits: 0 })}` }\n");
        script.append("      }\n");
        script.append("    },\n");
        script.append("    plugins: {\n");
        script.append("      legend: { display: true, position: 'bottom', labels: { usePointStyle: true } },\n");
        script.append("      tooltip: {\n");
        script.append("        callbacks: {\n");
        script.append("          label: ctx => {\n");
        script.append("            const rawValue = ctx.raw == null ? 0 : ctx.raw;\n");
        script.append("            if (ctx.dataset.yAxisID === 'yPremium') {\n");
        script.append("              return `${ctx.dataset.label}: AED ${Number(rawValue).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;\n");
        script.append("            }\n");
        script.append("            return `${ctx.dataset.label}: ${Number(rawValue).toLocaleString()}`;\n");
        script.append("          }\n");
        script.append("        }\n");
        script.append("      }\n");
        script.append("    }\n");
        script.append("  };\n");
        
        script.append("  const tplChineseElectricSegmentData = {\n");
        script.append("    labels: ").append(toJsStringArray(tplChineseElectricLabels)).append(",\n");
        script.append("    datasets: [\n");
        script.append("      {\n");
        script.append("        label: 'Success %',\n");
        script.append("        data: ").append(toJsDoubleArray(tplChineseElectricSuccessRatios)).append(",\n");
        script.append("        backgroundColor: 'rgba(59, 130, 246, 0.85)',\n");
        script.append("        stack: 'ratio',\n");
        script.append("        borderRadius: 8\n");
        script.append("      },\n");
        script.append("      {\n");
        script.append("        label: 'Failure %',\n");
        script.append("        data: ").append(toJsDoubleArray(tplChineseElectricFailureRatios)).append(",\n");
        script.append("        backgroundColor: 'rgba(217, 70, 239, 0.8)',\n");
        script.append("        stack: 'ratio',\n");
        script.append("        borderRadius: 8\n");
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
        script.append("  new Chart(document.getElementById('tplSalesBodyPremiumChart'), { type: 'bar', data: tplBodyPremiumChartData, options: bodyPremiumChartOptions });\n");
        script.append("  new Chart(document.getElementById('compSalesBodyPremiumChart'), { type: 'bar', data: compBodyPremiumChartData, options: bodyPremiumChartOptions });\n");
        script.append("  const tplSalesBodyStats = buildSalesStats(tplSalesBodyLabels, tplSalesBodyTotals, tplSalesBodySuccessCounts, tplSalesBodySoldCounts);\n");
        script.append("  const tplSalesAgeStats = buildSalesStats(tplSalesAgeLabels, tplSalesAgeTotals, tplSalesAgeSuccessCounts, tplSalesAgeSoldCounts);\n");
        script.append("  const compSalesBodyStats = buildSalesStats(compSalesBodyLabels, compSalesBodyTotals, compSalesBodySuccessCounts, compSalesBodySoldCounts);\n");
        script.append("  const compSalesAgeStats = buildSalesStats(compSalesAgeLabels, compSalesAgeTotals, compSalesAgeSuccessCounts, compSalesAgeSoldCounts);\n");
        script.append("  const tplSalesChineseStats = buildSalesStats(tplSalesChineseLabels, tplSalesChineseTotals, tplSalesChineseSuccessCounts, tplSalesChineseSoldCounts);\n");
        script.append("  const tplSalesFuelStats = buildSalesStats(tplSalesFuelLabels, tplSalesFuelTotals, tplSalesFuelSuccessCounts, tplSalesFuelSoldCounts);\n");
        script.append("  const compSalesChineseStats = buildSalesStats(compSalesChineseLabels, compSalesChineseTotals, compSalesChineseSuccessCounts, compSalesChineseSoldCounts);\n");
        script.append("  const compSalesFuelStats = buildSalesStats(compSalesFuelLabels, compSalesFuelTotals, compSalesFuelSuccessCounts, compSalesFuelSoldCounts);\n");
        script.append("  createBodySalesChart('tplSalesBodyChart', tplSalesBodyStats);\n");
        script.append("  createAgeSalesChart('tplSalesAgeChart', tplSalesAgeStats);\n");
        script.append("  createBodySalesChart('compSalesBodyChart', compSalesBodyStats);\n");
        script.append("  createAgeSalesChart('compSalesAgeChart', compSalesAgeStats);\n");
        script.append("  createQuotePolicyChart('tplSalesChineseChart', tplSalesChineseStats);\n");
        script.append("  createQuotePolicyChart('tplSalesFuelChart', tplSalesFuelStats);\n");
        script.append("  createQuotePolicyChart('compSalesChineseChart', compSalesChineseStats);\n");
        script.append("  createQuotePolicyChart('compSalesFuelChart', compSalesFuelStats);\n");
        script.append("  new Chart(document.getElementById('tplChineseOutcomeChart'), { type: 'bar', data: tplChineseOutcomeData, options: stackedRatioOptions });\n");
        script.append("  new Chart(document.getElementById('tplElectricOutcomeChart'), { type: 'bar', data: tplElectricOutcomeData, options: stackedRatioOptions });\n");
        script.append("  new Chart(document.getElementById('tplChineseElectricSegmentChart'), { type: 'bar', data: tplChineseElectricSegmentData, options: stackedRatioOptions });\n");
        script.append("  new Chart(document.getElementById('tplOutcomesChart'), { type: 'bar', data: tplData, options: sharedOptions });\n");
        script.append("  new Chart(document.getElementById('compOutcomesChart'), { type: 'bar', data: compData, options: sharedOptions });\n");
        script.append("  new Chart(document.getElementById('overallSpecUniqueChart'), { type: 'bar', data: overallSpecificationData, options: sharedOptions });\n");
        script.append("  new Chart(document.getElementById('tplEidChassisDedupChart'), { type: 'bar', data: tplEidChassisDedupData, options: sharedOptions });\n");
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
        script.append("  const overallManufactureYearChart = new Chart(document.getElementById('overallManufactureYearChart'), {\n");
        script.append("    type: 'line',\n");
        script.append("    data: {\n");
        script.append("      labels: ").append(toJsStringArray(overallManufactureYearLabels)).append(",\n");
        script.append("      datasets: [cloneDataset(overallManufactureYearSeries.quoted)]\n");
        script.append("    },\n");
        script.append("    options: trendLineOptions\n");
        script.append("  });\n");
        script.append("  registerChartToggle('overallManufactureYearChart', overallManufactureYearChart, overallManufactureYearSeries);\n");
        script.append("  const overallCustomerAgeChart = new Chart(document.getElementById('overallCustomerAgeChart'), {\n");
        script.append("    type: 'line',\n");
        script.append("    data: {\n");
        script.append("      labels: ").append(toJsStringArray(overallCustomerAgeLabels)).append(",\n");
        script.append("      datasets: [cloneDataset(overallCustomerAgeSeries.quoted)]\n");
        script.append("    },\n");
        script.append("    options: trendLineOptions\n");
        script.append("  });\n");
        script.append("  registerChartToggle('overallCustomerAgeChart', overallCustomerAgeChart, overallCustomerAgeSeries);\n");
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

    private void populateOutcomeRatioData(Map<String, QuoteStatistics.OutcomeBreakdown> outcomes,
                                          List<String> labels,
                                          List<Double> successRatios,
                                          List<Double> failureRatios) {
        if (outcomes.isEmpty()) {
            labels.add("No Data");
            successRatios.add(0.0);
            failureRatios.add(0.0);
            return;
        }
        for (Map.Entry<String, QuoteStatistics.OutcomeBreakdown> entry : outcomes.entrySet()) {
            QuoteStatistics.OutcomeBreakdown breakdown = entry.getValue();
            long total = breakdown.getProcessedTotal();
            double successRatio = total == 0 ? 0.0 : (breakdown.getSuccessCount() * 100.0) / total;
            double failureRatio = total == 0 ? 0.0 : (breakdown.getFailureCount() * 100.0) / total;
            labels.add(entry.getKey());
            successRatios.add(successRatio);
            failureRatios.add(failureRatio);
        }
    }

    private void populateSalesChartData(List<QuoteStatistics.SalesConversionStats> stats,
                                         List<String> labels,
                                         List<Long> totalRequests,
                                         List<Long> successfulQuotes,
                                         List<Long> soldPolicies) {
        if (stats.isEmpty()) {
            labels.add("No Data");
            totalRequests.add(0L);
            successfulQuotes.add(0L);
            soldPolicies.add(0L);
            return;
        }
        for (QuoteStatistics.SalesConversionStats stat : stats) {
            labels.add(stat.getLabel());
            totalRequests.add(stat.getTotalRequests());
            successfulQuotes.add(stat.getSuccessfulQuotes());
            soldPolicies.add(stat.getSoldPolicies());
        }
    }

    private void populatePremiumChartData(List<QuoteStatistics.SalesPremiumBreakdown> breakdowns,
                                          List<String> labels,
                                          List<Long> policies,
                                          List<BigDecimal> premiums) {
        if (breakdowns.isEmpty()) {
            labels.add("No Data");
            policies.add(0L);
            premiums.add(BigDecimal.ZERO);
            return;
        }
        for (QuoteStatistics.SalesPremiumBreakdown breakdown : breakdowns) {
            labels.add(breakdown.getLabel());
            policies.add(breakdown.getSoldPolicies());
            premiums.add(breakdown.getTotalPremium());
        }
    }

    private static String formatPercentage(long numerator, long denominator) {
        if (denominator == 0) {
            return PERCENT_FORMAT.format(0);
        }
        double ratio = (double) numerator / denominator;
        return PERCENT_FORMAT.format(ratio);
    }

    private static String formatCurrency(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return "AED " + CURRENCY_FORMAT.format(safeValue);
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
