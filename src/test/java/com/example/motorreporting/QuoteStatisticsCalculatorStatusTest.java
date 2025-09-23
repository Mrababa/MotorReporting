package com.example.motorreporting;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuoteStatisticsCalculatorStatusTest {

    @Test
    void successAndFailureCountsFollowStatusColumn() {
        QuoteRecord successRecord = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Third Party",
                "Status", "Success",
                "ErrorText", "Vehicle inspection required",
                "QuotationNo", ""
        ));

        QuoteRecord failureRecord = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Comprehensive",
                "Status", "Failed",
                "ErrorText", "",
                "QuotationNo", "QTN-0042"
        ));

        QuoteRecord skippedRecord = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Third Party",
                "Status", "Skipped",
                "ErrorText", "",
                "QuotationNo", "QTN-0099"
        ));

        QuoteStatistics statistics = QuoteStatisticsCalculator.calculate(List.of(
                successRecord,
                failureRecord,
                skippedRecord
        ));

        assertEquals(1, statistics.getOverallPassCount());
        assertEquals(1, statistics.getOverallFailCount());
        assertEquals(1, statistics.getOverallSkipCount());
    }

    @Test
    void uniqueChassisCountsTreatIdentifiersCaseInsensitively() {
        QuoteRecord firstSuccess = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Third Party",
                "Status", "Success",
                "QuotationNo", "Q-1001",
                "ChassisNumber", "chs123"
        ));

        QuoteRecord repeatFailureSameChassis = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Comprehensive",
                "Status", "Failed",
                "ErrorText", "declined",
                "ChassisNumber", "CHS123"
        ));

        QuoteRecord failureWithWhitespace = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Third Party",
                "Status", "Failed",
                "ErrorText", "declined",
                "ChassisNumber", " CHS456 "
        ));

        QuoteRecord secondSuccess = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Comprehensive",
                "Status", "Success",
                "QuotationNo", "Q-2002",
                "ChassisNumber", "Chs-789"
        ));

        QuoteStatistics statistics = QuoteStatisticsCalculator.calculate(List.of(
                firstSuccess,
                repeatFailureSameChassis,
                failureWithWhitespace,
                secondSuccess
        ));

        assertEquals(3, statistics.getUniqueChassisCount());
        assertEquals(2, statistics.getUniqueChassisSuccessCount());
        assertEquals(2, statistics.getUniqueChassisFailCount());
    }
}
