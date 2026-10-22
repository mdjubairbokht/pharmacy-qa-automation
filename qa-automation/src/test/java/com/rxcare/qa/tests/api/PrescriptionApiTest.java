package com.rxcare.qa.tests.api;

import com.rxcare.qa.api.PrescriptionWorkflow;
import com.rxcare.qa.api.models.Drug;
import com.rxcare.qa.api.models.Prescription;
import com.rxcare.qa.api.models.StatusHistory;
import com.rxcare.qa.data.SeedData;
import com.rxcare.qa.data.TestDataFactory;
import com.rxcare.qa.data.TestDataReader;
import com.rxcare.qa.db.DbClient;
import com.rxcare.qa.db.PharmacyQueries;
import com.rxcare.qa.tests.base.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

@Epic("Pharmacy Operations")
@Feature("Prescription Processing")
public class PrescriptionApiTest extends BaseApiTest {

    /** Metformin is reserved for this class so on-hand deltas can be asserted exactly while suites run in parallel. */
    private static final String EXCLUSIVE_NDC = SeedData.NDC_METFORMIN;

    @Test(groups = {"api", "smoke", "regression"},
            description = "Prescription lifecycle NEW -> VERIFIED -> FILLED -> PICKED_UP with inventory, sale and audit trail")
    @Story("RX-104/105/106/107 Prescription lifecycle")
    @Severity(SeverityLevel.BLOCKER)
    @TmsLink("TC-RX-001")
    @Description("Technician enters the prescription, pharmacist verifies, technician fills, patient pays at pickup. "
            + "Each step is validated in the API response and the database.")
    public void prescriptionLifecycleEndToEnd() {
        int patientId = PrescriptionWorkflow.newPatient(technician);
        Drug drugBefore = technician.inventory().drugOk(EXCLUSIVE_NDC);
        int quantity = 60;

        // Intake (technician)
        Prescription rx = technician.prescriptions()
                .create(TestDataFactory.prescription(patientId, EXCLUSIVE_NDC).withQuantity(quantity).withRefills(3))
                .then()
                .statusCode(201)
                .body(matchesJsonSchemaInClasspath("schemas/prescription.json"))
                .body("status", equalTo("NEW"))
                .body("rxNumber", matchesPattern("^RX\\d{7}$"))
                .body("refillsRemaining", equalTo(3))
                .body("createdBy", equalTo("tech.smith"))
                .extract().as(Prescription.class);

        // Verification (pharmacist)
        pharmacist.prescriptions().verify(rx.id())
                .then().statusCode(200)
                .body("status", equalTo("VERIFIED"))
                .body("verifiedBy", equalTo("rph.jones"));

        // Fill (technician) -> inventory decremented exactly once
        technician.prescriptions().fill(rx.id())
                .then().statusCode(200)
                .body("status", equalTo("FILLED"))
                .body("fillCount", equalTo(1))
                .body("filledBy", equalTo("tech.smith"));
        Drug drugAfter = technician.inventory().drugOk(EXCLUSIVE_NDC);
        assertEquals(drugAfter.qtyOnHand(), drugBefore.qtyOnHand() - quantity, "on-hand decreased by quantity");

        List<Map<String, Object>> dispenses = DbClient.query(PharmacyQueries.DISPENSE_TXNS_BY_RX, rx.id());
        assertEquals(dispenses.size(), 1, "one DISPENSE inventory transaction");
        assertEquals(((Number) dispenses.get(0).get("quantity")).intValue(), -quantity, "dispense quantity");

        // Pickup (point of sale)
        technician.prescriptions().pickup(rx.id(), "INSURANCE")
                .then().statusCode(200)
                .body("status", equalTo("PICKED_UP"));
        BigDecimal expectedAmount = drugBefore.unitPrice().multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal saleAmount = DbClient.scalar(BigDecimal.class,
                "SELECT amount FROM pharmacy_transactions WHERE prescription_id = ?", rx.id());
        assertEquals(saleAmount.compareTo(expectedAmount), 0, "sale amount = qty x unit price (" + expectedAmount + ")");

        // Audit trail
        List<StatusHistory> history = List.of(technician.prescriptions().history(rx.id())
                .then().statusCode(200).extract().as(StatusHistory[].class));
        assertEquals(history.stream().map(StatusHistory::toStatus).toList(),
                List.of("NEW", "VERIFIED", "FILLED", "PICKED_UP"), "status history");
        assertEquals(history.stream().map(StatusHistory::changedBy).toList(),
                List.of("tech.smith", "rph.jones", "tech.smith", "tech.smith"), "who changed each status");
    }

    @DataProvider(name = "allergyConflicts")
    public Object[][] allergyConflicts() {
        return new Object[][]{
                {"PENICILLIN", SeedData.NDC_AMOXICILLIN},
                {"SULFONAMIDE", SeedData.NDC_SMX_TMP},
                {"OPIOID", SeedData.NDC_OXYCODONE}
        };
    }

