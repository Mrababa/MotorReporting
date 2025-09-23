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
        return new QuoteStatistics(tplStats, compStats);
    }

    private static QuoteGroupStats buildStats(GroupType groupType, List<QuoteRecord> records) {
        if (records.isEmpty()) {
            return QuoteGroupStats.empty(groupType);
        }

        long total = records.size();
        long passCount = records.stream().filter(QuoteRecord::isSuccessful).count();
        long failCount = total - passCount;
        double failurePercentage = total == 0 ? 0.0 : (failCount * 100.0) / total;

        Map<String, Long> failureReasons = records.stream()
                .filter(record -> !record.isSuccessful())
                .collect(Collectors.groupingBy(QuoteRecord::getFailureReason, Collectors.counting()));

        Map<String, Long> sortedFailureReasons = failureReasons.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        Map<String, Long> failuresByYear = records.stream()
                .filter(record -> !record.isSuccessful())
                .collect(Collectors.groupingBy(record ->
                        record.getManufactureYear().map(String::valueOf).orElse("Unknown"),
                        Collectors.counting()));

        Map<String, Long> sortedFailuresByYear = failuresByYear.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> yearOrderKey(entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        BigDecimal totalBlocked = records.stream()
                .filter(record -> !record.isSuccessful())
                .map(QuoteRecord::getEstimatedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new QuoteGroupStats(groupType, total, passCount, failCount, failurePercentage,
                sortedFailureReasons, sortedFailuresByYear, totalBlocked);
    }

    private static int yearOrderKey(String label) {
        try {
            return Integer.parseInt(label);
        } catch (NumberFormatException ex) {
            return Integer.MAX_VALUE;
        }
    }
}
