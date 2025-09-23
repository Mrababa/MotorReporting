package com.example.motorreporting;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Centralized factory for creating report charts.
 */
public final class ChartCreator {

    static {
        ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());
    }

    private ChartCreator() {
    }

    public static JFreeChart createFailureReasonPieChart(QuoteGroupStats stats) {
        String title = stats.getGroupType().getDisplayName() + " - Failure Reasons";
        return createFailureReasonPieChart(title, stats.getFailureReasonCounts());
    }

    public static JFreeChart createFailureReasonPieChart(String title, Map<String, Long> failureReasons) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        if (failureReasons == null || failureReasons.isEmpty()) {
            dataset.setValue("No Failures", 1);
        } else {
            failureReasons.forEach(dataset::setValue);
        }

        JFreeChart chart = ChartFactory.createPieChart(
                title,
                dataset,
                true,
                true,
                false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 14));
        return chart;
    }

    public static JFreeChart createFailureByYearBarChart(QuoteGroupStats tplStats, QuoteGroupStats compStats) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<String, Long> tplFailures = tplStats.getFailuresByManufactureYear();
        Map<String, Long> compFailures = compStats.getFailuresByManufactureYear();
        Set<String> categories = new LinkedHashSet<>();
        categories.addAll(tplFailures.keySet());
        categories.addAll(compFailures.keySet());

        if (categories.isEmpty()) {
            categories.add("No Data");
        }

        for (String category : categories) {
            dataset.addValue(tplFailures.getOrDefault(category, 0L), tplStats.getGroupType().getShortLabel(), category);
            dataset.addValue(compFailures.getOrDefault(category, 0L), compStats.getGroupType().getShortLabel(), category);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Failures by Manufacture Year",
                "Manufacture Year",
                "Failed Quotes",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 14));
        return chart;
    }

    public static JFreeChart createFailureByYearBarChart(QuoteGroupStats stats) {
        String title = stats.getGroupType().getDisplayName() + " - Failures by Manufacture Year";
        return createFailureByYearBarChart(title, stats.getGroupType().getShortLabel(),
                stats.getFailuresByManufactureYear());
    }

    public static JFreeChart createFailureByYearBarChart(String title, String seriesLabel,
                                                         Map<String, Long> failuresByYear) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        if (failuresByYear == null || failuresByYear.isEmpty()) {
            dataset.addValue(0, seriesLabel, "No Data");
        } else {
            failuresByYear.forEach((year, count) ->
                    dataset.addValue(count, seriesLabel, year));
        }

        JFreeChart chart = ChartFactory.createBarChart(
                title,
                "Manufacture Year",
                "Failed Quotes",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 14));
        return chart;
    }

    public static JFreeChart createKpiSummaryChart(QuoteStatistics statistics) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        QuoteGroupStats tpl = statistics.getTplStats();
        QuoteGroupStats comp = statistics.getComprehensiveStats();

        addSummaryValues(dataset, tpl, "Total Quotes", tpl.getTotalQuotes());
        addSummaryValues(dataset, comp, "Total Quotes", comp.getTotalQuotes());

        addSummaryValues(dataset, tpl, "Failure %", tpl.getFailurePercentage());
        addSummaryValues(dataset, comp, "Failure %", comp.getFailurePercentage());

        addSummaryValues(dataset, tpl, "Blocked Value", tpl.getTotalBlockedEstimatedValue().doubleValue());
        addSummaryValues(dataset, comp, "Blocked Value", comp.getTotalBlockedEstimatedValue().doubleValue());

        JFreeChart chart = ChartFactory.createBarChart(
                "KPI Summary",
                "Metric",
                "Value",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 14));
        chart.addSubtitle(new TextTitle("Includes totals, failure percentages, and blocked values"));
        return chart;
    }

    private static void addSummaryValues(DefaultCategoryDataset dataset, QuoteGroupStats stats,
                                         String category, double value) {
        dataset.addValue(value, stats.getGroupType().getShortLabel(), category);
    }
}
