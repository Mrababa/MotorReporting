package com.example.motorreporting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class to transform raw records into aggregated statistics.
 */
public final class QuoteStatisticsCalculator {

    private QuoteStatisticsCalculator() {
    }

    private static final int TOP_REJECTED_MODEL_LIMIT = 10;

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
        UniqueChassisSummary tplUniqueChassisSummary = computeUniqueChassisSummary(tplRecords);
        UniqueChassisSummary compUniqueChassisSummary = computeUniqueChassisSummary(compRecords);
        Map<String, QuoteStatistics.OutcomeBreakdown> tplBodyCategoryOutcomes =
                computeOutcomeBreakdown(tplRecords, QuoteRecord::getBodyCategoryLabel);
        Map<String, QuoteStatistics.OutcomeBreakdown> tplSpecificationOutcomes =
                computeOutcomeBreakdown(tplRecords, QuoteRecord::getOverrideSpecificationLabel);
        Map<String, QuoteStatistics.OutcomeBreakdown> compBodyCategoryOutcomes =
                computeOutcomeBreakdown(compRecords, QuoteRecord::getBodyCategoryLabel);
        Map<String, QuoteStatistics.OutcomeBreakdown> compSpecificationOutcomes =
                computeOutcomeBreakdown(compRecords, QuoteRecord::getOverrideSpecificationLabel);
        List<QuoteStatistics.AgeRangeStats> tplAgeRangeStats = computeAgeRangeStats(tplRecords);
        List<QuoteStatistics.AgeRangeStats> compAgeRangeStats = computeAgeRangeStats(compRecords);
        List<QuoteStatistics.ValueRangeStats> compEstimatedValueStats =
                computeEstimatedValueRangeStats(compRecords);
        Map<String, Long> tplErrorCounts = computeErrorCounts(tplRecords, false);
        Map<String, Long> compErrorCounts = computeErrorCounts(compRecords, true);
        List<QuoteStatistics.ModelChassisSummary> tplTopRejectedModels =
                computeTopRejectedModelsByUniqueChassis(tplRecords, TOP_REJECTED_MODEL_LIMIT);
        return new QuoteStatistics(tplStats, compStats,
                uniqueChassisSummary.getTotal(),
                uniqueChassisSummary.getSuccessCount(),
                uniqueChassisSummary.getFailureCount(),
                tplUniqueChassisSummary.getSuccessCount(),
                tplUniqueChassisSummary.getFailureCount(),
                compUniqueChassisSummary.getSuccessCount(),
                compUniqueChassisSummary.getFailureCount(),
                tplBodyCategoryOutcomes,
                tplSpecificationOutcomes,
                compBodyCategoryOutcomes,
                compSpecificationOutcomes,
                tplAgeRangeStats,
                compAgeRangeStats,
                compEstimatedValueStats,
                tplTopRejectedModels,
                tplErrorCounts,
                compErrorCounts);
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

