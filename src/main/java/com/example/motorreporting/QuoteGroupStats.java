package com.example.motorreporting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Aggregate statistics for a group of quotes.
 */
public class QuoteGroupStats {

    private final GroupType groupType;
    private final long totalQuotes;
    private final long passCount;
    private final long failCount;
    private final long skipCount;
    private final double failurePercentage;
    private final Map<String, Long> failureReasonCounts;
    private final Map<String, Long> failuresByManufactureYear;
    private final BigDecimal totalBlockedEstimatedValue;

    public QuoteGroupStats(GroupType groupType,
                           long totalQuotes,
                           long passCount,
                           long failCount,
                           long skipCount,
                           double failurePercentage,
                           Map<String, Long> failureReasonCounts,
                           Map<String, Long> failuresByManufactureYear,
                           BigDecimal totalBlockedEstimatedValue) {
        this.groupType = Objects.requireNonNull(groupType, "groupType");
        this.totalQuotes = totalQuotes;
        this.passCount = passCount;
        this.failCount = failCount;
        this.skipCount = skipCount;
        this.failurePercentage = failurePercentage;
        this.failureReasonCounts = Collections.unmodifiableMap(new LinkedHashMap<>(failureReasonCounts));
        this.failuresByManufactureYear = Collections.unmodifiableMap(new LinkedHashMap<>(failuresByManufactureYear));
        this.totalBlockedEstimatedValue = totalBlockedEstimatedValue.setScale(2, RoundingMode.HALF_UP);
    }

    public static QuoteGroupStats empty(GroupType groupType) {
        return new QuoteGroupStats(groupType, 0, 0, 0, 0, 0.0,
                Collections.emptyMap(), Collections.emptyMap(), BigDecimal.ZERO);
    }

    public GroupType getGroupType() {
        return groupType;
    }

    public long getTotalQuotes() {
        return totalQuotes;
    }

    public long getPassCount() {
        return passCount;
    }

    public long getFailCount() {
        return failCount;
    }

    public long getSkipCount() {
        return skipCount;
    }

    public double getFailurePercentage() {
        return failurePercentage;
    }

    public Map<String, Long> getFailureReasonCounts() {
        return failureReasonCounts;
    }

    public Map<String, Long> getFailuresByManufactureYear() {
        return failuresByManufactureYear;
    }

    public BigDecimal getTotalBlockedEstimatedValue() {
        return totalBlockedEstimatedValue;
    }

    public boolean hasQuotes() {
        return totalQuotes > 0;
    }

    public List<Map.Entry<String, Long>> getTopFailureReasons(int limit) {
        return failureReasonCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