    @Test(groups = {"api", "regression"}, dataProvider = "allergyConflicts",
            description = "Drug utilization review blocks a drug from a class the patient is allergic to")
    @Story("RX-108 Allergy screening (DUR)")
    @Severity(SeverityLevel.BLOCKER)
    @TmsLink("TC-RX-002")
    public void allergyConflictIsBlocked(String allergen, String ndc) {
        int patientId = technician.patients().createOk(TestDataFactory.newPatientWithAllergies(allergen)).id();

        technician.prescriptions()
                .create(TestDataFactory.prescription(patientId, ndc).withRefills(0))
                .then()
                .statusCode(422)
                .body("code", equalTo("ALLERGY_CONFLICT"));

        assertEquals(DbClient.count("SELECT COUNT(*) FROM prescriptions WHERE patient_id = ?", patientId), 0,
                "no prescription persisted");
    }

    @Test(groups = {"api", "regression"},
            description = "Schedule II controlled substances cannot be written with refills")
    @Story("RX-109 Controlled substance rules")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-RX-003")
    public void scheduleTwoRefillsAreRejected() {
        int patientId = PrescriptionWorkflow.newPatient(technician);

        technician.prescriptions()
                .create(TestDataFactory.prescription(patientId, SeedData.NDC_OXYCODONE).withRefills(1))
                .then().statusCode(422)
                .body("code", equalTo("CONTROLLED_SUBSTANCE_REFILLS"));

        technician.prescriptions()
                .create(TestDataFactory.prescription(patientId, SeedData.NDC_OXYCODONE).withRefills(0).withQuantity(20))
                .then().statusCode(201)
                .body("refills", equalTo(0));
    }

    @Test(groups = {"api", "regression"}, description = "Only a pharmacist may verify a prescription")
    @Story("RX-105 Pharmacist verification")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-RX-004")
    public void technicianCannotVerify() {
        Prescription rx = PrescriptionWorkflow.created(technician, SeedData.NDC_LISINOPRIL, 30);

        technician.prescriptions().verify(rx.id())
                .then().statusCode(403)
                .body("code", equalTo("PHARMACIST_ONLY"));

        assertEquals(DbClient.queryOne(PharmacyQueries.PRESCRIPTION_BY_ID, rx.id()).get("status"), "NEW");
    }

    @Test(groups = {"api", "regression"},
            description = "Out-of-order workflow actions are rejected with 409 and leave the prescription unchanged")
    @Story("RX-104 Prescription workflow")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-RX-005")
    public void invalidTransitionsAreRejected() {
        Prescription fresh = PrescriptionWorkflow.created(technician, SeedData.NDC_LISINOPRIL, 30);
        technician.prescriptions().fill(fresh.id()).then().statusCode(409)
                .body("code", equalTo("INVALID_STATUS_TRANSITION"));
        technician.prescriptions().pickup(fresh.id(), "CASH").then().statusCode(409);
        technician.prescriptions().refill(fresh.id()).then().statusCode(409);

        Prescription filled = PrescriptionWorkflow.filled(pharmacist, SeedData.NDC_LISINOPRIL, 30);
        pharmacist.prescriptions().verify(filled.id()).then().statusCode(409);
        pharmacist.prescriptions().cancel(filled.id(), "Patient declined").then().statusCode(409);

        assertTrue(DbClient.query(PharmacyQueries.DISPENSE_TXNS_BY_RX, fresh.id()).isEmpty(),
                "nothing dispensed for the NEW prescription");
    }

    @Test(groups = {"api", "regression"},
            description = "Filling more than is on hand fails and does not touch inventory")
    @Story("RX-106 Dispensing & inventory")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-RX-006")
    public void fillWithInsufficientInventoryFails() {
        Prescription rx = PrescriptionWorkflow.verified(pharmacist, SeedData.NDC_ALBUTEROL, 50);
        int onHandBefore = DbClient.scalar(Integer.class, PharmacyQueries.DRUG_QTY_ON_HAND, SeedData.NDC_ALBUTEROL);

        pharmacist.prescriptions().fill(rx.id())
                .then().statusCode(422)
                .body("code", equalTo("INSUFFICIENT_INVENTORY"));

        assertEquals(pharmacist.prescriptions().getOk(rx.id()).status(), "VERIFIED");
        assertEquals(DbClient.scalar(Integer.class, PharmacyQueries.DRUG_QTY_ON_HAND, SeedData.NDC_ALBUTEROL),
                Integer.valueOf(onHandBefore), "on-hand unchanged");
        assertTrue(DbClient.query(PharmacyQueries.DISPENSE_TXNS_BY_RX, rx.id()).isEmpty(), "no DISPENSE row");
    }

