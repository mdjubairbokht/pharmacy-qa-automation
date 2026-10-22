package com.rxcare.qa.tests.api;

import com.rxcare.qa.api.models.Drug;
import com.rxcare.qa.api.models.InventoryAdjustment;
import com.rxcare.qa.data.SeedData;
import com.rxcare.qa.db.DbClient;
import com.rxcare.qa.db.PharmacyQueries;
import com.rxcare.qa.tests.base.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.testng.Assert.assertEquals;

@Epic("Pharmacy Operations")
@Feature("Inventory Management")
public class InventoryApiTest extends BaseApiTest {

    /** Omeprazole is reserved for this class in the API suite so quantity deltas are exact. */
    private static final String EXCLUSIVE_NDC = SeedData.NDC_OMEPRAZOLE;

    @Test(groups = {"api", "smoke", "regression"}, description = "Drug catalog returns well-formed NDCs and schedules")
    @Story("RX-111 Inventory")
    @TmsLink("TC-INV-001")
    public void drugCatalogIsWellFormed() {
        technician.inventory().drugs()
                .then().statusCode(200)
                .body("ndc", everyItem(matchesPattern("^\\d{5}-\\d{4}-\\d{2}$")))
                .body("ndc", hasItem(SeedData.NDC_OXYCODONE))
                .body("find { it.ndc == '" + SeedData.NDC_OXYCODONE + "' }.schedule", equalTo(2));
    }

    @Test(groups = {"api", "regression"}, description = "Low-stock report lists every item at or below its reorder level")
    @Story("RX-111 Inventory")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-INV-002")
    public void lowStockReport() {
        technician.inventory().lowStock()
                .then().statusCode(200)
                .body("ndc", hasItem(SeedData.NDC_ALBUTEROL))
                .body("lowStock", everyItem(equalTo(true)));

        int dbLowStock = DbClient.count("SELECT COUNT(*) FROM drugs WHERE qty_on_hand <= reorder_level");
        assertEquals(technician.inventory().lowStock().jsonPath().getList("ndc").size(), dbLowStock,
                "API and database agree on low-stock items");
    }

    @Test(groups = {"api", "regression"}, description = "Receiving stock increases on-hand and writes a RECEIVE ledger row")
    @Story("RX-111 Inventory")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-INV-003")
    public void receiveStockIncreasesOnHand() {
        Drug before = pharmacist.inventory().drugOk(EXCLUSIVE_NDC);

        pharmacist.inventory().adjust(EXCLUSIVE_NDC, InventoryAdjustment.receive(250, "PO-API-1001"))
                .then().statusCode(201)
                .body("qtyOnHand", equalTo(before.qtyOnHand() + 250));

        List<Map<String, Object>> ledger = pharmacist.inventory().transactions(EXCLUSIVE_NDC)
                .then().statusCode(200).extract().jsonPath().getList("$");
        Map<String, Object> last = ledger.get(ledger.size() - 1);
        assertEquals(last.get("txnType"), "RECEIVE");
        assertEquals(last.get("quantity"), 250);
        assertEquals(last.get("reason"), "PO-API-1001");
        assertEquals(last.get("performedBy"), "rph.jones");

        assertEquals(DbClient.scalar(Integer.class, PharmacyQueries.DRUG_QTY_ON_HAND, EXCLUSIVE_NDC),
                Integer.valueOf(before.qtyOnHand() + 250), "database on-hand");
    }

    @Test(groups = {"api", "regression"}, description = "Zero or negative receipts are rejected")
    @Story("RX-111 Inventory")
    @TmsLink("TC-INV-004")
    public void invalidReceiptsAreRejected() {
        pharmacist.inventory().adjust(EXCLUSIVE_NDC, InventoryAdjustment.receive(-10, "bad"))
                .then().statusCode(422).body("code", equalTo("INVALID_QUANTITY"));
        pharmacist.inventory().adjust(EXCLUSIVE_NDC, InventoryAdjustment.receive(0, "bad"))
                .then().statusCode(422).body("code", equalTo("INVALID_QUANTITY"));
        pharmacist.inventory().adjust(EXCLUSIVE_NDC, new InventoryAdjustment(5, "STEAL", "bad"))
                .then().statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test(groups = {"api", "regression"}, description = "An adjustment can never drive on-hand below zero")
    @Story("RX-111 Inventory")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-INV-005")
    public void adjustmentCannotMakeInventoryNegative() {
        int before = pharmacist.inventory().drugOk(SeedData.NDC_ALBUTEROL).qtyOnHand();

        pharmacist.inventory().adjust(SeedData.NDC_ALBUTEROL, InventoryAdjustment.adjust(-(before + 1), "Cycle count"))
                .then().statusCode(422).body("code", equalTo("NEGATIVE_INVENTORY"));

        assertEquals(pharmacist.inventory().drugOk(SeedData.NDC_ALBUTEROL).qtyOnHand(), before, "on-hand unchanged");
    }

    @Test(groups = {"api", "regression"}, description = "Unknown NDC returns 404")
    @Story("RX-111 Inventory")
    @TmsLink("TC-INV-006")
    public void unknownDrugReturns404() {
        technician.inventory().drug(SeedData.NDC_UNKNOWN)
                .then().statusCode(404).body("code", equalTo("DRUG_NOT_FOUND"));
    }
}
