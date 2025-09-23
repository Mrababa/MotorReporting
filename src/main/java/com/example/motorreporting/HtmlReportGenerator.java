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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds the simplified HTML quote report.
 */
public class HtmlReportGenerator {

    private static final String LOGO_URL = "https://www.shory.com/imgs/master/logo.svg";
    private static final DecimalFormat INTEGER_FORMAT;
    private static final DateTimeFormatter[] QUOTE_REQUESTED_ON_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };
    private static final DateTimeFormatter HEADER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        INTEGER_FORMAT = new DecimalFormat("#,##0", symbols);
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

        long totalQuotes = statistics.getOverallTotalQuotes();
        long successCount = statistics.getOverallPassCount();
        long failCount = statistics.getOverallFailCount();
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
        html.append("  <section class=\"summary-grid\">\n");
        appendSummaryCard(html, "Total Quotes Requested", totalQuotes, "#2563eb");
        appendSummaryCard(html, "Successful Quotes", successCount, "#16a34a");
        appendSummaryCard(html, "Failed Quotes", failCount, "#dc2626");
        html.append("  </section>\n");
        html.append("  <section class=\"charts\">\n");
        html.append("    <div class=\"chart-grid\">\n");
        html.append("      <div class=\"chart-card\">\n");
        html.append("        <h2>TPL Success vs Failed</h2>\n");
        html.append("        <canvas id=\"tplOutcomesChart\"></canvas>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"chart-card\">\n");
        html.append("        <h2>Comprehensive Success vs Failed</h2>\n");
        html.append("        <canvas id=\"compOutcomesChart\"></canvas>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("  </section>\n");
        html.append("</main>\n");
        html.append(buildScripts(tplStats, compStats));
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
            if (key != null && key.equalsIgnoreCase("QuoteRequestedOn")) {
                String value = entry.getValue();
                if (value == null) {
                    return Optional.empty();
                }
                return parseQuoteRequestedOn(value.trim());
            }
        }
        return Optional.empty();
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

    private String buildScripts(QuoteGroupStats tplStats, QuoteGroupStats compStats) {
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
        script.append("      tooltip: { callbacks: { label: ctx => `${ctx.label}: ${ctx.raw} quotes` } }\n");
        script.append("    }\n");
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
        script.append("  new Chart(document.getElementById('tplOutcomesChart'), {\n");
        script.append("    type: 'bar',\n");
        script.append("    data: tplData,\n");
        script.append("    options: sharedOptions\n");
        script.append("  });\n");
        script.append("  new Chart(document.getElementById('compOutcomesChart'), {\n");
        script.append("    type: 'bar',\n");
        script.append("    data: compData,\n");
        script.append("    options: sharedOptions\n");
        script.append("  });\n");
        script.append("</script>\n");
        return script.toString();
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
