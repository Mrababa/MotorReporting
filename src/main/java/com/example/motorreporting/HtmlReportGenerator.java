package com.example.motorreporting;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Builds the executive HTML quote report.
 */
public class HtmlReportGenerator {

    private static final DecimalFormat INTEGER_FORMAT;
    private static final DecimalFormat PERCENT_FORMAT;
    private static final DecimalFormat CURRENCY_FORMAT;
    private static final DateTimeFormatter TREND_LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        INTEGER_FORMAT = new DecimalFormat("#,##0", symbols);
        INTEGER_FORMAT.setRoundingMode(RoundingMode.HALF_UP);

        PERCENT_FORMAT = new DecimalFormat("0.0", symbols);
        PERCENT_FORMAT.setRoundingMode(RoundingMode.HALF_UP);

        CURRENCY_FORMAT = new DecimalFormat("#,##0.00", symbols);
        CURRENCY_FORMAT.setRoundingMode(RoundingMode.HALF_UP);
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

        List<QuoteRecord> tplFailedRecords = records.stream()
                .filter(record -> record.belongsTo(GroupType.TPL) && record.isFailure())
                .collect(Collectors.toList());
        List<QuoteRecord> compFailedRecords = records.stream()
                .filter(record -> record.belongsTo(GroupType.COMPREHENSIVE) && record.isFailure())
                .collect(Collectors.toList());

        ChartData tplFailureReasonsChart = buildFailureReasonData(tplStats);
        ChartData compFailureReasonsChart = buildFailureReasonData(compStats);

        ChartData tplFailureByYearChart = buildFailureByYearData(tplStats);
        ChartData compFailureByYearChart = buildFailureByYearData(compStats);

