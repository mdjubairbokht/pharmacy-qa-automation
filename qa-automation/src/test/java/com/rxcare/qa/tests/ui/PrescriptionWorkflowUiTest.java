package com.rxcare.qa.tests.ui;

import com.rxcare.qa.api.PrescriptionWorkflow;
import com.rxcare.qa.api.models.Drug;
import com.rxcare.qa.api.models.Prescription;
import com.rxcare.qa.data.SeedData;
import com.rxcare.qa.data.TestDataFactory;
import com.rxcare.qa.db.DbClient;
import com.rxcare.qa.db.PharmacyQueries;
import com.rxcare.qa.pages.PrescriptionsPage;
import com.rxcare.qa.pages.components.NewPrescriptionDialog;
import com.rxcare.qa.pages.components.PickupDialog;
import com.rxcare.qa.tests.base.BaseUiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Epic("Pharmacy Operations")
@Feature("Prescription Processing")
public class PrescriptionWorkflowUiTest extends BaseUiTest {

    @Test(groups = {"ui", "e2e", "regression"},
            description = "E2E: pharmacist creates, verifies, fills and sells a prescription; UI, API and DB agree")
    @Story("RX-104/105/106/107 Prescription lifecycle")
    @Severity(SeverityLevel.BLOCKER)
    @TmsLink("TC-UI-RX-001")
    @Description("Arrange the patient through the API, drive the whole workflow through the UI, "
            + "then validate inventory, sale amount and audit trail in the database.")
    public void prescriptionLifecycleThroughUi() {
        int patientId = PrescriptionWorkflow.newPatient(api());
        Drug drug = api().inventory().drugOk(SeedData.NDC_ATORVASTATIN);
        int quantity = 45;

        PrescriptionsPage queue = loginAsPharmacist().goToPrescriptions();
        String rxNumber = queue.openNewPrescription()
                .fill(TestDataFactory.prescription(patientId, SeedData.NDC_ATORVASTATIN).withQuantity(quantity))
                .create();
        assertEquals(queue.cell(rxNumber, "qty"), String.valueOf(quantity));

        queue.verify(rxNumber).fill(rxNumber);

        PickupDialog pickup = queue.openPickup(rxNumber);
        BigDecimal expected = drug.unitPrice().multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        assertEquals(pickup.amountDue().compareTo(expected), 0, "amount due shown at the register");
        pickup.completeSale("CARD");
        assertFalse(queue.isActionAvailable(rxNumber, "pickup"), "no further pickup action");

        // Backend validation
        Map<String, Object> rx = DbClient.queryOne(PharmacyQueries.PRESCRIPTION_BY_RX_NUMBER, rxNumber);
        assertEquals(rx.get("status"), "PICKED_UP");
        assertEquals(rx.get("verified_by"), "rph.jones");
        int rxId = ((Number) rx.get("id")).intValue();

        List<Map<String, Object>> sales = DbClient.query(PharmacyQueries.SALES_BY_RX, rxId);
        assertEquals(sales.size(), 1, "one sale");
        assertEquals(new BigDecimal(sales.get(0).get("amount").toString()).compareTo(expected), 0, "sale amount");
        assertEquals(sales.get(0).get("payment_method"), "CARD");

        List<Object> history = DbClient.query(PharmacyQueries.STATUS_HISTORY_BY_RX, rxId)
                .stream().map(r -> r.get("to_status")).toList();
        assertEquals(history, List.of("NEW", "VERIFIED", "FILLED", "PICKED_UP"), "audit trail");
    }

    @Test(groups = {"ui", "regression"}, description = "Technicians do not get the Verify action")
    @Story("RX-105 Pharmacist verification")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-UI-RX-002")
    public void technicianCannotVerifyInUi() {
        Prescription rx = PrescriptionWorkflow.created(api(), SeedData.NDC_LISINOPRIL, 30);

        PrescriptionsPage queue = loginAsTechnician().goToPrescriptions().filterByStatus("NEW");

        assertTrue(queue.hasRow(rx.rxNumber()), "prescription listed");
        assertFalse(queue.isActionAvailable(rx.rxNumber(), "verify"), "verify hidden for technicians");
        assertTrue(queue.isActionAvailable(rx.rxNumber(), "cancel"), "cancel still available");
    }

