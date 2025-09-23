package com.example.motorreporting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Holds statistics for both Third Party and Comprehensive groups and exposes overall metrics.
 */
public class QuoteStatistics {

    private final QuoteGroupStats tplStats;
    private final QuoteGroupStats comprehensiveStats;

    public QuoteStatistics(QuoteGroupStats tplStats, QuoteGroupStats comprehensiveStats) {
        this.tplStats = Objects.requireNonNull(tplStats, "tplStats");
        this.comprehensiveStats = Objects.requireNonNull(comprehensiveStats, "comprehensiveStats");
    }

    public QuoteGroupStats getTplStats() {
        return tplStats;
    }

    public QuoteGroupStats getComprehensiveStats() {
        return comprehensiveStats;
    }

    public long getOverallTotalQuotes() {
        return tplStats.getTotalQuotes() + comprehensiveStats.getTotalQuotes();
    }

    public long getOverallPassCount() {
        return tplStats.getPassCount() + comprehensiveStats.getPassCount();
    }

    public long getOverallFailCount() {
        return tplStats.getFailCount() + comprehensiveStats.getFailCount();
    }

    public long getOverallSkipCount() {
        return tplStats.getSkipCount() + comprehensiveStats.getSkipCount();
    }

    public long getOverallProcessedQuotes() {
        return getOverallPassCount() + getOverallFailCount();
    }

    public double getOverallFailurePercentage() {
        long processed = getOverallProcessedQuotes();
        if (processed == 0) {
            return 0.0;
        }
        return (getOverallFailCount() * 100.0) / processed;
    }

    public BigDecimal getOverallBlockedEstimatedValue() {
        return tplStats.getTotalBlockedEstimatedValue()
                .add(comprehensiveStats.getTotalBlockedEstimatedValue())
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Map<String, Long> getCombinedFailureReasons() {
        Map<String, Long> combined = new LinkedHashMap<>();
        tplStats.getFailureReasonCounts().forEach((reason, count) ->
                combined.merge(reason, count, Long::sum));
        comprehensiveStats.getFailureReasonCounts().forEach((reason, count) ->
                combined.merge(reason, count, Long::sum));

        return combined.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    public List<Map.Entry<String, Long>> getTopFailureReasons(int limit) {
        return getCombinedFailureReasons().entrySet().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getCombinedFailuresByManufactureYear() {
        Map<String, Long> combined = new LinkedHashMap<>();
        tplStats.getFailuresByManufactureYear().forEach((year, count) ->
                combined.merge(year, count, Long::sum));
        comprehensiveStats.getFailuresByManufactureYear().forEach((year, count) ->
                combined.merge(year, count, Long::sum));

        return combined.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> yearOrderKey(entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private static int yearOrderKey(String label) {
        try {
            return Integer.parseInt(label);
        } catch (NumberFormatException ex) {
            return Integer.MAX_VALUE;
        }
    }
}