        ChartData distributionChart = buildDistributionData(tplStats, compStats);
        TrendData trendData = buildTrendData(records);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"utf-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        html.append("    <title>Quote Generation Report</title>\n");
        html.append("    <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css\" integrity=\"sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN\" crossorigin=\"anonymous\">\n");
        html.append("    <link rel=\"stylesheet\" href=\"https://cdn.datatables.net/1.13.6/css/dataTables.bootstrap5.min.css\">\n");
        html.append("    <link rel=\"stylesheet\" href=\"https://cdn.datatables.net/buttons/2.4.1/css/buttons.bootstrap5.min.css\">\n");
        html.append("    <style>\n");
        html.append("        body {\n");
        html.append("            font-family: 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;\n");
        html.append("            background-color: #f3f5f9;\n");
        html.append("            color: #212529;\n");
        html.append("        }\n");
        html.append("        .navbar-brand {\n");
        html.append("            font-weight: 600;\n");
        html.append("            letter-spacing: 0.05em;\n");
        html.append("        }\n");
        html.append("        .nav-link {\n");
        html.append("            font-weight: 500;\n");
        html.append("            text-transform: uppercase;\n");
        html.append("            font-size: 0.85rem;\n");
        html.append("        }\n");
        html.append("        main {\n");
        html.append("            padding-top: 5rem;\n");
        html.append("        }\n");
        html.append("        .section-heading {\n");
        html.append("            font-size: 2rem;\n");
        html.append("            font-weight: 700;\n");
        html.append("            color: #0d1b3e;\n");
        html.append("        }\n");
        html.append("        .kpi-card {\n");
        html.append("            border: none;\n");
        html.append("            border-radius: 1rem;\n");
        html.append("            box-shadow: 0 1rem 3rem rgba(15, 23, 42, 0.15);\n");
        html.append("            background: linear-gradient(135deg, #ffffff 0%, #f7f9fc 100%);\n");
        html.append("            position: relative;\n");
        html.append("            overflow: hidden;\n");
        html.append("        }\n");
        html.append("        .kpi-card::after {\n");
        html.append("            content: '';\n");
        html.append("            position: absolute;\n");
        html.append("            width: 120px;\n");
        html.append("            height: 120px;\n");
        html.append("            border-radius: 50%;\n");
        html.append("            top: -60px;\n");
        html.append("            right: -30px;\n");
        html.append("            opacity: 0.25;\n");
        html.append("        }\n");
        html.append("        .kpi-neutral::after { background: #0d6efd; }\n");
        html.append("        .kpi-success::after { background: #198754; }\n");
        html.append("        .kpi-danger::after { background: #dc3545; }\n");
        html.append("        .kpi-card .card-body {\n");
        html.append("            padding: 1.75rem;\n");
        html.append("        }\n");
        html.append("        .kpi-icon {\n");
        html.append("            font-size: 2.5rem;\n");
        html.append("            margin-right: 1rem;\n");
        html.append("        }\n");
        html.append("        .metric-label {\n");
        html.append("            text-transform: uppercase;\n");
        html.append("            font-weight: 600;\n");
        html.append("            font-size: 0.85rem;\n");
            html.append("            color: #6c757d;\n");
        html.append("        }\n");
        html.append("        .metric-value {\n");
        html.append("            font-size: 2.25rem;\n");
        html.append("            font-weight: 700;\n");
        html.append("            color: #0d1b3e;\n");
        html.append("        }\n");
        html.append("        .card.shadow-sm {\n");
        html.append("            border: none;\n");
            html.append("            border-radius: 1rem;\n");
        html.append("        }\n");
        html.append("        .card-header {\n");
        html.append("            background: transparent;\n");
        html.append("            border-bottom: none;\n");
            html.append("            padding-bottom: 0;\n");
        html.append("        }\n");
        html.append("        .table thead {\n");
        html.append("            background-color: #0d1b3e;\n");
        html.append("            color: #fff;\n");
        html.append("        }\n");
        html.append("        .table thead th {\n");
        html.append("            font-size: 0.85rem;\n");
        html.append("            text-transform: uppercase;\n");
        html.append("            letter-spacing: 0.05em;\n");
        html.append("        }\n");
        html.append("        .dataTables_wrapper .dt-buttons .btn {\n");
        html.append("            margin-right: 0.35rem;\n");
        html.append("            border-radius: 999px;\n");
        html.append("        }\n");
        html.append("        .section-description {\n");
        html.append("            color: #6c757d;\n");
            html.append("            max-width: 720px;\n");
        html.append("        }\n");
        html.append("        footer {\n");
        html.append("            padding: 2rem 0;\n");
        html.append("            text-align: center;\n");
        html.append("            color: #6c757d;\n");
        html.append("            font-size: 0.9rem;\n");
        html.append("        }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<nav class=\"navbar navbar-expand-lg navbar-dark bg-dark fixed-top\">\n");
        html.append("  <div class=\"container-fluid\">\n");
        html.append("    <a class=\"navbar-brand\" href=\"#home\">Quote Report</a>\n");
        html.append("    <button class=\"navbar-toggler\" type=\"button\" data-bs-toggle=\"collapse\" data-bs-target=\"#navbarNav\" aria-controls=\"navbarNav\" aria-expanded=\"false\" aria-label=\"Toggle navigation\">\n");
        html.append("      <span class=\"navbar-toggler-icon\"></span>\n");
        html.append("    </button>\n");
        html.append("    <div class=\"collapse navbar-collapse\" id=\"navbarNav\">\n");
        html.append("      <ul class=\"navbar-nav ms-auto\">\n");
        html.append("        <li class=\"nav-item\"><a class=\"nav-link\" href=\"#home\">Home</a></li>\n");
        html.append("        <li class=\"nav-item\"><a class=\"nav-link\" href=\"#tpl\">TPL Report</a></li>\n");
        html.append("        <li class=\"nav-item\"><a class=\"nav-link\" href=\"#comp\">Comprehensive Report</a></li>\n");
        html.append("        <li class=\"nav-item\"><a class=\"nav-link\" href=\"#failures\">Failure Reasons</a></li>\n");
        html.append("      </ul>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");
        html.append("</nav>\n");
        html.append("<main>\n");

        appendHomeSection(html, statistics, tplStats, compStats, distributionChart, trendData);
        appendGroupSection(html, tplStats, tplFailureReasonsChart, tplFailureByYearChart,
                tplFailedRecords, "tplFailuresTable", "Third Party Liability (TPL)");
        appendGroupSection(html, compStats, compFailureReasonsChart, compFailureByYearChart,
                compFailedRecords, "compFailuresTable", "Comprehensive");
        appendFailureReasonsSection(html, statistics);

