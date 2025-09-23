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
}
