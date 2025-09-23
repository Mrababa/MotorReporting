package com.example.motorreporting;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Represents the insurance type groupings used in the report.
 */
public enum GroupType {
    TPL("Third Party", "Third Party Liability (TPL)", "TPL"),
    COMPREHENSIVE("Comprehensive", "Comprehensive (Comp)", "Comp");

    private final String canonicalValue;
    private final String displayName;
    private final String shortLabel;

    GroupType(String canonicalValue, String displayName, String shortLabel) {
        this.canonicalValue = canonicalValue;
        this.displayName = displayName;
        this.shortLabel = shortLabel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public boolean matches(String insuranceType) {
        if (insuranceType == null) {
            return false;
        }
        return canonicalValue.equalsIgnoreCase(insuranceType.trim());
    }

    public static Optional<GroupType> fromInsuranceType(String insuranceType) {
        if (insuranceType == null) {
            return Optional.empty();
        }
        final String normalized = insuranceType.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.canonicalValue.toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }
}
