package com.example.motorreporting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class to transform raw records into aggregated statistics.
 */
public final class QuoteStatisticsCalculator {

    private QuoteStatisticsCalculator() {
    }

    private static final int TOP_REJECTED_MODEL_LIMIT = 10;
    private static final int TOP_REJECTED_MAKE_MODEL_LIMIT = 20;
    private static final int TOP_REQUESTED_MAKE_MODEL_LIMIT = 20;
    private static final String SPEC_LABEL_GCC = "GCC";
    private static final String SPEC_LABEL_NON_GCC = "Non GCC";
    private static final String SPEC_LABEL_UNKNOWN = "Unknown";
    private static final String NO_DATA_LABEL = "No Data";

    private static String normalizeChassisNumber(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private static String normalizeEid(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                builder.append(Character.toUpperCase(ch));
            }
        }
        if (builder.length() == 0) {
            return null;
        }
        return builder.toString();
    }

    private static String normalizeSimpleIdentifier(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
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
        QuoteStatistics.UniqueRequestSummary overallUniqueRequests = computeUniqueRequestSummary(records);
        QuoteStatistics.UniqueRequestSummary tplUniqueRequests = computeUniqueRequestSummary(tplRecords);
        QuoteStatistics.UniqueRequestSummary compUniqueRequests = computeUniqueRequestSummary(compRecords);
        UniqueChassisSummary uniqueChassisSummary = computeUniqueChassisSummary(records);
        UniqueChassisSummary tplUniqueChassisSummary = computeUniqueChassisSummary(tplRecords);
        UniqueChassisSummary compUniqueChassisSummary = computeUniqueChassisSummary(compRecords);
        QuoteStatistics.EidChassisSummary tplEidChassisSummary = computeEidChassisSummary(tplRecords);
        Map<String, QuoteStatistics.OutcomeBreakdown> tplBodyCategoryOutcomes =
                computeOutcomeBreakdown(tplRecords, QuoteRecord::getBodyCategoryLabel);
        Map<String, QuoteStatistics.OutcomeBreakdown> tplSpecificationOutcomes =
                computeOutcomeBreakdown(tplRecords, QuoteRecord::getOverrideSpecificationLabel);
        Map<String, QuoteStatistics.OutcomeBreakdown> tplChineseOutcomes =
                computeOutcomeBreakdown(tplRecords, QuoteRecord::getChineseClassificationLabel);
        Map<String, QuoteStatistics.OutcomeBreakdown> tplElectricOutcomes =
                computeOutcomeBreakdown(tplRecords, QuoteRecord::getElectricClassificationLabel);
        Map<String, QuoteStatistics.OutcomeBreakdown> tplChineseElectricOutcomes =
                computeOutcomeBreakdown(tplRecords, QuoteRecord::getChineseElectricSegmentLabel);
        Map<String, QuoteStatistics.OutcomeBreakdown> compBodyCategoryOutcomes =
                computeOutcomeBreakdown(compRecords, QuoteRecord::getBodyCategoryLabel);
        Map<String, QuoteStatistics.OutcomeBreakdown> compSpecificationOutcomes =
                computeOutcomeBreakdown(compRecords, QuoteRecord::getOverrideSpecificationLabel);
        List<QuoteStatistics.AgeRangeStats> tplAgeRangeStats = computeAgeRangeStats(tplRecords);
        List<QuoteStatistics.AgeRangeStats> compAgeRangeStats = computeAgeRangeStats(compRecords);
        List<QuoteStatistics.ManufactureYearStats> tplManufactureYearStats =
                computeManufactureYearStats(tplRecords);
        List<QuoteStatistics.ManufactureYearStats> compManufactureYearStats =
                computeManufactureYearStats(compRecords);
        List<QuoteStatistics.ValueRangeStats> compEstimatedValueStats =
                computeEstimatedValueRangeStats(compRecords);
        List<QuoteStatistics.SalesConversionStats> tplSalesByBodyType =
                computeSalesConversionByBodyType(tplRecords);
        List<QuoteStatistics.SalesConversionStats> tplSalesByAgeRange =
                computeSalesConversionByAgeRange(tplRecords);
        List<QuoteStatistics.SalesConversionStats> tplSalesByChineseClassification =
                computeSalesConversionByClassifier(tplRecords, QuoteRecord::getChineseClassificationLabel);
        List<QuoteStatistics.SalesConversionStats> tplSalesByFuelType =
                computeSalesConversionByClassifier(tplRecords, QuoteRecord::getElectricClassificationLabel);
        List<QuoteStatistics.SalesConversionStats> compSalesByBodyType =
                computeSalesConversionByBodyType(compRecords);
        List<QuoteStatistics.SalesConversionStats> compSalesByAgeRange =
                computeSalesConversionByAgeRange(compRecords);
        List<QuoteStatistics.SalesConversionStats> compSalesByChineseClassification =
                computeSalesConversionByClassifier(compRecords, QuoteRecord::getChineseClassificationLabel);
        List<QuoteStatistics.SalesConversionStats> compSalesByFuelType =
                computeSalesConversionByClassifier(compRecords, QuoteRecord::getElectricClassificationLabel);
        Map<String, Long> tplErrorCounts = computeErrorCounts(tplRecords, false);
        Map<String, Long> compErrorCounts = computeErrorCounts(compRecords, true);
        List<QuoteStatistics.ModelChassisSummary> tplTopRejectedModels =
                computeTopRejectedModelsByUniqueChassis(tplRecords, TOP_REJECTED_MODEL_LIMIT);
        List<QuoteStatistics.MakeModelChassisSummary> topRequestedMakeModels =
                computeTopMakeModelByUniqueChassis(records, TOP_REQUESTED_MAKE_MODEL_LIMIT);
        List<QuoteStatistics.MakeModelChassisSummary> tplTopRequestedMakeModels =
                computeTopMakeModelByUniqueChassis(tplRecords, TOP_REQUESTED_MAKE_MODEL_LIMIT);
        List<QuoteStatistics.MakeModelChassisSummary> compTopRequestedMakeModels =
                computeTopMakeModelByUniqueChassis(compRecords, TOP_REQUESTED_MAKE_MODEL_LIMIT);
        List<QuoteStatistics.MakeModelChassisSummary> compTopRejectedMakeModels =
                computeTopRejectedMakeModelByUniqueChassis(compRecords, TOP_REJECTED_MAKE_MODEL_LIMIT);
        List<QuoteStatistics.CategoryCount> uniqueChassisBySpecification =
                computeUniqueChassisCountsBySpecification(records);
        List<QuoteStatistics.CategoryCount> uniqueChassisByInsurancePurpose =
                computeUniqueChassisCounts(records, QuoteRecord::getInsurancePurposeLabel);
        List<QuoteStatistics.CategoryCount> uniqueChassisByBodyType =
                computeUniqueChassisCounts(records, QuoteRecord::getBodyCategoryLabel);
        List<QuoteStatistics.TrendPoint> manufactureYearTrend =
                computeManufactureYearTrend(records);
        List<QuoteStatistics.TrendPoint> customerAgeTrend =
                computeCustomerAgeTrend(records);
        return new QuoteStatistics(tplStats, compStats,
                overallUniqueRequests,
                tplUniqueRequests,
                compUniqueRequests,
                uniqueChassisSummary.getTotal(),
                uniqueChassisSummary.getSuccessCount(),
                uniqueChassisSummary.getFailureCount(),
                tplUniqueChassisSummary.getSuccessCount(),
                tplUniqueChassisSummary.getFailureCount(),
                tplEidChassisSummary,
                compUniqueChassisSummary.getSuccessCount(),
                compUniqueChassisSummary.getFailureCount(),
                tplBodyCategoryOutcomes,
                tplSpecificationOutcomes,
                tplChineseOutcomes,
                tplElectricOutcomes,
                tplChineseElectricOutcomes,
                compBodyCategoryOutcomes,
                compSpecificationOutcomes,
                tplAgeRangeStats,
                compAgeRangeStats,
                tplManufactureYearStats,
                compManufactureYearStats,
                compEstimatedValueStats,
                tplSalesByBodyType,
                tplSalesByAgeRange,
                tplSalesByChineseClassification,
                tplSalesByFuelType,
                compSalesByBodyType,
                compSalesByAgeRange,
                compSalesByChineseClassification,
                compSalesByFuelType,
                tplTopRejectedModels,
                topRequestedMakeModels,
                tplTopRequestedMakeModels,
                compTopRequestedMakeModels,
                compTopRejectedMakeModels,
                tplErrorCounts,
                compErrorCounts,
                uniqueChassisBySpecification,
                uniqueChassisByInsurancePurpose,
                uniqueChassisByBodyType,
                manufactureYearTrend,
                customerAgeTrend);
    }

    private static QuoteStatistics.EidChassisSummary computeEidChassisSummary(List<QuoteRecord> records) {
        long total = 0;
        Set<String> uniqueKeys = new LinkedHashSet<>();

        for (QuoteRecord record : records) {
            String normalizedEid = normalizeEid(record.getEid().orElse(null));
            String normalizedChassis = normalizeChassisNumber(record.getChassisNumber().orElse(null));
            if (normalizedEid == null || normalizedChassis == null) {
                continue;
            }
            total++;
            uniqueKeys.add(normalizedEid + "::" + normalizedChassis);
        }

        long unique = uniqueKeys.size();
        long duplicate = Math.max(0, total - unique);
        return new QuoteStatistics.EidChassisSummary(total, unique, duplicate);
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

    private static int ageOrderKey(String label) {
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
            String chassis = normalizeChassisNumber(record.getChassisNumber().orElse(null));
            if (chassis == null) {
                continue;
            }
            uniqueValues.add(chassis);
            if (record.isSuccessful()) {
                successValues.add(chassis);
            } else if (record.isFailure()) {
                failureValues.add(chassis);
            }
        }

        return new UniqueChassisSummary(uniqueValues.size(), successValues.size(), failureValues.size());
    }

    private static QuoteStatistics.UniqueRequestSummary computeUniqueRequestSummary(List<QuoteRecord> records) {
        Set<String> uniqueKeys = new LinkedHashSet<>();
        Set<String> successKeys = new LinkedHashSet<>();
        Set<String> failureKeys = new LinkedHashSet<>();

        for (QuoteRecord record : records) {
            String requestKey = buildRequestKey(record);
            uniqueKeys.add(requestKey);
            if (record.isSuccessful()) {
                successKeys.add(requestKey);
            } else if (record.isFailure()) {
                failureKeys.add(requestKey);
            }
        }

        return new QuoteStatistics.UniqueRequestSummary(uniqueKeys.size(), successKeys.size(), failureKeys.size());
    }

    private static String buildRequestKey(QuoteRecord record) {
        String chassis = normalizeChassisNumber(record.getChassisNumber().orElse(null));
        String eid = normalizeEid(record.getEid().orElse(null));

        if (eid != null && chassis != null) {
            return "EID:" + eid + "::CH:" + chassis;
        }
        if (chassis != null) {
            return "CHASSIS_ONLY:" + chassis;
        }
        if (eid != null) {
            return "EID_ONLY:" + eid;
        }
        String quoteNumber = normalizeSimpleIdentifier(record.getQuoteNumber().orElse(null));
        if (quoteNumber != null) {
            return "QUOTE:" + quoteNumber;
        }
        return "FALLBACK:" + System.identityHashCode(record);
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
                .filter(Optional::isPresent)
                .map(Optional::get)
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
            return Collections.emptyList();
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
            String chassisNumber = normalizeChassisNumber(chassisOptional.get());
            if (model.isBlank() || chassisNumber == null) {
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

    private static List<QuoteStatistics.MakeModelChassisSummary> computeTopMakeModelByUniqueChassis(
            List<QuoteRecord> records,
            int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        Map<MakeModelKey, MakeModelChassisAccumulator> accumulatorByMakeModel = new HashMap<>();
        for (QuoteRecord record : records) {
            String chassis = normalizeChassisNumber(record.getChassisNumber().orElse(null));
            if (chassis == null) {
                continue;
            }
            MakeModelKey key = new MakeModelKey(record.getMakeLabel(), record.getModelLabel());
            MakeModelChassisAccumulator accumulator =
                    accumulatorByMakeModel.computeIfAbsent(key, ignored -> new MakeModelChassisAccumulator());
            accumulator.record(chassis, record.isSuccessful(), record.isFailure());
        }

        List<QuoteStatistics.MakeModelChassisSummary> results = accumulatorByMakeModel.entrySet().stream()
                .map(entry -> entry.getValue().toSummary(entry.getKey()))
                .sorted(Comparator
                        .comparingLong(QuoteStatistics.MakeModelChassisSummary::getUniqueChassisCount)
                        .reversed()
                        .thenComparing(QuoteStatistics.MakeModelChassisSummary::getMake)
                        .thenComparing(QuoteStatistics.MakeModelChassisSummary::getModel))
                .collect(Collectors.toList());

        if (results.size() > limit) {
            return new ArrayList<>(results.subList(0, limit));
        }
        return results;
    }

    private static List<QuoteStatistics.MakeModelChassisSummary> computeTopRejectedMakeModelByUniqueChassis(
            List<QuoteRecord> records,
            int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        Map<MakeModelKey, MakeModelChassisAccumulator> accumulatorByMakeModel = new HashMap<>();
        for (QuoteRecord record : records) {
            if (!record.isFailure() && !record.isSuccessful()) {
                continue;
            }
            String chassis = normalizeChassisNumber(record.getChassisNumber().orElse(null));
            if (chassis == null) {
                continue;
            }
            MakeModelKey key = new MakeModelKey(record.getMakeLabel(), record.getModelLabel());
            MakeModelChassisAccumulator accumulator =
                    accumulatorByMakeModel.computeIfAbsent(key, ignored -> new MakeModelChassisAccumulator());
            accumulator.record(chassis, record.isSuccessful(), record.isFailure());
        }

        List<QuoteStatistics.MakeModelChassisSummary> summaries = accumulatorByMakeModel.entrySet().stream()
                .map(entry -> entry.getValue().toSummary(entry.getKey()))
                .filter(summary -> summary.getFailedUniqueChassisCount() > 0)
                .sorted(Comparator
                        .comparingLong(QuoteStatistics.MakeModelChassisSummary::getFailedUniqueChassisCount)
                        .reversed()
                        .thenComparingLong(QuoteStatistics.MakeModelChassisSummary::getUniqueChassisCount)
                        .reversed()
                        .thenComparing(QuoteStatistics.MakeModelChassisSummary::getMake)
                        .thenComparing(QuoteStatistics.MakeModelChassisSummary::getModel))
                .collect(Collectors.toList());

        if (summaries.size() > limit) {
            return new ArrayList<>(summaries.subList(0, limit));
        }
        return summaries;
    }

    private static List<QuoteStatistics.CategoryCount> computeUniqueChassisCountsBySpecification(
            List<QuoteRecord> records) {
        Map<String, Set<String>> chassisBySpecification = new LinkedHashMap<>();
        for (QuoteRecord record : records) {
            String chassisNumber = normalizeChassisNumber(record.getChassisNumber().orElse(null));
            if (chassisNumber == null) {
                continue;
            }
            String label = classifySpecification(record.getOverrideSpecification());
            Set<String> chassis = chassisBySpecification.computeIfAbsent(label, ignored -> new LinkedHashSet<>());
            chassis.add(chassisNumber);
        }

        long gccCount = chassisBySpecification.getOrDefault(SPEC_LABEL_GCC, Collections.emptySet()).size();
        long nonGccCount = chassisBySpecification.getOrDefault(SPEC_LABEL_NON_GCC, Collections.emptySet()).size();
        long unknownCount = chassisBySpecification.getOrDefault(SPEC_LABEL_UNKNOWN, Collections.emptySet()).size();

        if (gccCount == 0 && nonGccCount == 0 && unknownCount == 0) {
            return Collections.singletonList(new QuoteStatistics.CategoryCount("No Data", 0));
        }

        List<QuoteStatistics.CategoryCount> counts = new ArrayList<>();
        counts.add(new QuoteStatistics.CategoryCount(SPEC_LABEL_GCC, gccCount));
        counts.add(new QuoteStatistics.CategoryCount(SPEC_LABEL_NON_GCC, nonGccCount));
        if (unknownCount > 0) {
            counts.add(new QuoteStatistics.CategoryCount(SPEC_LABEL_UNKNOWN, unknownCount));
        }
        return counts;
    }

    private static String classifySpecification(String value) {
        if (value == null) {
            return SPEC_LABEL_UNKNOWN;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return SPEC_LABEL_UNKNOWN;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (SPEC_LABEL_UNKNOWN.toLowerCase(Locale.ROOT).equals(lower)) {
            return SPEC_LABEL_UNKNOWN;
        }
        String condensed = lower.replace("-", "").replace(" ", "");
        switch (condensed) {
            case "gcc":
            case "gccspec":
            case "gccspecification":
            case "true":
            case "1":
            case "yes":
                return SPEC_LABEL_GCC;
            case "nongcc":
            case "nonegcc":
            case "nongccspec":
            case "false":
            case "0":
            case "no":
                return SPEC_LABEL_NON_GCC;
            default:
                return SPEC_LABEL_UNKNOWN;
        }
    }

    private static List<QuoteStatistics.CategoryCount> computeUniqueChassisCounts(
            List<QuoteRecord> records,
            Function<QuoteRecord, String> classifier) {
        List<QuoteStatistics.CategoryCount> counts = new ArrayList<>();
        Map<String, Set<String>> uniqueChassis = new HashMap<>();
        for (QuoteRecord record : records) {
            String chassisNumber = normalizeChassisNumber(record.getChassisNumber().orElse(null));
            if (chassisNumber == null) {
                continue;
            }
            String label = classifier.apply(record);
            if (label == null || label.isBlank()) {
                label = "Unknown";
            }
            Set<String> chassis = uniqueChassis.computeIfAbsent(label, ignored -> new LinkedHashSet<>());
            chassis.add(chassisNumber);
        }
        for (Map.Entry<String, Set<String>> entry : uniqueChassis.entrySet()) {
            counts.add(new QuoteStatistics.CategoryCount(entry.getKey(), entry.getValue().size()));
        }
        counts.sort(Comparator.comparingLong(QuoteStatistics.CategoryCount::getCount).reversed()
                .thenComparing(QuoteStatistics.CategoryCount::getLabel));
        if (counts.isEmpty()) {
            counts.add(new QuoteStatistics.CategoryCount("No Data", 0));
        }
        return counts;
    }

    private static List<QuoteStatistics.TrendPoint> computeManufactureYearTrend(List<QuoteRecord> records) {
        Map<String, ManufactureYearRequest> requestByKey = new LinkedHashMap<>();
        for (QuoteRecord record : records) {
            String requestKey = buildRequestKey(record);
            ManufactureYearRequest request = requestByKey.computeIfAbsent(requestKey,
                    ignored -> new ManufactureYearRequest());
            request.record(record);
        }

        if (requestByKey.isEmpty()) {
            return Collections.singletonList(new QuoteStatistics.TrendPoint("No Data", 0, 0));
        }

        Map<String, TrendCounter> countsByLabel = new HashMap<>();
        for (ManufactureYearRequest request : requestByKey.values()) {
            String label = request.getLabel();
            TrendCounter counter = countsByLabel.computeIfAbsent(label, ignored -> new TrendCounter());
            counter.incrementTotal();
            if (request.hasFailure()) {
                counter.incrementFailed();
            }
        }

        return countsByLabel.entrySet().stream()
                .sorted(Comparator
                        .comparingInt((Map.Entry<String, TrendCounter> entry) ->
                                yearOrderKey(entry.getKey()))
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> new QuoteStatistics.TrendPoint(entry.getKey(),
                        entry.getValue().getTotal(), entry.getValue().getFailed()))
                .collect(Collectors.toList());
    }

    private static List<QuoteStatistics.TrendPoint> computeCustomerAgeTrend(List<QuoteRecord> records) {
        Map<String, CustomerAgeRequest> requestByKey = new LinkedHashMap<>();
        for (QuoteRecord record : records) {
            String requestKey = buildRequestKey(record);
            CustomerAgeRequest request = requestByKey.computeIfAbsent(requestKey,
                    ignored -> new CustomerAgeRequest());
            request.record(record);
        }

        if (requestByKey.isEmpty()) {
            return Collections.singletonList(new QuoteStatistics.TrendPoint("No Data", 0, 0));
        }

        Map<String, TrendCounter> countsByLabel = new HashMap<>();
        for (CustomerAgeRequest request : requestByKey.values()) {
            String label = request.getLabel();
            TrendCounter counter = countsByLabel.computeIfAbsent(label, ignored -> new TrendCounter());
            counter.incrementTotal();
            if (request.hasFailure()) {
                counter.incrementFailed();
            }
        }

        return countsByLabel.entrySet().stream()
                .sorted(Comparator
                        .comparingInt((Map.Entry<String, TrendCounter> entry) ->
                                ageOrderKey(entry.getKey()))
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> new QuoteStatistics.TrendPoint(entry.getKey(),
                        entry.getValue().getTotal(), entry.getValue().getFailed()))
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

    private static List<QuoteStatistics.SalesConversionStats> computeSalesConversionByBodyType(List<QuoteRecord> records) {
        Map<String, SalesAccumulator> accumulatorByBodyType = new LinkedHashMap<>();
        for (QuoteRecord record : records) {
            String label = record.getBodyCategoryLabel();
            SalesAccumulator accumulator = accumulatorByBodyType.computeIfAbsent(label,
                    ignored -> new SalesAccumulator());
            recordSales(accumulator, record);
        }

        List<Map.Entry<String, SalesAccumulator>> ordered = new ArrayList<>(accumulatorByBodyType.entrySet());
        ordered.sort(Comparator.<Map.Entry<String, SalesAccumulator>>comparingLong(
                        entry -> entry.getValue().getTotalRequests())
                .reversed()
                .thenComparing(Map.Entry::getKey));

        List<QuoteStatistics.SalesConversionStats> results = new ArrayList<>();
        for (Map.Entry<String, SalesAccumulator> entry : ordered) {
            QuoteStatistics.SalesConversionStats stats = entry.getValue()
                    .toSalesConversionStats(entry.getKey());
            if (stats.hasData()) {
                results.add(stats);
            }
        }

        if (results.isEmpty()) {
            results.add(new QuoteStatistics.SalesConversionStats(NO_DATA_LABEL, 0, 0, 0));
        }
        return results;
    }

    private static List<QuoteStatistics.SalesConversionStats> computeSalesConversionByAgeRange(List<QuoteRecord> records) {
        Map<AgeRange, SalesAccumulator> accumulatorByRange = new LinkedHashMap<>();
        for (AgeRange range : AGE_RANGES) {
            accumulatorByRange.put(range, new SalesAccumulator());
        }
        SalesAccumulator otherAccumulator = new SalesAccumulator();

        for (QuoteRecord record : records) {
            SalesAccumulator accumulator = otherAccumulator;
            Optional<Integer> ageOptional = record.getDriverAge();
            if (ageOptional.isPresent()) {
                AgeRange matchingRange = findAgeRange(ageOptional.get());
                if (matchingRange != null) {
                    accumulator = accumulatorByRange.get(matchingRange);
                }
            }
            recordSales(accumulator, record);
        }

        List<QuoteStatistics.SalesConversionStats> results = new ArrayList<>();
        for (AgeRange range : AGE_RANGES) {
            SalesAccumulator accumulator = accumulatorByRange.get(range);
            QuoteStatistics.SalesConversionStats stats = accumulator.toSalesConversionStats(range.getLabel());
            if (stats.hasData()) {
                results.add(stats);
            }
        }
        QuoteStatistics.SalesConversionStats otherStats = otherAccumulator.toSalesConversionStats(OTHER_AGE_LABEL);
        if (otherStats.hasData()) {
            results.add(otherStats);
        }

        if (results.isEmpty()) {
            results.add(new QuoteStatistics.SalesConversionStats(NO_DATA_LABEL, 0, 0, 0));
        }
        return results;
    }

    private static List<QuoteStatistics.SalesConversionStats> computeSalesConversionByClassifier(
            List<QuoteRecord> records,
            Function<QuoteRecord, String> classifier) {
        LinkedHashMap<String, SalesAccumulator> accumulatorByLabel = new LinkedHashMap<>();
        for (QuoteRecord record : records) {
            String label = classifier.apply(record);
            if (label == null || label.isEmpty()) {
                label = NO_DATA_LABEL;
            }
            SalesAccumulator accumulator = accumulatorByLabel.computeIfAbsent(label, ignored -> new SalesAccumulator());
            recordSales(accumulator, record);
        }

        List<QuoteStatistics.SalesConversionStats> results = new ArrayList<>();
        for (Map.Entry<String, SalesAccumulator> entry : accumulatorByLabel.entrySet()) {
            QuoteStatistics.SalesConversionStats stats = entry.getValue().toSalesConversionStats(entry.getKey());
            if (stats.hasData()) {
                results.add(stats);
            }
        }

        if (results.isEmpty()) {
            results.add(new QuoteStatistics.SalesConversionStats(NO_DATA_LABEL, 0, 0, 0));
        }
        return results;
    }

    private static List<QuoteStatistics.ManufactureYearStats> computeManufactureYearStats(List<QuoteRecord> records) {
        LinkedHashMap<String, OutcomeAccumulator> predefinedBuckets = new LinkedHashMap<>();
        OutcomeAccumulator before2000 = new OutcomeAccumulator();
        predefinedBuckets.put("<2000", before2000);

        int currentYear = Year.now().getValue();
        int endYear = currentYear + 1;
        for (int year = 2000; year <= endYear; year++) {
            predefinedBuckets.put(String.valueOf(year), new OutcomeAccumulator());
        }

        TreeMap<Integer, OutcomeAccumulator> futureBuckets = new TreeMap<>();
        OutcomeAccumulator unknownBucket = new OutcomeAccumulator();

        for (QuoteRecord record : records) {
            if (!record.isSuccessful() && !record.isFailure()) {
                continue;
            }
            OutcomeAccumulator accumulator = null;
            Optional<Integer> manufactureYear = record.getManufactureYear();
            if (manufactureYear.isEmpty()) {
                accumulator = unknownBucket;
            } else {
                int year = manufactureYear.get();
                if (year < 2000) {
                    accumulator = before2000;
                } else if (year <= endYear) {
                    accumulator = predefinedBuckets.get(String.valueOf(year));
                } else {
                    accumulator = futureBuckets.computeIfAbsent(year, ignored -> new OutcomeAccumulator());
                }
            }

            if (accumulator == null) {
                continue;
            }

            if (record.isSuccessful()) {
                accumulator.incrementSuccess();
            } else {
                accumulator.incrementFailure();
            }
        }

        List<QuoteStatistics.ManufactureYearStats> results = new ArrayList<>();
        for (Map.Entry<String, OutcomeAccumulator> entry : predefinedBuckets.entrySet()) {
            results.add(entry.getValue().toManufactureYearStats(entry.getKey()));
        }
        for (Map.Entry<Integer, OutcomeAccumulator> entry : futureBuckets.entrySet()) {
            results.add(entry.getValue().toManufactureYearStats(String.valueOf(entry.getKey())));
        }
        if (unknownBucket.total() > 0) {
            results.add(unknownBucket.toManufactureYearStats("Unknown"));
        }
        return results;
    }

    private static void recordSales(SalesAccumulator accumulator, QuoteRecord record) {
        boolean processed = false;
        if (record.isSuccessful()) {
            accumulator.incrementTotal();
            accumulator.incrementSuccessful();
            processed = true;
        } else if (record.isFailure()) {
            accumulator.incrementTotal();
            processed = true;
        }

        if (record.hasPolicyNumber()) {
            accumulator.incrementSold();
            if (!processed) {
                accumulator.incrementTotal();
                accumulator.incrementSuccessful();
            }
        }
    }

    private static final class SalesAccumulator {
        private long totalRequests;
        private long successfulQuotes;
        private long soldPolicies;

        private void incrementTotal() {
            totalRequests++;
        }

        private void incrementSuccessful() {
            successfulQuotes++;
        }

        private void incrementSold() {
            soldPolicies++;
        }

        private long getTotalRequests() {
            return totalRequests;
        }

        private QuoteStatistics.SalesConversionStats toSalesConversionStats(String label) {
            return new QuoteStatistics.SalesConversionStats(label, totalRequests, successfulQuotes, soldPolicies);
        }
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

    private static final class MakeModelChassisAccumulator {
        private final Map<String, ChassisOutcome> outcomesByChassis = new HashMap<>();

        private void record(String chassis, boolean success, boolean failure) {
            ChassisOutcome outcome = outcomesByChassis.computeIfAbsent(chassis, key -> new ChassisOutcome());
            if (success) {
                outcome.markSuccess();
            }
            if (failure) {
                outcome.markFailure();
            }
        }

        private QuoteStatistics.MakeModelChassisSummary toSummary(MakeModelKey key) {
            long successCount = 0;
            long failureCount = 0;
            for (ChassisOutcome outcome : outcomesByChassis.values()) {
                if (outcome.hasSuccess()) {
                    successCount++;
                } else if (outcome.hasFailure()) {
                    failureCount++;
                }
            }
            return new QuoteStatistics.MakeModelChassisSummary(
                    key.getMake(),
                    key.getModel(),
                    outcomesByChassis.size(),
                    successCount,
                    failureCount);
        }

        private static final class ChassisOutcome {
            private boolean success;
            private boolean failure;

            private void markSuccess() {
                success = true;
            }

            private void markFailure() {
                failure = true;
            }

            private boolean hasSuccess() {
                return success;
            }

            private boolean hasFailure() {
                return failure;
            }
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

        private QuoteStatistics.ManufactureYearStats toManufactureYearStats(String label) {
            return new QuoteStatistics.ManufactureYearStats(label, success, failure);
        }
    }

    private static final class MakeModelKey {
        private final String make;
        private final String model;

        private MakeModelKey(String make, String model) {
            this.make = make;
            this.model = model;
        }

        private String getMake() {
            return make;
        }

        private String getModel() {
            return model;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MakeModelKey)) {
                return false;
            }
            MakeModelKey other = (MakeModelKey) obj;
            return Objects.equals(make, other.make) && Objects.equals(model, other.model);
        }

        @Override
        public int hashCode() {
            return Objects.hash(make, model);
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

    private static final List<AgeRange> AGE_RANGES = Collections.unmodifiableList(Arrays.asList(
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
    ));

    private static final List<ValueRange> VALUE_RANGES = Collections.unmodifiableList(Arrays.asList(
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
    ));

    private static final class ManufactureYearRequest {
        private String label;
        private boolean failure;

        private void record(QuoteRecord record) {
            String candidate = record.getManufactureYear()
                    .map(String::valueOf)
                    .orElse("Unknown");
            if (label == null || ("Unknown".equals(label) && !"Unknown".equals(candidate))) {
                label = candidate;
            }
            if (record.isFailure()) {
                failure = true;
            }
        }

        private String getLabel() {
            return label == null ? "Unknown" : label;
        }

        private boolean hasFailure() {
            return failure;
        }
    }

    private static final class CustomerAgeRequest {
        private String label;
        private boolean failure;

        private void record(QuoteRecord record) {
            Integer ageValue = record.getDriverAge()
                    .filter(age -> age > 0)
                    .orElse(null);
            String candidate = ageValue != null ? String.valueOf(ageValue) : "Unknown";
            if (label == null || ("Unknown".equals(label) && !"Unknown".equals(candidate))) {
                label = candidate;
            }
            if (record.isFailure()) {
                failure = true;
            }
        }

        private String getLabel() {
            return label == null ? "Unknown" : label;
        }

        private boolean hasFailure() {
            return failure;
        }
    }

    private static final class TrendCounter {
        private long total;
        private long failed;

        private void incrementTotal() {
            total++;
        }

        private void incrementFailed() {
            failed++;
        }

        private long getTotal() {
            return total;
        }

        private long getFailed() {
            return failed;
        }
    }

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
