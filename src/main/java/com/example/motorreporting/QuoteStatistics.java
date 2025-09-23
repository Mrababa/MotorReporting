package com.example.motorreporting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
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
    private final long uniqueChassisCount;
    private final long uniqueChassisSuccessCount;
    private final long uniqueChassisFailCount;
    private final long tplUniqueChassisSuccessCount;
    private final long tplUniqueChassisFailCount;
    private final long comprehensiveUniqueChassisSuccessCount;
    private final long comprehensiveUniqueChassisFailCount;
    private final Map<String, OutcomeBreakdown> tplBodyCategoryOutcomes;
    private final Map<String, OutcomeBreakdown> tplSpecificationOutcomes;
    private final List<AgeRangeStats> tplAgeRangeStats;
    private final Map<String, Long> tplErrorCounts;

    public QuoteStatistics(QuoteGroupStats tplStats,
                           QuoteGroupStats comprehensiveStats,
                           long uniqueChassisCount,
                           long uniqueChassisSuccessCount,
                           long uniqueChassisFailCount,
                           long tplUniqueChassisSuccessCount,
                           long tplUniqueChassisFailCount,
                           long comprehensiveUniqueChassisSuccessCount,
                           long comprehensiveUniqueChassisFailCount,
                           Map<String, OutcomeBreakdown> tplBodyCategoryOutcomes,
                           Map<String, OutcomeBreakdown> tplSpecificationOutcomes,
                           List<AgeRangeStats> tplAgeRangeStats,
                           Map<String, Long> tplErrorCounts) {
        this.tplStats = Objects.requireNonNull(tplStats, "tplStats");
        this.comprehensiveStats = Objects.requireNonNull(comprehensiveStats, "comprehensiveStats");
        this.uniqueChassisCount = uniqueChassisCount;
        this.uniqueChassisSuccessCount = uniqueChassisSuccessCount;
        this.uniqueChassisFailCount = uniqueChassisFailCount;
        this.tplUniqueChassisSuccessCount = tplUniqueChassisSuccessCount;
        this.tplUniqueChassisFailCount = tplUniqueChassisFailCount;
        this.comprehensiveUniqueChassisSuccessCount = comprehensiveUniqueChassisSuccessCount;
        this.comprehensiveUniqueChassisFailCount = comprehensiveUniqueChassisFailCount;
        this.tplBodyCategoryOutcomes = Collections.unmodifiableMap(new LinkedHashMap<>(tplBodyCategoryOutcomes));
        this.tplSpecificationOutcomes = Collections.unmodifiableMap(new LinkedHashMap<>(tplSpecificationOutcomes));
        this.tplAgeRangeStats = List.copyOf(tplAgeRangeStats);
        this.tplErrorCounts = Collections.unmodifiableMap(new LinkedHashMap<>(tplErrorCounts));
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

    public long getUniqueChassisCount() {
        return uniqueChassisCount;
    }

    public long getUniqueChassisSuccessCount() {
        return uniqueChassisSuccessCount;
    }

    public long getUniqueChassisFailCount() {
        return uniqueChassisFailCount;
    }

    public long getTplUniqueChassisSuccessCount() {
        return tplUniqueChassisSuccessCount;
    }

    public long getTplUniqueChassisFailCount() {
        return tplUniqueChassisFailCount;
    }

    public long getComprehensiveUniqueChassisSuccessCount() {
        return comprehensiveUniqueChassisSuccessCount;
    }

    public long getComprehensiveUniqueChassisFailCount() {
        return comprehensiveUniqueChassisFailCount;
    }

    public Map<String, OutcomeBreakdown> getTplBodyCategoryOutcomes() {
        return tplBodyCategoryOutcomes;
    }

    public Map<String, OutcomeBreakdown> getTplSpecificationOutcomes() {
        return tplSpecificationOutcomes;
    }

    public List<AgeRangeStats> getTplAgeRangeStats() {
        return tplAgeRangeStats;
    }

    public Map<String, Long> getTplErrorCounts() {
        return tplErrorCounts;
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

    public static final class OutcomeBreakdown {
        private final long successCount;
        private final long failureCount;

        public OutcomeBreakdown(long successCount, long failureCount) {
            this.successCount = successCount;
            this.failureCount = failureCount;
        }

        public long getSuccessCount() {
            return successCount;
        }

        public long getFailureCount() {
            return failureCount;
        }

        public long getProcessedTotal() {
            return successCount + failureCount;
        }
    }

    public static final class AgeRangeStats {
        private final String label;
        private final long successCount;
        private final long failureCount;

        public AgeRangeStats(String label, long successCount, long failureCount) {
            this.label = Objects.requireNonNull(label, "label");
            this.successCount = successCount;
            this.failureCount = failureCount;
        }

        public String getLabel() {
            return label;
        }

        public long getSuccessCount() {
            return successCount;
        }

        public long getFailureCount() {
            return failureCount;
        }

        public long getProcessedTotal() {
            return successCount + failureCount;
        }

        public double getSuccessRatio() {
            long total = getProcessedTotal();
            if (total == 0) {
                return 0.0;
            }
            return (successCount * 100.0) / total;
        }

        public double getFailureRatio() {
            long total = getProcessedTotal();
            if (total == 0) {
                return 0.0;
            }
            return (failureCount * 100.0) / total;
        }
    }
}
