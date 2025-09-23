package com.example.motorreporting;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a single quote attempt as loaded from the source data.
 */
public class QuoteRecord {

    private final Map<String, String> rawValues;
    private final String insuranceType;
    private final String status;
    private final String errorText;
    private final String quoteNumber;
    private final Integer manufactureYear;
    private final BigDecimal estimatedValue;

    private QuoteRecord(Map<String, String> rawValues,
                        String insuranceType,
                        String status,
                        String errorText,
                        Integer manufactureYear,
                        BigDecimal estimatedValue,
                        String quoteNumber) {
        this.rawValues = Collections.unmodifiableMap(new HashMap<>(rawValues));
        this.insuranceType = insuranceType;
        this.status = status;
        this.errorText = errorText;
        this.manufactureYear = manufactureYear;
        this.estimatedValue = estimatedValue;
        this.quoteNumber = quoteNumber;
    }

    public static QuoteRecord fromValues(Map<String, String> values) {
        Map<String, String> normalized = new HashMap<>();
        values.forEach((key, value) -> {
            if (key != null) {
                normalized.put(key.trim(), value == null ? "" : value.trim());
            }
        });

        ensureStatusColumn(normalized);

        String errorText = getValueIgnoreCase(normalized, "ErrorText");
        String quotationNumber = getValueIgnoreCase(normalized, "QuotationNo");
        String derivedStatus = determineStatus(errorText, quotationNumber);
        setValueIgnoreCase(normalized, "Status", derivedStatus);
        normalizeOverrideIsGccSpec(normalized);

        String insuranceType = getValueIgnoreCase(normalized, "InsuranceType");
        String status = getValueIgnoreCase(normalized, "Status");
        Integer manufactureYear = parseInteger(getValueIgnoreCase(normalized, "ManufactureYear"));
        BigDecimal estimatedValue = parseBigDecimal(getValueIgnoreCase(normalized, "EstimatedValue"));
        String quoteNumber = extractQuoteNumber(normalized);

        return new QuoteRecord(normalized, insuranceType, status, errorText, manufactureYear, estimatedValue, quoteNumber);
    }

    private static String getValueIgnoreCase(Map<String, String> values, String key) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return "";
    }

    private static void setValueIgnoreCase(Map<String, String> values, String key, String newValue) {
        String existingKey = null;
        for (String currentKey : values.keySet()) {
            if (currentKey != null && currentKey.equalsIgnoreCase(key)) {
                existingKey = currentKey;
                break;
            }
        }
        if (existingKey != null) {
            values.put(existingKey, newValue);
        } else {
            values.put(key, newValue);
        }
    }

    private static void ensureStatusColumn(Map<String, String> values) {
        if (!containsKeyIgnoreCase(values, "Status")) {
            values.put("Status", "");
        }
    }

    private static boolean containsKeyIgnoreCase(Map<String, String> values, String key) {
        for (String currentKey : values.keySet()) {
            if (currentKey != null && currentKey.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    private static String determineStatus(String errorText, String quotationNumber) {
      if (!"NULL".equals(errorText) ) {
            return "Failed";
        }
        if (!"NULL".equals(quotationNumber )) {
            return "Pass";
        }
        return "Skipped";
    }


    private static void normalizeOverrideIsGccSpec(Map<String, String> values) {
        String rawValue = getValueIgnoreCase(values, "OverrideIsGccSpec");
        if (rawValue == null) {
            return;
        }
        String trimmed = rawValue.trim();
        String replacement = null;
        if ("1".equals(trimmed)) {
            replacement = "GCC";
        } else if ("0".equals(trimmed)) {
            replacement = "None GCC";
        }
        if (replacement != null) {
            setValueIgnoreCase(values, "OverrideIsGccSpec", replacement);
        }
    }

    private static boolean isNullLiteral(String value) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || "Null".equals(trimmed);
    }

    private static String extractQuoteNumber(Map<String, String> values) {
        String[] possibleKeys = {
                "QuotationNo",
                "QuotationNumber",
                "QuoteNumber",
                "QuoteNo",
                "Quote #",
                "Quotation #"
        };
        for (String key : possibleKeys) {
            String value = getValueIgnoreCase(values, key);
            if (!isNullLiteral(value)) {
                return value.trim();
            }
        }
        return null;
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
        return !hasError() && hasQuoteNumber();
    }

    public boolean isFailure() {
        return hasError();
    }

    public boolean isSkipped() {
        return !hasError() && !hasQuoteNumber();
    }

    public String getFailureReason() {
        if (isNullLiteral(errorText)) {
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

    public Optional<String> getQuoteNumber() {
        return Optional.ofNullable(quoteNumber);
    }

    public boolean hasQuoteNumber() {
        return !isNullLiteral(quoteNumber);
    }

    public boolean hasError() {
        return !isNullLiteral(errorText);
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
