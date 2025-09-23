package com.example.motorreporting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class to transform raw records into aggregated statistics.
 */
public final class QuoteStatisticsCalculator {

    private QuoteStatisticsCalculator() {
    }

    public static QuoteStatistics calculate(List<QuoteRecord> records) {
        Objects.requireNonNull(records, "records");

        List<QuoteRecord> tplRecords = records.stream()
                .filter(record -> record.belongsTo(GroupType.TPL))
                .collect(Collectors.toList());
        List<QuoteRecord> compRecords = records.stream()
                .filter(record -> record.belongsTo(GroupType.COMPREHENSIVE))
                .collect(Collectors.toList());

        QuoteGroupStats tplStats = buildStats(GroupType.TPL, tplRecords);
        QuoteGroupStats compStats = buildStats(GroupType.COMPREHENSIVE, compRecords);
        UniqueChassisSummary uniqueChassisSummary = computeUniqueChassisSummary(records);
        return new QuoteStatistics(tplStats, compStats,
                uniqueChassisSummary.getTotal(),
                uniqueChassisSummary.getSuccessCount(),
                uniqueChassisSummary.getFailureCount());
    }

    private static QuoteGroupStats buildStats(GroupType groupType, List<QuoteRecord> records) {
        if (records.isEmpty()) {
            return QuoteGroupStats.empty(groupType);
        }

        long passCount = records.stream().filter(QuoteRecord::isSuccessful).count();
        long failCount = records.stream().filter(QuoteRecord::isFailure).count();
        long skipCount = records.stream().filter(QuoteRecord::isSkipped).count();
        long total = passCount + failCount + skipCount;
        long processedCount = passCount + failCount;
        double failurePercentage = processedCount == 0 ? 0.0 : (failCount * 100.0) / processedCount;

        Map<String, Long> failureReasons = records.stream()
                .filter(QuoteRecord::isFailure)
                .collect(Collectors.groupingBy(QuoteRecord::getFailureReason, Collectors.counting()));

        Map<String, Long> sortedFailureReasons = failureReasons.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        Map<String, Long> failuresByYear = records.stream()
                .filter(QuoteRecord::isFailure)
                .collect(Collectors.groupingBy(record ->
                        record.getManufactureYear().map(String::valueOf).orElse("Unknown"),
                        Collectors.counting()));

        Map<String, Long> sortedFailuresByYear = failuresByYear.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> yearOrderKey(entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        BigDecimal totalBlocked = records.stream()
                .filter(QuoteRecord::isFailure)
                .map(QuoteRecord::getEstimatedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new QuoteGroupStats(groupType, total, passCount, failCount, skipCount, failurePercentage,
                sortedFailureReasons, sortedFailuresByYear, totalBlocked);
    }

    private static int yearOrderKey(String label) {
        try {
            return Integer.parseInt(label);
        } catch (NumberFormatException ex) {
            return Integer.MAX_VALUE;
        }
    }

    private static UniqueChassisSummary computeUniqueChassisSummary(List<QuoteRecord> records) {
        Set<String> uniqueValues = new LinkedHashSet<>();
        Set<String> successValues = new LinkedHashSet<>();
        Set<String> failureValues = new LinkedHashSet<>();

        for (QuoteRecord record : records) {
            record.getChassisNumber().ifPresent(chassis -> {
                uniqueValues.add(chassis);
                if (record.isSuccessful()) {
                    successValues.add(chassis);
                } else if (record.isFailure()) {
                    failureValues.add(chassis);
                }
            });
        }

        return new UniqueChassisSummary(uniqueValues.size(), successValues.size(), failureValues.size());
    }

    private static final class UniqueChassisSummary {
        private final long total;
        private final long successCount;
        private final long failureCount;

        private UniqueChassisSummary(long total, long successCount, long failureCount) {
            this.total = total;
            this.successCount = successCount;
            this.failureCount = failureCount;
        }

        private long getTotal() {
            return total;
        }

        private long getSuccessCount() {
            return successCount;
        }

        private long getFailureCount() {
            return failureCount;
        }
    }
}
