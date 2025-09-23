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
    private final Map<String, OutcomeBreakdown> comprehensiveBodyCategoryOutcomes;
    private final Map<String, OutcomeBreakdown> comprehensiveSpecificationOutcomes;
    private final List<AgeRangeStats> tplAgeRangeStats;
    private final List<AgeRangeStats> comprehensiveAgeRangeStats;
    private final List<ManufactureYearStats> tplManufactureYearStats;
    private final List<ManufactureYearStats> comprehensiveManufactureYearStats;
    private final List<ValueRangeStats> comprehensiveEstimatedValueStats;
    private final List<ModelChassisSummary> tplTopRejectedModelsByUniqueChassis;
    private final List<MakeModelChassisSummary> topRequestedMakeModelsByUniqueChassis;
    private final Map<String, Long> tplErrorCounts;
    private final Map<String, Long> comprehensiveErrorCounts;
    private final List<CategoryCount> uniqueChassisByInsurancePurpose;
    private final List<CategoryCount> uniqueChassisByBodyType;
    private final List<CategoryCount> manufactureYearTrend;
    private final List<CategoryCount> customerAgeTrend;

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
                           Map<String, OutcomeBreakdown> comprehensiveBodyCategoryOutcomes,
                           Map<String, OutcomeBreakdown> comprehensiveSpecificationOutcomes,
                           List<AgeRangeStats> tplAgeRangeStats,
                           List<AgeRangeStats> comprehensiveAgeRangeStats,
                           List<ManufactureYearStats> tplManufactureYearStats,
                           List<ManufactureYearStats> comprehensiveManufactureYearStats,
                           List<ValueRangeStats> comprehensiveEstimatedValueStats,
                           List<ModelChassisSummary> tplTopRejectedModelsByUniqueChassis,
                           List<MakeModelChassisSummary> topRequestedMakeModelsByUniqueChassis,
                           Map<String, Long> tplErrorCounts,
                           Map<String, Long> comprehensiveErrorCounts,
                           List<CategoryCount> uniqueChassisByInsurancePurpose,
                           List<CategoryCount> uniqueChassisByBodyType,
                           List<CategoryCount> manufactureYearTrend,
                           List<CategoryCount> customerAgeTrend) {
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
        this.comprehensiveBodyCategoryOutcomes = Collections.unmodifiableMap(new LinkedHashMap<>(comprehensiveBodyCategoryOutcomes));
        this.comprehensiveSpecificationOutcomes = Collections.unmodifiableMap(new LinkedHashMap<>(comprehensiveSpecificationOutcomes));
        this.tplAgeRangeStats = List.copyOf(tplAgeRangeStats);
        this.comprehensiveAgeRangeStats = List.copyOf(comprehensiveAgeRangeStats);
        this.tplManufactureYearStats = List.copyOf(tplManufactureYearStats);
        this.comprehensiveManufactureYearStats = List.copyOf(comprehensiveManufactureYearStats);
        this.comprehensiveEstimatedValueStats = List.copyOf(comprehensiveEstimatedValueStats);
        this.tplTopRejectedModelsByUniqueChassis = List.copyOf(tplTopRejectedModelsByUniqueChassis);
        this.topRequestedMakeModelsByUniqueChassis = List.copyOf(topRequestedMakeModelsByUniqueChassis);
        this.tplErrorCounts = Collections.unmodifiableMap(new LinkedHashMap<>(tplErrorCounts));
        this.comprehensiveErrorCounts = Collections.unmodifiableMap(new LinkedHashMap<>(comprehensiveErrorCounts));
        this.uniqueChassisByInsurancePurpose = List.copyOf(uniqueChassisByInsurancePurpose);
        this.uniqueChassisByBodyType = List.copyOf(uniqueChassisByBodyType);
        this.manufactureYearTrend = List.copyOf(manufactureYearTrend);
        this.customerAgeTrend = List.copyOf(customerAgeTrend);
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

    public List<ModelChassisSummary> getTplTopRejectedModelsByUniqueChassis() {
        return tplTopRejectedModelsByUniqueChassis;
    }

    public List<MakeModelChassisSummary> getTopRequestedMakeModelsByUniqueChassis() {
        return topRequestedMakeModelsByUniqueChassis;
    }

    public Map<String, Long> getTplErrorCounts() {
        return tplErrorCounts;
    }

    public Map<String, OutcomeBreakdown> getComprehensiveBodyCategoryOutcomes() {
        return comprehensiveBodyCategoryOutcomes;
    }

    public Map<String, OutcomeBreakdown> getComprehensiveSpecificationOutcomes() {
        return comprehensiveSpecificationOutcomes;
    }

    public List<AgeRangeStats> getComprehensiveAgeRangeStats() {
        return comprehensiveAgeRangeStats;
    }

    public List<ValueRangeStats> getComprehensiveEstimatedValueStats() {
        return comprehensiveEstimatedValueStats;
    }

    public List<ManufactureYearStats> getTplManufactureYearStats() {
        return tplManufactureYearStats;
    }

    public List<ManufactureYearStats> getComprehensiveManufactureYearStats() {
        return comprehensiveManufactureYearStats;
    }

    public Map<String, Long> getComprehensiveErrorCounts() {
        return comprehensiveErrorCounts;
    }

    public List<CategoryCount> getUniqueChassisByInsurancePurpose() {
        return uniqueChassisByInsurancePurpose;
    }

    public List<CategoryCount> getUniqueChassisByBodyType() {
        return uniqueChassisByBodyType;
    }

    public List<CategoryCount> getManufactureYearTrend() {
        return manufactureYearTrend;
    }

    public List<CategoryCount> getCustomerAgeTrend() {
        return customerAgeTrend;
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

    public static final class ModelChassisSummary {
        private final String model;
        private final long uniqueChassisCount;

        public ModelChassisSummary(String model, long uniqueChassisCount) {
            this.model = Objects.requireNonNull(model, "model");
            this.uniqueChassisCount = uniqueChassisCount;
        }

        public String getModel() {
            return model;
        }

        public long getUniqueChassisCount() {
            return uniqueChassisCount;
        }
    }

    public static final class MakeModelChassisSummary {
        private final String make;
        private final String model;
        private final long uniqueChassisCount;
        private final long successfulUniqueChassisCount;
        private final long failedUniqueChassisCount;

        public MakeModelChassisSummary(String make,
                                       String model,
                                       long uniqueChassisCount,
                                       long successfulUniqueChassisCount,
                                       long failedUniqueChassisCount) {
            this.make = Objects.requireNonNull(make, "make");
            this.model = Objects.requireNonNull(model, "model");
            this.uniqueChassisCount = uniqueChassisCount;
            this.successfulUniqueChassisCount = successfulUniqueChassisCount;
            this.failedUniqueChassisCount = failedUniqueChassisCount;
        }

        public String getMake() {
            return make;
        }

        public String getModel() {
            return model;
        }

        public long getUniqueChassisCount() {
            return uniqueChassisCount;
        }

        public long getSuccessfulUniqueChassisCount() {
            return successfulUniqueChassisCount;
        }

        public long getFailedUniqueChassisCount() {
            return failedUniqueChassisCount;
        }
    }

    public static final class CategoryCount {
        private final String label;
        private final long count;

        public CategoryCount(String label, long count) {
            this.label = Objects.requireNonNull(label, "label");
            this.count = count;
        }

        public String getLabel() {
            return label;
        }

        public long getCount() {
            return count;
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

    public static final class ValueRangeStats {
        private final String label;
        private final long successCount;
        private final long failureCount;

        public ValueRangeStats(String label, long successCount, long failureCount) {
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

    public static final class ManufactureYearStats {
        private final String label;
        private final long successCount;
        private final long failureCount;

        public ManufactureYearStats(String label, long successCount, long failureCount) {
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
