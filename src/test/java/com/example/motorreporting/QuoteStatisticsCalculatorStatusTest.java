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
        assertEquals(1, statistics.getTplUniqueChassisSuccessCount());
        assertEquals(1, statistics.getTplUniqueChassisFailCount());
        assertEquals(1, statistics.getComprehensiveUniqueChassisSuccessCount());
        assertEquals(1, statistics.getComprehensiveUniqueChassisFailCount());
    }

    @Test
    void topMakeModelSummariesAreSplitByInsuranceType() {
        QuoteRecord tplSuccess = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Third Party",
                "Status", "Success",
                "ChassisNumber", "TPL-001",
                "ShoryMakeEn", "Toyota",
                "ShoryModelEn", "Corolla"
        ));

        QuoteRecord tplFailure = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Third Party",
                "Status", "Failed",
                "ErrorText", "declined",
                "ChassisNumber", "TPL-002",
                "ShoryMakeEn", "Toyota",
                "ShoryModelEn", "Corolla"
        ));

        QuoteRecord tplSecondSuccess = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Third Party",
                "Status", "Success",
                "ChassisNumber", "TPL-003",
                "ShoryMakeEn", "Nissan",
                "ShoryModelEn", "Sunny"
        ));

        QuoteRecord compFailure = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Comprehensive",
                "Status", "Failed",
                "ErrorText", "declined",
                "ChassisNumber", "COMP-001",
                "ShoryMakeEn", "Toyota",
                "ShoryModelEn", "Corolla"
        ));

        QuoteRecord compSuccess = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Comprehensive",
                "Status", "Success",
                "ChassisNumber", "COMP-002",
                "ShoryMakeEn", "Honda",
                "ShoryModelEn", "Civic"
        ));

        QuoteRecord compSecondFailure = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Comprehensive",
                "Status", "Failed",
                "ErrorText", "declined",
                "ChassisNumber", "COMP-003",
                "ShoryMakeEn", "Honda",
                "ShoryModelEn", "Civic"
        ));

        QuoteStatistics statistics = QuoteStatisticsCalculator.calculate(List.of(
                tplSuccess,
                tplFailure,
                tplSecondSuccess,
                compFailure,
                compSuccess,
                compSecondFailure
        ));

        List<QuoteStatistics.MakeModelChassisSummary> tplSummaries =
                statistics.getTplTopRequestedMakeModelsByUniqueChassis();
        assertEquals(2, tplSummaries.size());
        QuoteStatistics.MakeModelChassisSummary tplToyota = tplSummaries.get(0);
        assertEquals("Toyota", tplToyota.getMake());
        assertEquals("Corolla", tplToyota.getModel());
        assertEquals(2, tplToyota.getUniqueChassisCount());
        assertEquals(1, tplToyota.getSuccessfulUniqueChassisCount());
        assertEquals(1, tplToyota.getFailedUniqueChassisCount());

        QuoteStatistics.MakeModelChassisSummary tplNissan = tplSummaries.get(1);
        assertEquals("Nissan", tplNissan.getMake());
        assertEquals("Sunny", tplNissan.getModel());
        assertEquals(1, tplNissan.getUniqueChassisCount());
        assertEquals(1, tplNissan.getSuccessfulUniqueChassisCount());
        assertEquals(0, tplNissan.getFailedUniqueChassisCount());

        List<QuoteStatistics.MakeModelChassisSummary> compSummaries =
                statistics.getComprehensiveTopRequestedMakeModelsByUniqueChassis();
        assertEquals(2, compSummaries.size());
        QuoteStatistics.MakeModelChassisSummary compHonda = compSummaries.get(0);
        assertEquals("Honda", compHonda.getMake());
        assertEquals("Civic", compHonda.getModel());
        assertEquals(2, compHonda.getUniqueChassisCount());
        assertEquals(1, compHonda.getSuccessfulUniqueChassisCount());
        assertEquals(1, compHonda.getFailedUniqueChassisCount());

        QuoteStatistics.MakeModelChassisSummary compToyota = compSummaries.get(1);
        assertEquals("Toyota", compToyota.getMake());
        assertEquals("Corolla", compToyota.getModel());
        assertEquals(1, compToyota.getUniqueChassisCount());
        assertEquals(0, compToyota.getSuccessfulUniqueChassisCount());
        assertEquals(1, compToyota.getFailedUniqueChassisCount());

        List<QuoteStatistics.MakeModelChassisSummary> compRejected =
                statistics.getComprehensiveTopRejectedModelsByUniqueChassis();
        assertEquals(2, compRejected.size());

        QuoteStatistics.MakeModelChassisSummary rejectedHonda = compRejected.get(0);
        assertEquals("Honda", rejectedHonda.getMake());
        assertEquals("Civic", rejectedHonda.getModel());
        assertEquals(2, rejectedHonda.getUniqueChassisCount());
        assertEquals(1, rejectedHonda.getSuccessfulUniqueChassisCount());
        assertEquals(1, rejectedHonda.getFailedUniqueChassisCount());

        QuoteStatistics.MakeModelChassisSummary rejectedToyota = compRejected.get(1);
        assertEquals("Toyota", rejectedToyota.getMake());
        assertEquals("Corolla", rejectedToyota.getModel());
        assertEquals(1, rejectedToyota.getUniqueChassisCount());
        assertEquals(0, rejectedToyota.getSuccessfulUniqueChassisCount());
        assertEquals(1, rejectedToyota.getFailedUniqueChassisCount());
    }

    @Test
    void tplEidChassisSummaryDeduplicatesRequests() {
        QuoteRecord firstRequest = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Third Party",
                "Status", "Success",
                "ChassisNumber", "ABC-123",
                "EID", "784-1980-1234567-1"
        ));

        QuoteRecord duplicateFormatting = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Third Party",
                "Status", "Failed",
                "ErrorText", "declined",
                "ChassisNumber", "ABC-123 ",
                "EID", "784198012345671"
        ));

        QuoteRecord missingEid = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Third Party",
                "Status", "Success",
                "ChassisNumber", "NO-EID-1"
        ));

        QuoteRecord differentCombination = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Third Party",
                "Status", "Failed",
                "ErrorText", "declined",
                "ChassisNumber", "XYZ-999",
                "EID", "784198012345672"
        ));

        QuoteRecord comprehensiveRecord = QuoteRecord.fromValues(Map.of(
                "InsuranceType", "Comprehensive",
                "Status", "Success",
                "ChassisNumber", "COMP-DEDUP",
                "EID", "784198012345673"
        ));

        QuoteStatistics statistics = QuoteStatisticsCalculator.calculate(List.of(
                firstRequest,
                duplicateFormatting,
                missingEid,
                differentCombination,
                comprehensiveRecord
        ));

        QuoteStatistics.EidChassisSummary summary = statistics.getTplEidChassisSummary();
        assertEquals(3, summary.getTotalRequests());
        assertEquals(2, summary.getUniqueRequests());
        assertEquals(1, summary.getDuplicateRequests());
    }
}
