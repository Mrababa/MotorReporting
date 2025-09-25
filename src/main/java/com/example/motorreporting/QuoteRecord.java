package com.example.motorreporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a single quote attempt as loaded from the source data.
 */
public class QuoteRecord {

    private final Map<String, String> rawValues;
    private final String insuranceType;
    private final String status;
    private final String insurancePurpose;
    private final String insuranceCompanyName;
    private final String errorText;
    private final String quoteNumber;
    private final String policyNumber;
    private final Integer manufactureYear;
    private final BigDecimal estimatedValue;
    private final String chassisNumber;
    private final BigDecimal policyPremium;
    private final String eid;
    private final QuoteOutcome outcome;
    private final String bodyCategory;
    private final String overrideSpecification;
    private final String model;
    private final String make;
    private final Integer driverAge;
    private final LocalDateTime quoteRequestedOn;

    private static final DateTimeFormatter[] QUOTE_REQUESTED_ON_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    private QuoteRecord(Map<String, String> rawValues,
                        String insuranceType,
                        String status,
                        String insurancePurpose,
                        String insuranceCompanyName,
                        String errorText,
                        Integer manufactureYear,
                        BigDecimal estimatedValue,
                        String quoteNumber,
                        String policyNumber,
                        String chassisNumber,
                        BigDecimal policyPremium,
                        String eid,
                        QuoteOutcome outcome,
                        String bodyCategory,
                        String overrideSpecification,
                        Integer driverAge,
                        String model,
                        String make,
                        LocalDateTime quoteRequestedOn) {
        this.rawValues = Collections.unmodifiableMap(new HashMap<>(rawValues));
        this.insuranceType = insuranceType;
        this.status = status;
        this.insurancePurpose = insurancePurpose;
        this.insuranceCompanyName = insuranceCompanyName;
        this.errorText = errorText;
        this.manufactureYear = manufactureYear;
        this.estimatedValue = estimatedValue;
        this.quoteNumber = quoteNumber;
        this.policyNumber = policyNumber;
        this.chassisNumber = chassisNumber;
        this.policyPremium = policyPremium;
        this.eid = eid;
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.bodyCategory = bodyCategory;
        this.overrideSpecification = overrideSpecification;
        this.model = model;
        this.make = make;
        this.driverAge = driverAge;
        this.quoteRequestedOn = quoteRequestedOn;
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
        String rawStatus = getValueIgnoreCase(normalized, "Status");
        QuoteOutcome outcome = determineOutcome(rawStatus, errorText, quotationNumber);
        setValueIgnoreCase(normalized, "Status", outcome.getDisplayLabel());
        normalizeOverrideIsGccSpec(normalized);

        String insuranceType = getValueIgnoreCase(normalized, "InsuranceType");
        String status = outcome.getDisplayLabel();
        String insurancePurpose = normalizeCategoricalValue(getValueIgnoreCase(normalized, "InsurancePurpose"));
        String insuranceCompanyName = normalizeCategoricalValue(getValueIgnoreCase(normalized, "ICName"));
        Integer manufactureYear = parseInteger(getValueIgnoreCase(normalized, "ManufactureYear"));
        BigDecimal estimatedValue = parseBigDecimal(getValueIgnoreCase(normalized, "EstimatedValue"));
        String quoteNumber = extractQuoteNumber(normalized);
        String policyNumber = extractPolicyNumber(normalized);
        String chassisNumber = extractChassisNumber(normalized);
        BigDecimal policyPremium = extractPolicyPremium(normalized);
        String bodyCategory = normalizeCategoricalValue(getValueIgnoreCase(normalized, "BodyCategory"));
        String overrideSpec = normalizeCategoricalValue(getValueIgnoreCase(normalized, "OverrideIsGccSpec"));
        Integer driverAge = parseInteger(getValueIgnoreCase(normalized, "Age"));
        String model = normalizeCategoricalValue(getValueIgnoreCase(normalized, "ShoryModelEn"));
        String make = normalizeCategoricalValue(getValueIgnoreCase(normalized, "ShoryMakeEn"));
        String eid = extractEid(normalized);
        LocalDateTime quoteRequestedOn = parseQuoteRequestedOn(getValueIgnoreCase(normalized, "QuoteRequestedOn"));

        return new QuoteRecord(normalized, insuranceType, status, insurancePurpose, insuranceCompanyName, errorText,
                manufactureYear, estimatedValue, quoteNumber, policyNumber, chassisNumber, policyPremium, eid, outcome,
                bodyCategory, overrideSpec, driverAge, model, make, quoteRequestedOn);
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

    private static QuoteOutcome determineOutcome(String statusValue, String errorText, String quotationNumber) {
        Optional<QuoteOutcome> parsed = QuoteOutcome.fromStatusValue(statusValue);
        if (parsed.isPresent()) {
            return parsed.get();
        }
        if (!isNullLiteral(errorText)) {
            return QuoteOutcome.FAILURE;
        }
        if (!isNullLiteral(quotationNumber)) {
            return QuoteOutcome.SUCCESS;
        }
        return QuoteOutcome.SKIPPED;
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

    private static String normalizeCategoricalValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private static boolean isNullLiteral(String value) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        return "null".equalsIgnoreCase(trimmed);
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

    private static String extractPolicyNumber(Map<String, String> values) {
        String[] possibleKeys = {
                "PolicyNumber",
                "PolicyNo",
                "Policy #",
                "Policy No",
                "PolicyNo.",
                "Policy",
                "Policy Reference",
                "PolicyRef"
        };
        for (String key : possibleKeys) {
            String value = getValueIgnoreCase(values, key);
            if (!isNullLiteral(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static BigDecimal extractPolicyPremium(Map<String, String> values) {
        String[] possibleKeys = {
                "PolicyPremium",
                "Policy Premium",
                "TotalPremium",
                "Total Premium",
                "GrossPremium",
                "Gross Premium"
        };
        for (String key : possibleKeys) {
            BigDecimal premium = parseOptionalMonetaryValue(getValueIgnoreCase(values, key));
            if (premium != null) {
                return premium;
            }
        }
        return null;
    }

    private static String extractChassisNumber(Map<String, String> values) {
        String[] possibleKeys = {
                "ChassisNumber",
                "ChassisNo",
                "Chassis No",
                "VehicleIdentificationNumber",
                "VIN"
        };
        for (String key : possibleKeys) {
            String value = getValueIgnoreCase(values, key);
            String normalized = normalizeChassisNumber(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String extractEid(Map<String, String> values) {
        String[] possibleKeys = {
                "EID",
                "EIDNumber",
                "EID No",
                "EID#",
                "EmiratesID",
                "Emirates ID",
                "EmiratesIDNumber",
                "Emirates ID Number",
                "Emirates ID No",
                "EmiratesIDNo",
                "NationalID",
                "National ID",
                "NationalIDNumber",
                "CustomerEID",
                "Customer EID"
        };
        for (String key : possibleKeys) {
            String value = getValueIgnoreCase(values, key);
            String normalized = normalizeEid(value);
            if (normalized != null) {
                return normalized;
            }
        }

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String header = entry.getKey();
            if (header == null || header.isBlank()) {
                continue;
            }
            if (!isLikelyEidHeader(normalizeHeaderKey(header))) {
                continue;
            }
            String normalized = normalizeEid(entry.getValue());
            if (normalized != null) {
                return normalized;
            }
        }

        return null;
    }

    private static LocalDateTime parseQuoteRequestedOn(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter formatter : QUOTE_REQUESTED_ON_FORMATS) {
            try {
                return LocalDateTime.parse(trimmed, formatter);
            } catch (DateTimeParseException ex) {
                // try next
            }
        }
        try {
            LocalDate date = LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.atStartOfDay();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String normalizeChassisNumber(String value) {
        if (isNullLiteral(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        StringBuilder cleaned = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (!Character.isWhitespace(ch)) {
                cleaned.append(ch);
            }
        }
        if (cleaned.length() == 0) {
            return null;
        }
        return cleaned.toString().toUpperCase(Locale.ROOT);
    }

    private static String normalizeEid(String value) {
        if (isNullLiteral(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        StringBuilder cleaned = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                cleaned.append(Character.toUpperCase(ch));
            }
        }
        if (cleaned.length() == 0) {
            return null;
        }
        return cleaned.toString();
    }

    private static boolean isLikelyEidHeader(String normalizedHeader) {
        if (normalizedHeader == null || normalizedHeader.isEmpty()) {
            return false;
        }
        String lower = normalizedHeader.toLowerCase(Locale.ROOT);
        return lower.equals("eid")
                || lower.endsWith("eid")
                || lower.endsWith("eidnumber")
                || lower.endsWith("eidno")
                || lower.contains("emiratesid")
                || lower.equals("nationalid")
                || lower.endsWith("nationalid")
                || lower.endsWith("nationalidnumber")
                || lower.endsWith("nationalidno");
    }

    private static String normalizeHeaderKey(String header) {
        if (header == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(header.length());
        for (int i = 0; i < header.length(); i++) {
            char ch = header.charAt(i);
            if (Character.isWhitespace(ch) || ch == '_' || ch == '-' || ch == '#') {
                continue;
            }
            builder.append(Character.toLowerCase(ch));
        }
        return builder.toString();
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

    private static BigDecimal parseOptionalMonetaryValue(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = value.replaceAll("[^0-9.\\-]", "").trim();
        if (sanitized.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(sanitized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public boolean belongsTo(GroupType groupType) {
        return groupType.matches(insuranceType);
    }

    public boolean isSuccessful() {
        return outcome == QuoteOutcome.SUCCESS;
    }

    public boolean isFailure() {
        return outcome == QuoteOutcome.FAILURE;
    }

    public boolean isSkipped() {
        return outcome == QuoteOutcome.SKIPPED;
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

    public Optional<String> getPolicyNumber() {
        return Optional.ofNullable(policyNumber);
    }

    public Optional<BigDecimal> getPolicyPremium() {
        return Optional.ofNullable(policyPremium);
    }

    public Optional<String> getChassisNumber() {
        return Optional.ofNullable(chassisNumber);
    }

    public Optional<String> getInsuranceCompanyName() {
        return Optional.ofNullable(insuranceCompanyName);
    }

    public Optional<String> getEid() {
        return Optional.ofNullable(eid);
    }

    public Optional<LocalDateTime> getQuoteRequestedOn() {
        return Optional.ofNullable(quoteRequestedOn);
    }

    public boolean hasQuoteNumber() {
        return !isNullLiteral(quoteNumber);
    }

    public boolean hasPolicyNumber() {
        return !isNullLiteral(policyNumber);
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

    public Optional<String> getInsurancePurpose() {
        return Optional.ofNullable(insurancePurpose);
    }

    public String getInsurancePurposeLabel() {
        return labelOrUnknown(insurancePurpose);
    }

    public Map<String, String> getRawValues() {
        return rawValues;
    }

    public String getBodyCategory() {
        return bodyCategory;
    }

    public String getBodyCategoryLabel() {
        return labelOrUnknown(bodyCategory);
    }

    public String getOverrideSpecification() {
        return overrideSpecification;
    }

    public String getOverrideSpecificationLabel() {
        return labelOrUnknown(overrideSpecification);
    }

    public Optional<String> getModel() {
        return Optional.ofNullable(model);
    }

    public String getModelLabel() {
        return labelOrUnknown(model);
    }

    public Optional<String> getMake() {
        return Optional.ofNullable(make);
    }

    public String getMakeLabel() {
        return labelOrUnknown(make);
    }

    public Optional<Integer> getDriverAge() {
        return Optional.ofNullable(driverAge);
    }

    public Optional<String> getFailureErrorText() {
        if (isNullLiteral(errorText)) {
            return Optional.empty();
        }
        return Optional.of(errorText.trim());
    }

    public boolean isChineseQuote() {
        String value = getValueIgnoreCase(rawValues, "IsChinese");
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return false;
        }
        if ("1".equals(normalized)) {
            return true;
        }
        String lowerCase = normalized.toLowerCase(Locale.ROOT);
        return "true".equals(lowerCase) || "yes".equals(lowerCase);
    }

    public boolean isElectricVehicle() {
        String value = getValueIgnoreCase(rawValues, "FuelType");
        if (value == null) {
            return false;
        }
        return "ELECTRIC POWER".equalsIgnoreCase(value.trim());
    }

    public String getChineseClassificationLabel() {
        return isChineseQuote() ? "Chinese Quotes" : "Non-Chinese Quotes";
    }

    public String getElectricClassificationLabel() {
        return isElectricVehicle() ? "Electric Vehicles" : "Non-Electric Vehicles";
    }

    public String getChineseElectricSegmentLabel() {
        boolean chinese = isChineseQuote();
        boolean electric = isElectricVehicle();
        if (chinese && electric) {
            return "Chinese · Electric";
        }
        if (chinese) {
            return "Chinese · Non-Electric";
        }
        if (electric) {
            return "Non-Chinese · Electric";
        }
        return "Non-Chinese · Non-Electric";
    }

    private static String labelOrUnknown(String value) {
        if (value == null || value.isEmpty()) {
            return "Unknown";
        }
        return value;
    }

    private enum QuoteOutcome {
        SUCCESS("Success"),
        FAILURE("Failed"),
        SKIPPED("Skipped");

        private static final Set<String> SUCCESS_LABELS = Set.of(
                "success",
                "successful",
                "pass",
                "passed",
                "approved",
                "complete",
                "completed",
                "done"
        );

        private static final Set<String> FAILURE_LABELS = Set.of(
                "fail",
                "failed",
                "failure",
                "error",
                "declined",
                "rejected",
                "denied"
        );

        private static final Set<String> SKIPPED_LABELS = Set.of(
                "skip",
                "skipped",
                "pending",
                "not processed",
                "incomplete",
                "cancelled",
                "canceled",
                "void",
                "abandoned"
        );

        private final String displayLabel;

        QuoteOutcome(String displayLabel) {
            this.displayLabel = displayLabel;
        }

        String getDisplayLabel() {
            return displayLabel;
        }

        static Optional<QuoteOutcome> fromStatusValue(String statusValue) {
            if (statusValue == null) {
                return Optional.empty();
            }
            String normalized = statusValue.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty() || "null".equals(normalized)) {
                return Optional.empty();
            }
            if (SUCCESS_LABELS.contains(normalized)
                    || normalized.startsWith("success")
                    || normalized.startsWith("pass")) {
                return Optional.of(SUCCESS);
            }
            if (FAILURE_LABELS.contains(normalized)
                    || normalized.startsWith("fail")
                    || normalized.startsWith("error")
                    || normalized.startsWith("declin")
                    || normalized.startsWith("reject")) {
                return Optional.of(FAILURE);
            }
            if (SKIPPED_LABELS.contains(normalized)
                    || normalized.startsWith("skip")
                    || normalized.startsWith("pending")
                    || normalized.startsWith("cancel")
                    || normalized.startsWith("void")) {
                return Optional.of(SKIPPED);
            }
            return Optional.empty();
        }
    }
}
