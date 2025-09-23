package com.example.motorreporting;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a single quote attempt as loaded from the source data.
 */
public class QuoteRecord {

    private static final String STATUS_SUCCESS = "pass";
    private static final String STATUS_SUCCESS_ALT = "success";

    private final Map<String, String> rawValues;
    private final String insuranceType;
    private final String status;
    private final String errorText;
    private final Integer manufactureYear;
    private final BigDecimal estimatedValue;

    private QuoteRecord(Map<String, String> rawValues,
                        String insuranceType,
                        String status,
                        String errorText,
                        Integer manufactureYear,
                        BigDecimal estimatedValue) {
        this.rawValues = Collections.unmodifiableMap(new HashMap<>(rawValues));
        this.insuranceType = insuranceType;
        this.status = status;
        this.errorText = errorText;
        this.manufactureYear = manufactureYear;
        this.estimatedValue = estimatedValue;
    }

    public static QuoteRecord fromValues(Map<String, String> values) {
        Map<String, String> normalized = new HashMap<>();
        values.forEach((key, value) -> {
            if (key != null) {
                normalized.put(key.trim(), value == null ? "" : value.trim());
            }
        });

        String insuranceType = normalized.getOrDefault("InsuranceType", "");
        String status = normalized.getOrDefault("Status", "");
        String errorText = normalized.getOrDefault("ErrorText", "");
        Integer manufactureYear = parseInteger(normalized.get("ManufactureYear"));
        BigDecimal estimatedValue = parseBigDecimal(normalized.get("EstimatedValue"));

        return new QuoteRecord(normalized, insuranceType, status, errorText, manufactureYear, estimatedValue);
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        String sanitized = value.replace(",", "").trim();
        try {
            return new BigDecimal(sanitized);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    public boolean belongsTo(GroupType groupType) {
        return groupType.matches(insuranceType);
    }

    public boolean isSuccessful() {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return STATUS_SUCCESS.equals(normalized) || STATUS_SUCCESS_ALT.equals(normalized);
    }

    public String getFailureReason() {
        if (errorText == null || errorText.isBlank()) {
            return "Unknown";
        }
        return errorText.trim();
    }

    public Optional<Integer> getManufactureYear() {
        return Optional.ofNullable(manufactureYear);
    }

    public BigDecimal getEstimatedValue() {
        return estimatedValue;
    }

    public String getInsuranceType() {
        return insuranceType;
    }

    public String getStatus() {
        return status;
    }

    public Map<String, String> getRawValues() {
        return rawValues;
    }
}
