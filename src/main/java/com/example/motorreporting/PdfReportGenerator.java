package com.example.motorreporting;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Builds the PDF report using OpenPDF.
 */
public class PdfReportGenerator {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL);
    private static final Color HEADER_BACKGROUND = new Color(230, 230, 230);
    private static final DecimalFormat INTEGER_FORMAT = new DecimalFormat("#,##0");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.0'%'");
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");

    public void generate(Path outputPath, QuoteStatistics statistics) throws IOException {
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
            PdfWriter.getInstance(document, outputStream);

            try {
                document.open();

                addTitlePage(document);
                addOverallSummary(document, statistics);
                addSummaryChart(document, statistics);
                addSpacing(document);

                addGroupSection(document, statistics.getTplStats());
                addSpacing(document);
                addGroupSection(document, statistics.getComprehensiveStats());
                addSpacing(document);

                addFailureByYearChart(document, statistics);
                addSpacing(document);

                addRecommendations(document, statistics);
            } finally {
                if (document.isOpen()) {
                    document.close();
                }
            }
        } catch (DocumentException ex) {
            throw new IOException("Failed to build PDF report", ex);
        }
    }

    private void addTitlePage(Document document) throws DocumentException {
        Paragraph title = new Paragraph("Quote Generation Report", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph date = new Paragraph("Report Date: " + LocalDate.now(), NORMAL_FONT);
        date.setAlignment(Element.ALIGN_CENTER);
        document.add(date);
        addSpacing(document);
    }

    private void addOverallSummary(Document document, QuoteStatistics statistics) throws DocumentException {
        Paragraph header = new Paragraph("Overall Summary", SECTION_FONT);
        document.add(header);

        PdfPTable table = new PdfPTable(new float[]{3f, 2f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.addCell(createHeaderCell("Metric"));
        table.addCell(createHeaderCell("Value"));

        table.addCell(createValueCell("Total Quotes"));
        table.addCell(createValueCell(INTEGER_FORMAT.format(statistics.getOverallTotalQuotes())));
        table.addCell(createValueCell("Pass Count"));
        table.addCell(createValueCell(INTEGER_FORMAT.format(statistics.getOverallPassCount())));
        table.addCell(createValueCell("Fail Count"));
        table.addCell(createValueCell(INTEGER_FORMAT.format(statistics.getOverallFailCount())));
        table.addCell(createValueCell("Failure %"));
        table.addCell(createValueCell(PERCENT_FORMAT.format(statistics.getOverallFailurePercentage())));
        table.addCell(createValueCell("Blocked Estimated Value"));
        table.addCell(createValueCell(CURRENCY_FORMAT.format(statistics.getOverallBlockedEstimatedValue())));

        document.add(table);
        addSpacing(document);
    }

    private void addGroupSection(Document document, QuoteGroupStats stats) throws DocumentException, IOException {
        Paragraph header = new Paragraph(stats.getGroupType().getDisplayName(), SECTION_FONT);
        document.add(header);

        if (!stats.hasQuotes()) {
            Paragraph empty = new Paragraph("No quotes found for this group.", NORMAL_FONT);
            document.add(empty);
            return;
        }

        PdfPTable metrics = new PdfPTable(new float[]{3f, 2f});
        metrics.setWidthPercentage(100);
        metrics.setSpacingBefore(8f);
        metrics.addCell(createHeaderCell("Metric"));
        metrics.addCell(createHeaderCell("Value"));

        metrics.addCell(createValueCell("Total Quotes"));
        metrics.addCell(createValueCell(INTEGER_FORMAT.format(stats.getTotalQuotes())));
        metrics.addCell(createValueCell("Pass Count"));
        metrics.addCell(createValueCell(INTEGER_FORMAT.format(stats.getPassCount())));
        metrics.addCell(createValueCell("Fail Count"));
        metrics.addCell(createValueCell(INTEGER_FORMAT.format(stats.getFailCount())));
        metrics.addCell(createValueCell("Failure %"));
        metrics.addCell(createValueCell(PERCENT_FORMAT.format(stats.getFailurePercentage())));
        metrics.addCell(createValueCell("Blocked Estimated Value"));
        metrics.addCell(createValueCell(CURRENCY_FORMAT.format(stats.getTotalBlockedEstimatedValue())));

        document.add(metrics);
        addSpacing(document);

        Paragraph breakdownHeader = new Paragraph("Failure Breakdown", SUBTITLE_FONT);
        document.add(breakdownHeader);

        PdfPTable breakdownTable = new PdfPTable(new float[]{4f, 1f});
        breakdownTable.setWidthPercentage(100);
        breakdownTable.setSpacingBefore(6f);
        breakdownTable.addCell(createHeaderCell("Error Text"));
        breakdownTable.addCell(createHeaderCell("Count"));

        if (stats.getFailureReasonCounts().isEmpty()) {
            PdfPCell reasonCell = createValueCell("No failures recorded");
            reasonCell.setColspan(2);
            breakdownTable.addCell(reasonCell);
        } else {
            for (Map.Entry<String, Long> entry : stats.getFailureReasonCounts().entrySet()) {
                breakdownTable.addCell(createValueCell(entry.getKey()));
                breakdownTable.addCell(createValueCell(INTEGER_FORMAT.format(entry.getValue())));
            }
        }

        document.add(breakdownTable);
        addSpacing(document);

        Paragraph pieHeader = new Paragraph("Failure Reasons Distribution", SUBTITLE_FONT);
        document.add(pieHeader);
        addSpacing(document);

        JFreeChart chart = ChartCreator.createFailureReasonPieChart(stats);
        addChart(document, chart, 480, 320);
        addSpacing(document);
    }

    private void addSummaryChart(Document document, QuoteStatistics statistics) throws DocumentException, IOException {
        Paragraph header = new Paragraph("KPI Summary", SUBTITLE_FONT);
        document.add(header);
        addSpacing(document);

        JFreeChart chart = ChartCreator.createKpiSummaryChart(statistics);
        addChart(document, chart, 520, 320);
    }

    private void addFailureByYearChart(Document document, QuoteStatistics statistics) throws DocumentException, IOException {
        Paragraph header = new Paragraph("Failures by Manufacture Year", SECTION_FONT);
        document.add(header);
        addSpacing(document);

        JFreeChart chart = ChartCreator.createFailureByYearBarChart(statistics.getTplStats(), statistics.getComprehensiveStats());
        addChart(document, chart, 520, 320);
    }

    private void addRecommendations(Document document, QuoteStatistics statistics) throws DocumentException {
        Paragraph header = new Paragraph("Recommendations", SECTION_FONT);
        document.add(header);

        List<Map.Entry<String, Long>> topReasons = statistics.getTopFailureReasons(3);
        if (topReasons.isEmpty()) {
            document.add(new Paragraph("No failures detected. Continue monitoring the process.", NORMAL_FONT));
            return;
        }

        com.lowagie.text.List list = new com.lowagie.text.List(com.lowagie.text.List.UNORDERED);
        for (Map.Entry<String, Long> entry : topReasons) {
            String text = String.format("%s (%,d occurrences) - Investigate root causes and prioritize corrective actions.",
                    entry.getKey(), entry.getValue());
            list.add(new com.lowagie.text.ListItem(new Phrase(text, NORMAL_FONT)));
        }
        document.add(list);
    }

    private PdfPCell createHeaderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SUBTITLE_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBackgroundColor(HEADER_BACKGROUND);
        cell.setPadding(6f);
        return cell;
    }

    private PdfPCell createValueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(6f);
        return cell;
    }

    private void addChart(Document document, JFreeChart chart, int width, int height) throws DocumentException, IOException {
        ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(imageBytes, chart, width, height);
        com.lowagie.text.Image image = com.lowagie.text.Image.getInstance(imageBytes.toByteArray());
        image.setAlignment(Element.ALIGN_CENTER);
        document.add(image);
    }

    private void addSpacing(Document document) throws DocumentException {
        document.add(Chunk.NEWLINE);
    }
}
