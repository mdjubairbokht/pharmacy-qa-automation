package com.rxcare.qa.tests.db;

import com.rxcare.qa.api.PrescriptionWorkflow;
import com.rxcare.qa.api.RxApi;
import com.rxcare.qa.api.models.DailySales;
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
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * ETL validation for the daily sales job (source: pharmacy_transactions, target: daily_sales_summary):
 * completeness, accuracy, no duplicates and idempotent re-runs, comparing source and target with SQL.
 */
@Epic("Data Quality")
@Feature("ETL - Daily Sales Summary")
public class DailySalesEtlTest {

    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);
    private RxApi pharmacist;

    @BeforeClass(alwaysRun = true)
    @Description("Create today's sales through real pickups (two CASH, one CARD)")
    public void createSales() {
        pharmacist = RxApi.asPharmacist();
        PrescriptionWorkflow.pickedUp(pharmacist, SeedData.NDC_LISINOPRIL, 30, "CASH");
        PrescriptionWorkflow.pickedUp(pharmacist, SeedData.NDC_SERTRALINE, 15, "CASH");
        PrescriptionWorkflow.pickedUp(pharmacist, SeedData.NDC_ATORVASTATIN, 60, "CARD");
    }

    @Test(groups = {"db", "etl", "regression"}, description = "Target totals equal source aggregates per payment method")
    @Story("RX-112 Daily sales ETL")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-ETL-001")
    public void etlLoadsAccurateAggregates() {
        List<DailySales> apiRows = List.of(pharmacist.reports().runDailySales(today)
                .then().statusCode(200).extract().as(DailySales[].class));

        Map<String, Totals> source = totals(DbClient.query(PharmacyQueries.ETL_SOURCE_AGGREGATE, today));
        Map<String, Totals> target = totals(DbClient.query(PharmacyQueries.ETL_TARGET_ROWS, today));

        assertTrue(source.containsKey("CASH") && source.containsKey("CARD"), "test sales exist in source: " + source);
        assertEquals(target, source, "target table matches source aggregate (completeness + accuracy)");

        SoftAssert soft = new SoftAssert();
        soft.assertEquals(apiRows.size(), target.size(), "API returns one row per payment method");
        for (DailySales row : apiRows) {
            Totals expected = target.get(row.paymentMethod());
            soft.assertNotNull(expected, "API row exists in target: " + row.paymentMethod());
            if (expected != null) {
                soft.assertEquals(row.transactionCount(), expected.count(), row.paymentMethod() + " count");
                soft.assertEquals(row.totalAmount().compareTo(expected.amount()), 0, row.paymentMethod() + " amount");
            }
        }
        soft.assertAll();
    }

    @Test(groups = {"db", "etl", "regression"}, dependsOnMethods = "etlLoadsAccurateAggregates",
            description = "Re-running the job for the same day neither duplicates nor changes the result")
    @Story("RX-112 Daily sales ETL")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-ETL-002")
    public void etlRerunIsIdempotent() {
        Map<String, Totals> first = totals(DbClient.query(PharmacyQueries.ETL_TARGET_ROWS, today));

        pharmacist.reports().runDailySales(today).then().statusCode(200);

        assertTrue(DbClient.query(PharmacyQueries.ETL_DUPLICATE_KEYS).isEmpty(), "no duplicate (date, method) keys");
        assertEquals(totals(DbClient.query(PharmacyQueries.ETL_TARGET_ROWS, today)), first, "unchanged after re-run");
    }

    @Test(groups = {"db", "etl", "regression"}, dependsOnMethods = "etlRerunIsIdempotent",
            description = "A new sale is picked up incrementally by the next run")
    @Story("RX-112 Daily sales ETL")
    @TmsLink("TC-ETL-003")
    public void etlPicksUpNewSale() {
        Totals before = totals(DbClient.query(PharmacyQueries.ETL_TARGET_ROWS, today)).get("CASH");

        Prescription rx = PrescriptionWorkflow.pickedUp(pharmacist, SeedData.NDC_LISINOPRIL, 90, "CASH");
        BigDecimal saleAmount = DbClient.scalar(BigDecimal.class,
                "SELECT amount FROM pharmacy_transactions WHERE prescription_id = ?", rx.id());
        pharmacist.reports().runDailySales(today).then().statusCode(200);

        Totals after = totals(DbClient.query(PharmacyQueries.ETL_TARGET_ROWS, today)).get("CASH");
        assertEquals(after.count(), before.count() + 1, "one more CASH transaction");
        assertEquals(after.amount().compareTo(before.amount().add(saleAmount)), 0, "CASH total grew by the sale amount");
    }

    @Test(groups = {"db", "etl", "regression"}, description = "A day without sales produces no summary rows")
    @Story("RX-112 Daily sales ETL")
    @TmsLink("TC-ETL-004")
    public void dayWithoutSalesProducesNoRows() {
        LocalDate quietDay = LocalDate.of(2000, 1, 1);
        pharmacist.reports().runDailySales(quietDay).then().statusCode(200);
        assertTrue(DbClient.query(PharmacyQueries.ETL_TARGET_ROWS, quietDay).isEmpty());
    }

    /** Count + amount per payment method; amounts normalised so 10.5 and 10.50 compare equal. */
    record Totals(int count, BigDecimal amount) {
        Totals {
            amount = amount.stripTrailingZeros();
        }
    }

    private static Map<String, Totals> totals(List<Map<String, Object>> rows) {
        Map<String, Totals> map = new TreeMap<>();
        for (Map<String, Object> row : rows) {
            map.put((String) row.get("payment_method"), new Totals(
                    ((Number) row.get("transaction_count")).intValue(),
                    new BigDecimal(row.get("total_amount").toString())));
        }
        return map;
    }
}
