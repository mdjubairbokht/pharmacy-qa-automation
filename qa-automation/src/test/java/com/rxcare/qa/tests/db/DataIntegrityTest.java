package com.rxcare.qa.tests.db;

import com.rxcare.qa.api.PrescriptionWorkflow;
import com.rxcare.qa.api.RxApi;
import com.rxcare.qa.api.models.Prescription;
import com.rxcare.qa.data.SeedData;
import com.rxcare.qa.db.DbClient;
import com.rxcare.qa.db.PharmacyQueries;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertTrue;

/**
 * Backend data-integrity checks. Each check is a SQL query that returns the rows breaking a
 * business rule, so the expected result is always "no rows" and a failure lists the offenders.
 */
@Epic("Data Quality")
@Feature("Database Integrity")
public class DataIntegrityTest {

    @BeforeClass(alwaysRun = true)
    @Description("Generate real business activity through the API so the checks validate more than seed data")
    public void generateActivity() {
        RxApi pharmacist = RxApi.asPharmacist();
        Prescription refilled = PrescriptionWorkflow.pickedUp(pharmacist, SeedData.NDC_LISINOPRIL, 30, "CASH");
        pharmacist.prescriptions().refill(refilled.id()).then().statusCode(200);
        PrescriptionWorkflow.pickedUp(pharmacist, SeedData.NDC_SERTRALINE, 15, "INSURANCE");
        PrescriptionWorkflow.filled(pharmacist, SeedData.NDC_ATORVASTATIN, 30);
        Prescription cancelled = PrescriptionWorkflow.verified(pharmacist, SeedData.NDC_LISINOPRIL, 10);
        pharmacist.prescriptions().cancel(cancelled.id(), "Duplicate therapy").then().statusCode(200);
    }

    @DataProvider(name = "integrityRules")
    public Object[][] integrityRules() {
        return new Object[][]{
                {"Every prescription references an existing patient and drug", PharmacyQueries.ORPHAN_PRESCRIPTIONS},
                {"Every allergy belongs to an existing patient", PharmacyQueries.ORPHAN_ALLERGIES},
                {"On-hand quantity reconciles to the inventory ledger", PharmacyQueries.INVENTORY_RECONCILIATION_BREAKS},
                {"Latest audit row matches the current prescription status", PharmacyQueries.STATUS_HISTORY_MISMATCHES},
                {"Every prescription has an audit trail", PharmacyQueries.PRESCRIPTIONS_WITHOUT_HISTORY},
                {"One DISPENSE ledger row per fill", PharmacyQueries.FILL_COUNT_VS_DISPENSE_MISMATCHES},
                {"One sale per completed pickup", PharmacyQueries.SALES_VS_PICKUP_MISMATCHES},
                {"Sale amount equals quantity x unit price", PharmacyQueries.SALE_AMOUNT_MISMATCHES},
                {"Schedule II prescriptions never carry refills", PharmacyQueries.CONTROLLED_SUBSTANCE_REFILL_VIOLATIONS},
                {"No active prescription conflicts with a documented allergy", PharmacyQueries.ALLERGY_CONFLICTS},
                {"Workflow audit columns are populated for each status", PharmacyQueries.WORKFLOW_FIELD_GAPS},
                {"Passwords are stored as SHA-256 hashes, never plain text", PharmacyQueries.PLAIN_TEXT_PASSWORDS},
                {"Rx numbers are unique", PharmacyQueries.DUPLICATE_RX_NUMBERS}
        };
    }

    @Test(groups = {"db", "regression"}, dataProvider = "integrityRules",
            description = "Business-rule integrity check returns no violating rows")
    @Story("RX-113 Audit trail & data integrity")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-DB-001")
    public void integrityRuleHolds(String rule, String violationQuery) {
        List<Map<String, Object>> violations = DbClient.query(violationQuery);
        assertTrue(violations.isEmpty(), rule + " - violating rows: " + violations);
    }

    @Test(groups = {"db", "smoke", "regression"}, description = "Seed reference data is present")
    @Story("Release readiness")
    @TmsLink("TC-DB-002")
    public void seedDataIsLoaded() {
        assertTrue(DbClient.count("SELECT COUNT(*) FROM users WHERE active") >= 2, "active users");
        assertTrue(DbClient.count("SELECT COUNT(*) FROM drugs") >= 9, "drug catalog");
        assertTrue(DbClient.count("SELECT COUNT(*) FROM drugs WHERE schedule = 2") >= 1, "controlled substances");
    }
}