    private static Map<String, QuoteStatistics.OutcomeBreakdown> computeOutcomeBreakdown(
            List<QuoteRecord> records,
            Function<QuoteRecord, String> classifier) {
        Map<String, OutcomeAccumulator> accumulatorByKey = new HashMap<>();
        for (QuoteRecord record : records) {
            if (!record.isSuccessful() && !record.isFailure()) {
                continue;
            }
            String label = classifier.apply(record);
            if (label == null || label.isBlank()) {
                label = "Unknown";
            }
            OutcomeAccumulator accumulator = accumulatorByKey.computeIfAbsent(label, key -> new OutcomeAccumulator());
            if (record.isSuccessful()) {
                accumulator.incrementSuccess();
            } else {
                accumulator.incrementFailure();
            }
        }

        return accumulatorByKey.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, OutcomeAccumulator>>comparingLong(
                                entry -> entry.getValue().total())
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().toOutcomeBreakdown(),
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    private static List<QuoteStatistics.AgeRangeStats> computeAgeRangeStats(List<QuoteRecord> records) {
        Map<AgeRange, OutcomeAccumulator> accumulatorByRange = new LinkedHashMap<>();
        for (AgeRange range : AGE_RANGES) {
            accumulatorByRange.put(range, new OutcomeAccumulator());
        }
        OutcomeAccumulator otherAccumulator = new OutcomeAccumulator();

        for (QuoteRecord record : records) {
            if (!record.isSuccessful() && !record.isFailure()) {
                continue;
            }
            OutcomeAccumulator accumulator = otherAccumulator;
            if (record.getDriverAge().isPresent()) {
                int age = record.getDriverAge().get();
                AgeRange matchingRange = findAgeRange(age);
                if (matchingRange != null) {
                    accumulator = accumulatorByRange.get(matchingRange);
                }
            }

            if (record.isSuccessful()) {
                accumulator.incrementSuccess();
            } else {
                accumulator.incrementFailure();
            }
        }

        List<QuoteStatistics.AgeRangeStats> results = new ArrayList<>();
        for (AgeRange range : AGE_RANGES) {
            OutcomeAccumulator accumulator = accumulatorByRange.get(range);
            results.add(accumulator.toAgeRangeStats(range.getLabel()));
        }
        if (otherAccumulator.total() > 0) {
            results.add(otherAccumulator.toAgeRangeStats(OTHER_AGE_LABEL));
        }
        return results;
    }

    private static Map<String, Long> computeErrorCounts(List<QuoteRecord> records, boolean excludeNullLabel) {
        Map<String, Long> counts = records.stream()
                .filter(QuoteRecord::isFailure)
                .map(QuoteRecord::getFailureErrorText)
                .flatMap(Optional::stream)
                .filter(label -> !excludeNullLabel || !"null".equalsIgnoreCase(label))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private static List<QuoteStatistics.ModelChassisSummary> computeTopRejectedModelsByUniqueChassis(
            List<QuoteRecord> records,
            int limit) {
        if (limit <= 0) {
            return List.of();
        }
        Map<String, Set<String>> chassisByModel = new HashMap<>();
        for (QuoteRecord record : records) {
            if (!record.isFailure()) {
                continue;
            }
            Optional<String> modelOptional = record.getModel();
            Optional<String> chassisOptional = record.getChassisNumber();
            if (modelOptional.isEmpty() || chassisOptional.isEmpty()) {
                continue;
            }
            String model = modelOptional.get();
            String chassisNumber = chassisOptional.get();
            if (model.isBlank() || chassisNumber.isBlank()) {
                continue;
            }
            Set<String> uniqueChassis = chassisByModel.computeIfAbsent(model, key -> new LinkedHashSet<>());
            uniqueChassis.add(chassisNumber);
        }

        return chassisByModel.entrySet().stream()
                .map(entry -> new QuoteStatistics.ModelChassisSummary(entry.getKey(), entry.getValue().size()))
                .sorted(Comparator.comparingLong(QuoteStatistics.ModelChassisSummary::getUniqueChassisCount).reversed()
                        .thenComparing(QuoteStatistics.ModelChassisSummary::getModel))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private static List<QuoteStatistics.ValueRangeStats> computeEstimatedValueRangeStats(List<QuoteRecord> records) {
        Map<ValueRange, OutcomeAccumulator> accumulatorByRange = new LinkedHashMap<>();
        for (ValueRange range : VALUE_RANGES) {
            accumulatorByRange.put(range, new OutcomeAccumulator());
        }
        OutcomeAccumulator otherAccumulator = new OutcomeAccumulator();

        for (QuoteRecord record : records) {
            if (!record.isSuccessful() && !record.isFailure()) {
                continue;
            }
            OutcomeAccumulator accumulator = otherAccumulator;
            BigDecimal estimatedValue = record.getEstimatedValue();
            if (estimatedValue != null && estimatedValue.compareTo(BigDecimal.ZERO) > 0) {
                long numericValue = estimatedValue.longValue();
                ValueRange matchingRange = findValueRange(numericValue);
                if (matchingRange != null) {
                    accumulator = accumulatorByRange.get(matchingRange);
                }
            }

            if (record.isSuccessful()) {
                accumulator.incrementSuccess();
            } else {
                accumulator.incrementFailure();
            }
        }

        List<QuoteStatistics.ValueRangeStats> results = new ArrayList<>();
        for (ValueRange range : VALUE_RANGES) {
            OutcomeAccumulator accumulator = accumulatorByRange.get(range);
            results.add(accumulator.toValueRangeStats(range.getLabel()));
        }
        if (otherAccumulator.total() > 0) {
            results.add(otherAccumulator.toValueRangeStats(OTHER_VALUE_LABEL));
        }
        return results;
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

    private static final class OutcomeAccumulator {
        private long success;
        private long failure;

        private void incrementSuccess() {
            success++;
        }

        private void incrementFailure() {
            failure++;
        }

        private long total() {
            return success + failure;
        }

        private QuoteStatistics.OutcomeBreakdown toOutcomeBreakdown() {
            return new QuoteStatistics.OutcomeBreakdown(success, failure);
        }

        private QuoteStatistics.AgeRangeStats toAgeRangeStats(String label) {
            return new QuoteStatistics.AgeRangeStats(label, success, failure);
        }

        private QuoteStatistics.ValueRangeStats toValueRangeStats(String label) {
            return new QuoteStatistics.ValueRangeStats(label, success, failure);
        }
    }

    private static AgeRange findAgeRange(int age) {
        for (AgeRange range : AGE_RANGES) {
            if (range.contains(age)) {
                return range;
            }
        }
        return null;
    }

    private static final String OTHER_AGE_LABEL = "Other / Unknown";
    private static final String OTHER_VALUE_LABEL = "Other / Unknown";

    private static final List<AgeRange> AGE_RANGES = List.of(
            new AgeRange(18, 24),
            new AgeRange(25, 29),
            new AgeRange(30, 34),
            new AgeRange(35, 39),
            new AgeRange(40, 44),
            new AgeRange(45, 49),
            new AgeRange(50, 54),
            new AgeRange(55, 59),
            new AgeRange(60, 64),
            new AgeRange(65, 70)
    );

    private static final List<ValueRange> VALUE_RANGES = List.of(
            new ValueRange(5_000, 49_999),
            new ValueRange(50_000, 99_999),
            new ValueRange(100_000, 149_999),
            new ValueRange(150_000, 199_999),
            new ValueRange(200_000, 249_999),
            new ValueRange(250_000, 299_999),
            new ValueRange(300_000, 349_999),
            new ValueRange(350_000, 399_999),
            new ValueRange(400_000, 449_999),
            new ValueRange(450_000, 500_000)
    );

    private static final class AgeRange {
        private final int startInclusive;
        private final int endInclusive;
        private final String label;

        private AgeRange(int startInclusive, int endInclusive) {
            this.startInclusive = startInclusive;
            this.endInclusive = endInclusive;
            this.label = startInclusive + "–" + endInclusive;
        }

        private boolean contains(int value) {
            return value >= startInclusive && value <= endInclusive;
        }

        private String getLabel() {
            return label;
        }
    }

    private static ValueRange findValueRange(long value) {
        for (ValueRange range : VALUE_RANGES) {
            if (range.contains(value)) {
                return range;
            }
        }
        return null;
    }

    private static final class ValueRange {
        private final long startInclusive;
        private final long endInclusive;
        private final String label;

        private ValueRange(long startInclusive, long endInclusive) {
            this.startInclusive = startInclusive;
            this.endInclusive = endInclusive;
            this.label = formatLabel(startInclusive, endInclusive);
        }

        private boolean contains(long value) {
            return value >= startInclusive && value <= endInclusive;
        }

        private String getLabel() {
            return label;
        }

        private static String formatLabel(long startInclusive, long endInclusive) {
            return String.format(Locale.US, "%,d–%,d", startInclusive, endInclusive);
        }
    }
}