    @Test(groups = {"api", "regression"}, description = "Cancelling records the reason and blocks further processing")
    @Story("RX-104 Prescription workflow")
    @TmsLink("TC-RX-007")
    public void cancelPrescription() {
        Prescription rx = PrescriptionWorkflow.created(technician, SeedData.NDC_LISINOPRIL, 30);

        technician.prescriptions().cancel(rx.id(), "")
                .then().statusCode(400).body("details", hasItem(startsWith("reason:")));

        technician.prescriptions().cancel(rx.id(), "Prescriber called to discontinue")
                .then().statusCode(200)
                .body("status", equalTo("CANCELLED"))
                .body("cancelReason", equalTo("Prescriber called to discontinue"));

        pharmacist.prescriptions().verify(rx.id()).then().statusCode(409);
    }

    @Test(groups = {"api", "regression"}, description = "Refill re-queues a picked-up prescription and decrements refills")
    @Story("RX-110 Refills")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-RX-008")
    public void refillDecrementsRemainingRefills() {
        Prescription rx = PrescriptionWorkflow.pickedUp(pharmacist, SeedData.NDC_LISINOPRIL, 30, "CARD");
        assertEquals(rx.refillsRemaining(), 2);

        technician.prescriptions().refill(rx.id())
                .then().statusCode(200)
                .body("status", equalTo("NEW"))
                .body("refillsRemaining", equalTo(1))
                .body("fillCount", equalTo(1));

        Map<String, Object> row = DbClient.queryOne(PharmacyQueries.PRESCRIPTION_BY_ID, rx.id());
        assertNull(row.get("verified_by"), "refill requires a new pharmacist verification");
    }

    @Test(groups = {"api", "regression"}, description = "No refill is possible when none remain")
    @Story("RX-110 Refills")
    @TmsLink("TC-RX-009")
    public void refillWithNoRefillsRemainingFails() {
        int patientId = PrescriptionWorkflow.newPatient(technician);
        Prescription rx = technician.prescriptions()
                .createOk(TestDataFactory.prescription(patientId, SeedData.NDC_LISINOPRIL).withRefills(0));
        pharmacist.prescriptions().verify(rx.id()).then().statusCode(200);
        pharmacist.prescriptions().fill(rx.id()).then().statusCode(200);
        pharmacist.prescriptions().pickup(rx.id(), "CASH").then().statusCode(200);

        technician.prescriptions().refill(rx.id())
                .then().statusCode(422)
                .body("code", equalTo("NO_REFILLS_REMAINING"));
    }

    @DataProvider(name = "invalidPrescriptions")
    public Object[][] invalidPrescriptions() {
        return TestDataReader.csv("testdata/invalid-prescriptions.csv");
    }

    @Test(groups = {"api", "regression"}, dataProvider = "invalidPrescriptions",
            description = "Prescription field validation (boundary values for quantity, days supply, refills, NDC)")
    @Story("RX-104 Prescription intake")
    @TmsLink("TC-RX-010")
    public void invalidPrescriptionIsRejected(String scenario, String field, String value) {
        Object typedValue = value.isEmpty() ? null : (value.matches("-?\\d+") ? Integer.valueOf(value) : value);
        Map<String, Object> payload = TestDataFactory.withOverrides(
                TestDataFactory.prescription(SeedData.PATIENT_JOHN_CARTER, SeedData.NDC_LISINOPRIL),
                Collections.singletonMap(field, typedValue));

        technician.prescriptions().create(payload)
                .then().statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("details", hasItem(startsWith(field + ":")));
    }

    @Test(groups = {"api", "regression"}, description = "Unknown patient or drug returns 404")
    @Story("RX-104 Prescription intake")
    @TmsLink("TC-RX-011")
    public void unknownPatientOrDrugReturns404() {
        technician.prescriptions()
                .create(TestDataFactory.prescription(SeedData.PATIENT_UNKNOWN, SeedData.NDC_LISINOPRIL))
                .then().statusCode(404).body("code", equalTo("PATIENT_NOT_FOUND"));
        technician.prescriptions()
                .create(TestDataFactory.prescription(SeedData.PATIENT_JOHN_CARTER, SeedData.NDC_UNKNOWN))
                .then().statusCode(404).body("code", equalTo("DRUG_NOT_FOUND"));
    }

    @Test(groups = {"api", "regression"}, description = "Work queue filter returns only the requested status")
    @Story("RX-104 Prescription workflow")
    @TmsLink("TC-RX-012")
    public void filterByStatus() {
        PrescriptionWorkflow.verified(pharmacist, SeedData.NDC_LISINOPRIL, 30);

        Response response = technician.prescriptions().listByStatus("verified");
        response.then().statusCode(200).body("status", everyItem(equalTo("VERIFIED")));
        assertTrue(response.jsonPath().getList("id").size() >= 1, "at least one VERIFIED prescription");

        technician.prescriptions().listByStatus("SHIPPED")
                .then().statusCode(400).body("code", equalTo("INVALID_STATUS"));
    }
}