    @Test(groups = {"ui", "regression"}, description = "Allergy conflict is reported in the new prescription form")
    @Story("RX-108 Allergy screening (DUR)")
    @Severity(SeverityLevel.BLOCKER)
    @TmsLink("TC-UI-RX-003")
    public void allergyConflictShownInForm() {
        int patientId = api().patients().createOk(TestDataFactory.newPatientWithAllergies("PENICILLIN")).id();

        NewPrescriptionDialog dialog = loginAsTechnician().goToPrescriptions()
                .openNewPrescription()
                .fill(TestDataFactory.prescription(patientId, SeedData.NDC_AMOXICILLIN).withRefills(0))
                .createExpectingError();

        assertTrue(dialog.errorMessage().contains("PENICILLIN allergy"), dialog.errorMessage());
        assertEquals(DbClient.count("SELECT COUNT(*) FROM prescriptions WHERE patient_id = ?", patientId), 0);
    }

    @Test(groups = {"ui", "regression"}, description = "Filling beyond stock shows an error and keeps the prescription VERIFIED")
    @Story("RX-106 Dispensing & inventory")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-UI-RX-004")
    public void insufficientStockShowsError() {
        Prescription rx = PrescriptionWorkflow.verified(api(), SeedData.NDC_ALBUTEROL, 40);

        PrescriptionsPage queue = loginAsPharmacist().goToPrescriptions().filterByStatus("VERIFIED");
        String message = queue.clickActionAndReadToast(rx.rxNumber(), "fill");

        assertTrue(message.contains("Insufficient inventory"), message);
        assertTrue(queue.isToastError(), "shown as an error");
        assertEquals(queue.statusOf(rx.rxNumber()), "VERIFIED");
    }

    @Test(groups = {"ui", "regression"}, description = "Cancelling a prescription stores the reason")
    @Story("RX-104 Prescription workflow")
    @TmsLink("TC-UI-RX-005")
    public void cancelPrescriptionThroughUi() {
        Prescription rx = PrescriptionWorkflow.created(api(), SeedData.NDC_LISINOPRIL, 30);

        PrescriptionsPage queue = loginAsTechnician().goToPrescriptions()
                .filterByStatus("NEW")
                .cancel(rx.rxNumber(), "Patient requested cancellation");

        assertFalse(queue.hasRow(rx.rxNumber()), "cancelled prescription leaves the NEW queue");
        queue.filterByStatus("CANCELLED");
        assertEquals(queue.statusOf(rx.rxNumber()), "CANCELLED", "shown under the CANCELLED filter");
        assertFalse(queue.isActionAvailable(rx.rxNumber(), "cancel"), "no further actions on a cancelled prescription");
        assertEquals(DbClient.queryOne(PharmacyQueries.PRESCRIPTION_BY_ID, rx.id()).get("cancel_reason"),
                "Patient requested cancellation");
    }

    @Test(groups = {"ui", "regression"}, description = "Status filter shows only matching prescriptions")
    @Story("RX-104 Prescription workflow")
    @TmsLink("TC-UI-RX-006")
    public void statusFilterWorks() {
        Prescription rx = PrescriptionWorkflow.filled(api(), SeedData.NDC_LISINOPRIL, 30);

        PrescriptionsPage queue = loginAsTechnician().goToPrescriptions().filterByStatus("FILLED");

        assertTrue(queue.hasRow(rx.rxNumber()), "filled prescription listed");
        assertTrue(queue.statusesShown().stream().allMatch("FILLED"::equals), "only FILLED: " + queue.statusesShown());
    }
}
