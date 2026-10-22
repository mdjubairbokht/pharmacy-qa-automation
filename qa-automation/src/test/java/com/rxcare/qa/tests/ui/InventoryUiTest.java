package com.rxcare.qa.tests.ui;

import com.rxcare.qa.data.SeedData;
import com.rxcare.qa.db.DbClient;
import com.rxcare.qa.db.PharmacyQueries;
import com.rxcare.qa.pages.DashboardPage;
import com.rxcare.qa.pages.InventoryPage;
import com.rxcare.qa.tests.base.BaseUiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Epic("Pharmacy Operations")
@Feature("Inventory Management")
public class InventoryUiTest extends BaseUiTest {

    @Test(groups = {"ui", "regression"}, description = "Low-stock filter shows only flagged items")
    @Story("RX-111 Inventory")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-UI-INV-001")
    public void lowStockFilter() {
        InventoryPage inventory = loginAsTechnician().goToInventory().showLowStockOnly(true);

        List<String> shown = inventory.displayedNdcs();
        assertFalse(shown.isEmpty(), "at least one low-stock item");
        assertTrue(shown.contains(SeedData.NDC_ALBUTEROL), "Albuterol is below its reorder level");
        shown.forEach(ndc -> assertTrue(inventory.isFlaggedLowStock(ndc), ndc + " flagged"));
        assertEquals(shown.size(), DbClient.count("SELECT COUNT(*) FROM drugs WHERE qty_on_hand <= reorder_level"),
                "UI matches database");
    }

    @Test(groups = {"ui", "regression"}, description = "Receiving stock updates the on-hand quantity in UI and database")
    @Story("RX-111 Inventory")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-UI-INV-002")
    public void receiveStock() {
        InventoryPage inventory = loginAsPharmacist().goToInventory();
        int before = inventory.qtyOnHand(SeedData.NDC_OMEPRAZOLE);
        String reference = "PO-UI-" + System.currentTimeMillis();

        inventory.receive(SeedData.NDC_OMEPRAZOLE, 120, reference);

        int after = inventory.qtyOnHand(SeedData.NDC_OMEPRAZOLE);
        assertEquals(after, before + 120, "UI on-hand");
        assertEquals(DbClient.scalar(Integer.class, PharmacyQueries.DRUG_QTY_ON_HAND, SeedData.NDC_OMEPRAZOLE),
                Integer.valueOf(after), "database on-hand");
        Map<String, Object> ledger = DbClient.queryOne(
                "SELECT txn_type, quantity, performed_by FROM inventory_transactions WHERE reason = ?", reference);
        assertEquals(ledger.get("txn_type"), "RECEIVE");
        assertEquals(((Number) ledger.get("quantity")).intValue(), 120);
        assertEquals(ledger.get("performed_by"), "rph.jones");
    }

    @Test(groups = {"ui", "regression"}, description = "Dashboard low-stock card matches the database")
    @Story("RX-111 Inventory")
    @TmsLink("TC-UI-INV-003")
    public void dashboardLowStockCount() {
        DashboardPage dashboard = loginAsTechnician();
        assertEquals(dashboard.statAsInt("low-stock"),
                DbClient.count("SELECT COUNT(*) FROM drugs WHERE qty_on_hand <= reorder_level"));
    }
}