        html.append("</main>\n");
        html.append("<footer class=\"bg-transparent\">Generated automatically from the motor quote dataset.</footer>\n");
        html.append(buildScripts(distributionChart, tplFailureReasonsChart, compFailureReasonsChart,
                tplFailureByYearChart, compFailureByYearChart, trendData));
        html.append("</body>\n");
        html.append("</html>\n");
        return html.toString();
    }

    private void appendHomeSection(StringBuilder html,
                                   QuoteStatistics statistics,
                                   QuoteGroupStats tplStats,
                                   QuoteGroupStats compStats,
                                   ChartData distributionChart,
                                   TrendData trendData) {
        html.append("<section id=\"home\" class=\"container py-5\">\n");
        html.append("  <div class=\"mb-4\">\n");
        html.append("    <h1 class=\"section-heading mb-2\">Quote Generation Overview</h1>\n");
        html.append("    <p class=\"section-description\">Executive dashboard summarising quote performance across Third Party Liability and Comprehensive insurance types.</p>\n");
        html.append("  </div>\n");
        html.append("  <div class=\"row g-4\">\n");
        appendKpiCard(html, "Total Quotes", "📊", formatInteger(statistics.getOverallTotalQuotes()), "kpi-neutral");
        appendKpiCard(html, "Pass Count", "✅", formatInteger(statistics.getOverallPassCount()), "kpi-success");
        appendKpiCard(html, "Fail Count", "❌", formatInteger(statistics.getOverallFailCount()), "kpi-danger");
        appendKpiCard(html, "Fail %", "❌", formatPercentage(statistics.getOverallFailurePercentage()), "kpi-danger");
        appendKpiCard(html, "Total Estimated Value Lost", "📊", formatCurrency(statistics.getOverallBlockedEstimatedValue()), "kpi-neutral");
        html.append("  </div>\n");

        html.append("  <div class=\"row g-4 mt-1\">\n");
        html.append("    <div class=\"col-xl-4\">\n");
        html.append("      <div class=\"card shadow-sm h-100\">\n");
        html.append("        <div class=\"card-header\">\n");
        html.append("          <h6 class=\"text-uppercase text-muted mb-1\">Quote Mix</h6>\n");
        html.append("          <h4 class=\"fw-semibold mb-0\">TPL vs Comprehensive</h4>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"card-body\">\n");
        html.append("          <canvas id=\"distributionChart\" height=\"260\"></canvas>\n");
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"col-xl-8\">\n");
        html.append("      <div class=\"card shadow-sm h-100\">\n");
        html.append("        <div class=\"card-header\">\n");
        html.append("          <h6 class=\"text-uppercase text-muted mb-1\">Quote Requests Trend</h6>\n");
        html.append("          <h4 class=\"fw-semibold mb-0\">Daily Request &amp; Outcome Trend</h4>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"card-body\">\n");
        if (trendData.labels().isEmpty()) {
            html.append("          <div class=\"alert alert-light border fw-semibold mb-0\">No quote request dates available for trend analysis.</div>\n");
        } else {
            html.append("          <canvas id=\"quoteTrendChart\" height=\"260\"></canvas>\n");
        }
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");

        html.append("  <div class=\"row g-4 mt-1\">\n");
        appendGroupSummaryCard(html, tplStats, "col-xl-6");
        appendGroupSummaryCard(html, compStats, "col-xl-6");
        html.append("  </div>\n");
        html.append("</section>\n");
    }

    private void appendGroupSection(StringBuilder html,
                                    QuoteGroupStats stats,
                                    ChartData failureReasonsChart,
                                    ChartData failuresByYearChart,
                                    List<QuoteRecord> failedRecords,
                                    String tableId,
                                    String heading) {
        html.append("<section id=\"" + (stats.getGroupType() == GroupType.TPL ? "tpl" : "comp") + "\" class=\"container py-5\">\n");
        html.append("  <div class=\"mb-4\">\n");
        html.append("    <h2 class=\"section-heading mb-2\">" + escapeHtml(heading) + " Report</h2>\n");
        html.append("    <p class=\"section-description\">Detailed breakdown of quote outcomes, reasons for failure, and value impact for the "
                + escapeHtml(stats.getGroupType().getDisplayName()) + " segment.</p>\n");
        html.append("  </div>\n");

        html.append("  <div class=\"row g-4\">\n");
        html.append("    <div class=\"col-lg-4\">\n");
        html.append("      <div class=\"card shadow-sm h-100\">\n");
        html.append("        <div class=\"card-header\">\n");
        html.append("          <h6 class=\"text-uppercase text-muted mb-1\">Key Metrics</h6>\n");
        html.append("          <h4 class=\"fw-semibold mb-0\">" + escapeHtml(stats.getGroupType().getDisplayName()) + "</h4>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"card-body\">\n");
        html.append("          <div class=\"table-responsive\">\n");
        html.append("            <table class=\"table table-borderless mb-0\">\n");
        html.append("              <tbody>\n");
        appendMetricRow(html, "Total Quotes", formatInteger(stats.getTotalQuotes()));
        appendMetricRow(html, "Pass Count", formatInteger(stats.getPassCount()));
        appendMetricRow(html, "Fail Count", formatInteger(stats.getFailCount()));
        appendMetricRow(html, "Fail %", formatPercentage(stats.getFailurePercentage()));
        appendMetricRow(html, "Estimated Value Blocked", formatCurrency(stats.getTotalBlockedEstimatedValue()));
        html.append("              </tbody>\n");
        html.append("            </table>\n");
        html.append("          </div>\n");
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");

        html.append("    <div class=\"col-lg-4\">\n");
        html.append("      <div class=\"card shadow-sm h-100\">\n");
        html.append("        <div class=\"card-header\">\n");
        html.append("          <h6 class=\"text-uppercase text-muted mb-1\">Failure Mix</h6>\n");
        html.append("          <h4 class=\"fw-semibold mb-0\">Failure Reasons</h4>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"card-body\">\n");
        html.append("          <canvas id=\"" + failureReasonsChart.canvasId() + "\" height=\"260\"></canvas>\n");
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");

        html.append("    <div class=\"col-lg-4\">\n");
        html.append("      <div class=\"card shadow-sm h-100\">\n");
        html.append("        <div class=\"card-header\">\n");
        html.append("          <h6 class=\"text-uppercase text-muted mb-1\">Vehicle Profile</h6>\n");
        html.append("          <h4 class=\"fw-semibold mb-0\">Failures by Manufacture Year</h4>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"card-body\">\n");
        html.append("          <canvas id=\"" + failuresByYearChart.canvasId() + "\" height=\"260\"></canvas>\n");
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");

        html.append("  <div class=\"card shadow-sm mt-4\">\n");
        html.append("    <div class=\"card-header\">\n");
        html.append("      <h6 class=\"text-uppercase text-muted mb-1\">Failed Quotes</h6>\n");
        html.append("      <h4 class=\"fw-semibold mb-0\">Error Details by Quote</h4>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"card-body\">\n");
        html.append("      <div class=\"table-responsive\">\n");
        html.append("        <table id=\"" + tableId + "\" class=\"table table-striped table-bordered align-middle\">\n");
        html.append("          <thead>\n");
        html.append("            <tr><th>Reference No</th><th>Manufacture Year</th><th>Estimated Value</th><th>Error Text</th></tr>\n");
        html.append("          </thead>\n");
        html.append("          <tbody>\n");
        appendFailedRecords(html, failedRecords);
        html.append("          </tbody>\n");
        html.append("        </table>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");

        html.append("</section>\n");
    }

    private void appendFailureReasonsSection(StringBuilder html, QuoteStatistics statistics) {
        html.append("<section id=\"failures\" class=\"container py-5\">\n");
        html.append("  <div class=\"mb-4\">\n");
        html.append("    <h2 class=\"section-heading mb-2\">Failure Reasons</h2>\n");
        html.append("    <p class=\"section-description\">Complete inventory of failure errors across both insurance types, sorted by frequency with relative impact.</p>\n");
        html.append("  </div>\n");

        html.append("  <div class=\"card shadow-sm\">\n");
        html.append("    <div class=\"card-header\">\n");
        html.append("      <h6 class=\"text-uppercase text-muted mb-1\">Error Catalogue</h6>\n");
        html.append("      <h4 class=\"fw-semibold mb-0\">Most Common Issues</h4>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"card-body\">\n");
        html.append("      <div class=\"table-responsive\">\n");
        html.append("        <table id=\"failureReasonsTable\" class=\"table table-striped table-bordered align-middle\">\n");
        html.append("          <thead>\n");
        html.append("            <tr><th>Error Text</th><th>Occurrences</th><th>% of Failures</th></tr>\n");
        html.append("          </thead>\n");
        html.append("          <tbody>\n");

        long totalFailures = Math.max(1, statistics.getOverallFailCount());
        for (Map.Entry<String, Long> entry : statistics.getCombinedFailureReasons().entrySet()) {
            long count = entry.getValue();
            double percentage = (count * 100.0) / totalFailures;
            html.append("            <tr>");
            html.append("<td>" + escapeHtml(entry.getKey()) + "</td>");
            html.append("<td data-order=\"" + count + "\">" + formatInteger(count) + "</td>");
            html.append("<td data-order=\"" + percentage + "\">" + formatPercentage(percentage) + "</td>");
            html.append("</tr>\n");
        }

        html.append("          </tbody>\n");
        html.append("        </table>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");
        html.append("</section>\n");
    }

    private void appendKpiCard(StringBuilder html, String label, String icon, String value, String modifier) {
        html.append("    <div class=\"col-sm-6 col-xl-3\">\n");
        html.append("      <div class=\"card kpi-card " + modifier + " h-100\">\n");
        html.append("        <div class=\"card-body d-flex align-items-center\">\n");
        html.append("          <div class=\"kpi-icon\">" + escapeHtml(icon) + "</div>\n");
        html.append("          <div>\n");
        html.append("            <div class=\"metric-label\">" + escapeHtml(label) + "</div>\n");
        html.append("            <div class=\"metric-value\">" + escapeHtml(value) + "</div>\n");
        html.append("          </div>\n");
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
    }

    private void appendGroupSummaryCard(StringBuilder html, QuoteGroupStats stats, String columnClass) {
        html.append("    <div class=\"" + columnClass + "\">\n");
        html.append("      <div class=\"card shadow-sm h-100\">\n");
        html.append("        <div class=\"card-header\">\n");
        html.append("          <h6 class=\"text-uppercase text-muted mb-1\">" + escapeHtml(stats.getGroupType().getShortLabel()) + " Snapshot</h6>\n");
        html.append("          <h4 class=\"fw-semibold mb-0\">" + escapeHtml(stats.getGroupType().getDisplayName()) + "</h4>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"card-body\">\n");
        html.append("          <div class=\"row g-3\">\n");
        appendMiniMetric(html, "Total", formatInteger(stats.getTotalQuotes()));
        appendMiniMetric(html, "Pass", formatInteger(stats.getPassCount()));
        appendMiniMetric(html, "Fail", formatInteger(stats.getFailCount()));
        appendMiniMetric(html, "Fail %", formatPercentage(stats.getFailurePercentage()));
        appendMiniMetric(html, "Value Lost", formatCurrency(stats.getTotalBlockedEstimatedValue()));
        html.append("          </div>\n");
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
    }

    private void appendMiniMetric(StringBuilder html, String label, String value) {
        html.append("            <div class=\"col-6\">\n");
        html.append("              <div class=\"small text-uppercase fw-semibold text-muted\">" + escapeHtml(label) + "</div>\n");
        html.append("              <div class=\"fs-5 fw-bold\">" + escapeHtml(value) + "</div>\n");
        html.append("            </div>\n");
    }

    private void appendMetricRow(StringBuilder html, String label, String value) {
        html.append("                <tr><th scope=\"row\" class=\"fw-semibold text-muted\">" + escapeHtml(label) + "</th><td class=\"fw-bold\">" + escapeHtml(value) + "</td></tr>\n");
    }

    private void appendFailedRecords(StringBuilder html, List<QuoteRecord> records) {
        for (QuoteRecord record : records) {
            String reference = findValue(record, "ReferenceNumber", "ReferenceNo", "QuotationNo", "QuoteNo", "QuoteNumber");
            if (reference.isBlank()) {
                reference = record.getQuoteNumber().orElse("-");
            }
            String manufactureYear = record.getManufactureYear().map(String::valueOf).orElse("Unknown");
            BigDecimal estimatedValue = record.getEstimatedValue();
            html.append("            <tr>");
            html.append("<td>" + escapeHtml(reference) + "</td>");
            html.append("<td>" + escapeHtml(manufactureYear) + "</td>");
            html.append("<td data-order=\"" + estimatedValue + "\">" + escapeHtml(formatCurrency(estimatedValue)) + "</td>");
            html.append("<td>" + escapeHtml(record.getFailureReason()) + "</td>");
            html.append("</tr>\n");
        }
    }

    private String buildScripts(ChartData distributionChart,
                                ChartData tplFailureReasonsChart,
                                ChartData compFailureReasonsChart,
                                ChartData tplFailureByYearChart,
                                ChartData compFailureByYearChart,
                                TrendData trendData) {
        StringBuilder script = new StringBuilder();
        script.append("<script src=\"https://code.jquery.com/jquery-3.7.1.min.js\"></script>\n");
        script.append("<script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js\" integrity=\"sha384-C6RzsynM9kWDrMNeT87bh95OGNyZPhcTNXj1NW7RuBCsyN/o0jlpcV8Qyq46cDfL\" crossorigin=\"anonymous\"></script>\n");
        script.append("<script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js\"></script>\n");
        script.append("<script src=\"https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js\"></script>\n");
        script.append("<script src=\"https://cdn.datatables.net/1.13.6/js/dataTables.bootstrap5.min.js\"></script>\n");
        script.append("<script src=\"https://cdn.datatables.net/buttons/2.4.1/js/dataTables.buttons.min.js\"></script>\n");
        script.append("<script src=\"https://cdn.datatables.net/buttons/2.4.1/js/buttons.bootstrap5.min.js\"></script>\n");
        script.append("<script src=\"https://cdnjs.cloudflare.com/ajax/libs/jszip/3.10.1/jszip.min.js\"></script>\n");
        script.append("<script src=\"https://cdn.datatables.net/buttons/2.4.1/js/buttons.html5.min.js\"></script>\n");
        script.append("<script src=\"https://cdn.datatables.net/buttons/2.4.1/js/buttons.print.min.js\"></script>\n");

        script.append("<script>\n");
        script.append("  const distributionData = { labels: " + distributionChart.labelsJson() + ", datasets: [{ data: " + distributionChart.valuesJson() + ", backgroundColor: " + distributionChart.colorsJson() + " }] };\n");
        script.append("  const tplFailureReasonsData = { labels: " + tplFailureReasonsChart.labelsJson() + ", datasets: [{ data: " + tplFailureReasonsChart.valuesJson() + ", backgroundColor: " + tplFailureReasonsChart.colorsJson() + " }] };\n");
        script.append("  const compFailureReasonsData = { labels: " + compFailureReasonsChart.labelsJson() + ", datasets: [{ data: " + compFailureReasonsChart.valuesJson() + ", backgroundColor: " + compFailureReasonsChart.colorsJson() + " }] };\n");
        script.append("  const tplFailureByYearData = { labels: " + tplFailureByYearChart.labelsJson() + ", datasets: [{ data: " + tplFailureByYearChart.valuesJson() + ", backgroundColor: " + tplFailureByYearChart.colorsJson() + " }] };\n");
        script.append("  const compFailureByYearData = { labels: " + compFailureByYearChart.labelsJson() + ", datasets: [{ data: " + compFailureByYearChart.valuesJson() + ", backgroundColor: " + compFailureByYearChart.colorsJson() + " }] };\n");
        script.append("  const trendData = { labels: " + trendData.labelsJson() + ", totals: " + trendData.totalJson() + ", successes: " + trendData.successJson() + ", failures: " + trendData.failureJson() + " };\n");

        script.append("  function buildPieChart(canvasId, data) {\n");
        script.append("    const canvas = document.getElementById(canvasId);\n");
        script.append("    if (!canvas) { return; }\n");
        script.append("    return new Chart(canvas, {\n");
        script.append("      type: 'pie',\n");
        script.append("      data: data,\n");
        script.append("      options: {\n");
        script.append("        plugins: {\n");
        script.append("          legend: { position: 'bottom' },\n");
        script.append("          tooltip: { callbacks: { label: ctx => ctx.label + ': ' + ctx.parsed + ' quotes' } }\n");
        script.append("        }\n");
        script.append("      }\n");
        script.append("    });\n");
        script.append("  }\n");

        script.append("  function buildBarChart(canvasId, data, label) {\n");
        script.append("    const canvas = document.getElementById(canvasId);\n");
        script.append("    if (!canvas) { return; }\n");
        script.append("    return new Chart(canvas, {\n");
        script.append("      type: 'bar',\n");
        script.append("      data: { labels: data.labels, datasets: [{ label: label, data: data.datasets[0].data, backgroundColor: data.datasets[0].backgroundColor }] },\n");
        script.append("      options: {\n");
        script.append("        plugins: { legend: { display: false } },\n");
        script.append("        scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }\n");
        script.append("      }\n");
        script.append("    });\n");
        script.append("  }\n");

        script.append("  function buildTrendChart(canvasId, trend) {\n");
        script.append("    const canvas = document.getElementById(canvasId);\n");
        script.append("    if (!canvas || trend.labels.length === 0) { return; }\n");
        script.append("    return new Chart(canvas, {\n");
        script.append("      type: 'line',\n");
        script.append("      data: {\n");
        script.append("        labels: trend.labels,\n");
        script.append("        datasets: [\n");
        script.append("          { label: 'Total Requests', data: trend.totals, borderColor: '#0d6efd', backgroundColor: 'rgba(13,110,253,0.15)', fill: true, tension: 0.35, borderWidth: 2 },\n");
        script.append("          { label: 'Successful Quotes', data: trend.successes, borderColor: '#198754', backgroundColor: 'rgba(25,135,84,0.15)', fill: true, tension: 0.35, borderWidth: 2 },\n");
        script.append("          { label: 'Failed Quotes', data: trend.failures, borderColor: '#dc3545', backgroundColor: 'rgba(220,53,69,0.15)', fill: true, tension: 0.35, borderWidth: 2 }\n");
        script.append("        ]\n");
        script.append("      },\n");
        script.append("      options: {\n");
        script.append("        plugins: { legend: { position: 'bottom' } },\n");
        script.append("        scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }\n");
        script.append("      }\n");
        script.append("    });\n");
        script.append("  }\n");

        script.append("  $(function() {\n");
        script.append("    buildPieChart('distributionChart', distributionData);\n");
        script.append("    buildPieChart('tplFailureReasonsChart', tplFailureReasonsData);\n");
        script.append("    buildPieChart('compFailureReasonsChart', compFailureReasonsData);\n");
        script.append("    buildBarChart('tplFailureByYearChart', tplFailureByYearData, 'TPL Failures');\n");
        script.append("    buildBarChart('compFailureByYearChart', compFailureByYearData, 'Comprehensive Failures');\n");
        script.append("    buildTrendChart('quoteTrendChart', trendData);\n");

        script.append("    function initTable(id, exportName, orderColumn) {\n");
        script.append("      const options = {\n");
        script.append("        dom: 'Bfrtip',\n");
        script.append("        buttons: [\n");
        script.append("          { extend: 'csvHtml5', className: 'btn btn-sm btn-outline-primary', title: exportName },\n");
        script.append("          { extend: 'excelHtml5', className: 'btn btn-sm btn-outline-primary', title: exportName },\n");
        script.append("          { extend: 'print', className: 'btn btn-sm btn-outline-primary', title: exportName }\n");
        script.append("        ],\n");
        script.append("        pageLength: 10,\n");
        script.append("        lengthMenu: [[5, 10, 20, -1], [5, 10, 20, 'All']],\n");
        script.append("        order: orderColumn !== null ? [[orderColumn, 'desc']] : []\n");
        script.append("      };\n");
        script.append("      $(id).DataTable(options);\n");
        script.append("    }\n");

        script.append("    initTable('#tplFailuresTable', 'TPL_Failure_Details', 2);\n");
        script.append("    initTable('#compFailuresTable', 'Comprehensive_Failure_Details', 2);\n");
        script.append("    initTable('#failureReasonsTable', 'Failure_Reasons', 1);\n");
        script.append("  });\n");
        script.append("});\n");
        script.append("</script>\n");
        return script.toString();
    }

    private ChartData buildFailureReasonData(QuoteGroupStats stats) {
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        stats.getFailureReasonCounts().forEach((reason, count) -> {
            labels.add(reason);
            values.add(count);
        });
        if (labels.isEmpty()) {
            labels = List.of("No Failures Recorded");
            values = List.of(1L);
        }
        List<String> colors = createPalette(labels.size());
        String canvasId = stats.getGroupType() == GroupType.TPL ? "tplFailureReasonsChart" : "compFailureReasonsChart";
        return new ChartData(canvasId, toJsonArrayOfStrings(labels), toJsonArrayOfNumbers(values), toJsonArrayOfStrings(colors));
    }

    private ChartData buildFailureByYearData(QuoteGroupStats stats) {
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        stats.getFailuresByManufactureYear().forEach((year, count) -> {
            labels.add(year);
            values.add(count);
        });
        if (labels.isEmpty()) {
            labels = List.of("No Data");
            values = List.of(0L);
        }
        List<String> colors = Collections.nCopies(labels.size(), stats.getGroupType() == GroupType.TPL ? "rgba(13, 110, 253, 0.85)" : "rgba(220, 53, 69, 0.85)");
        String canvasId = stats.getGroupType() == GroupType.TPL ? "tplFailureByYearChart" : "compFailureByYearChart";
        return new ChartData(canvasId, toJsonArrayOfStrings(labels), toJsonArrayOfNumbers(values), toJsonArrayOfStrings(colors));
    }

    private ChartData buildDistributionData(QuoteGroupStats tplStats, QuoteGroupStats compStats) {
        List<String> labels = List.of(tplStats.getGroupType().getDisplayName(), compStats.getGroupType().getDisplayName());
        List<Long> values = List.of(tplStats.getTotalQuotes(), compStats.getTotalQuotes());
        List<String> colors = Arrays.asList("#0d6efd", "#198754");
        return new ChartData("distributionChart", toJsonArrayOfStrings(labels), toJsonArrayOfNumbers(values), toJsonArrayOfStrings(colors));
    }

    private TrendData buildTrendData(List<QuoteRecord> records) {
        SortedMap<LocalDate, long[]> dailyCounts = new TreeMap<>();
        for (QuoteRecord record : records) {
            Optional<LocalDate> requestDate = extractRequestDate(record);
            if (requestDate.isEmpty()) {
                continue;
            }
            LocalDate date = requestDate.get();
            long[] counts = dailyCounts.computeIfAbsent(date, key -> new long[3]);
            counts[0]++;
            if (record.isSuccessful()) {
                counts[1]++;
            } else if (record.isFailure()) {
                counts[2]++;
            }
        }

        List<String> labels = new ArrayList<>();
        List<Long> totals = new ArrayList<>();
        List<Long> successes = new ArrayList<>();
        List<Long> failures = new ArrayList<>();
        dailyCounts.forEach((date, counts) -> {
            labels.add(date.format(TREND_LABEL_FORMAT));
            totals.add(counts[0]);
            successes.add(counts[1]);
            failures.add(counts[2]);
        });

        return new TrendData(labels,
                toJsonArrayOfStrings(labels),
                toJsonArrayOfNumbers(totals),
                toJsonArrayOfNumbers(successes),
                toJsonArrayOfNumbers(failures));
    }

    private Optional<LocalDate> extractRequestDate(QuoteRecord record) {
        String raw = findValue(record, "QuoteRequestedOn", "RequestDate", "CreatedOn");
        if (raw.isBlank()) {
            return Optional.empty();
        }
        String value = raw.trim();
        List<DateTimeFormatter> dateTimeFormatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        );
        for (DateTimeFormatter formatter : dateTimeFormatters) {
            try {
                return Optional.of(LocalDateTime.parse(value, formatter).toLocalDate());
            } catch (DateTimeParseException ignored) {
            }
        }
        List<DateTimeFormatter> dateFormatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
        );
        for (DateTimeFormatter formatter : dateFormatters) {
            try {
                return Optional.of(LocalDate.parse(value, formatter));
            } catch (DateTimeParseException ignored) {
            }
        }
        return Optional.empty();
    }

    private String findValue(QuoteRecord record, String... keys) {
        Map<String, String> rawValues = record.getRawValues();
        for (String key : keys) {
            for (Map.Entry<String, String> entry : rawValues.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue() == null ? "" : entry.getValue();
                }
            }
        }
        return "";
    }

    private static List<String> createPalette(int size) {
        List<String> base = Arrays.asList(
                "#0d6efd", "#6610f2", "#6f42c1", "#d63384", "#dc3545",
                "#fd7e14", "#ffc107", "#198754", "#20c997", "#0dcaf0",
                "#495057", "#343a40"
        );
        List<String> palette = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            palette.add(base.get(i % base.size()));
        }
        return palette;
    }

    private static String toJsonArrayOfStrings(List<String> values) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(toJsonString(values.get(i)));
        }
        builder.append(']');
        return builder.toString();
    }

    private static String toJsonArrayOfNumbers(List<? extends Number> values) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            Number value = values.get(i);
            builder.append(value == null ? "0" : value.toString());
        }
        builder.append(']');
        return builder.toString();
    }

    private static String toJsonString(String value) {
        if (value == null) {
            return "\"\"";
        }
        StringBuilder builder = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\"':
                    builder.append("\\\"");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
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
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                    break;
            }
        }
        builder.append('"');
        return builder.toString();
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

    private static String formatCurrency(BigDecimal value) {
        return CURRENCY_FORMAT.format(value);
    }

    private static String formatPercentage(double value) {
        return PERCENT_FORMAT.format(value) + "%";
    }

    private static final class ChartData {
        private final String canvasId;
        private final String labelsJson;
        private final String valuesJson;
        private final String colorsJson;

        private ChartData(String canvasId, String labelsJson, String valuesJson, String colorsJson) {
            this.canvasId = canvasId;
            this.labelsJson = labelsJson;
            this.valuesJson = valuesJson;
            this.colorsJson = colorsJson;
        }

        private String canvasId() {
            return canvasId;
        }

        private String labelsJson() {
            return labelsJson;
        }

        private String valuesJson() {
            return valuesJson;
        }

        private String colorsJson() {
            return colorsJson;
        }
    }

    private static final class TrendData {
        private final List<String> labels;
        private final String labelsJson;
        private final String totalJson;
        private final String successJson;
        private final String failureJson;

        private TrendData(List<String> labels, String labelsJson, String totalJson, String successJson, String failureJson) {
            this.labels = labels;
            this.labelsJson = labelsJson;
            this.totalJson = totalJson;
            this.successJson = successJson;
            this.failureJson = failureJson;
        }

        private List<String> labels() {
            return labels;
        }

        private String labelsJson() {
            return labelsJson;
        }

        private String totalJson() {
            return totalJson;
        }

        private String successJson() {
            return successJson;
        }

        private String failureJson() {
            return failureJson;
        }
    }
}
