package com.rxcare.qa.tests.api;

import com.rxcare.qa.api.models.Patient;
import com.rxcare.qa.api.models.PatientRequest;
import com.rxcare.qa.data.TestDataFactory;
import com.rxcare.qa.data.TestDataReader;
import com.rxcare.qa.db.DbClient;
import com.rxcare.qa.db.PharmacyQueries;
import com.rxcare.qa.tests.base.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.sql.Date;
import java.util.List;
import java.util.Map;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.testng.Assert.assertEquals;

@Epic("Pharmacy Operations")
@Feature("Patient Management")
public class PatientApiTest extends BaseApiTest {

    @Test(groups = {"api", "smoke", "regression"},
            description = "Register a patient with allergies; API response and database rows match the request")
    @Story("RX-102 Patient registration")
    @Severity(SeverityLevel.BLOCKER)
    @TmsLink("TC-PAT-001")
    public void registerPatientPersistsAllFields() {
        PatientRequest request = TestDataFactory.newPatientWithAllergies("penicillin", " Sulfonamide ");

        Patient created = technician.patients().create(request)
                .then()
                .statusCode(201)
                .body(matchesJsonSchemaInClasspath("schemas/patient.json"))
                .extract().as(Patient.class);

        SoftAssert soft = new SoftAssert();
        soft.assertEquals(created.firstName(), request.firstName(), "firstName");
        soft.assertEquals(created.lastName(), request.lastName(), "lastName");
        soft.assertEquals(created.dateOfBirth(), request.dateOfBirth(), "dateOfBirth");
        soft.assertEquals(created.phone(), request.phone(), "phone");
        // Allergies are trimmed, upper-cased and sorted by the application.
        soft.assertEquals(created.allergies(), List.of("PENICILLIN", "SULFONAMIDE"), "normalised allergies");

        // Backend validation: the row and its child rows exist with the same values.
        Map<String, Object> row = DbClient.queryOne(PharmacyQueries.PATIENT_BY_ID, created.id());
        soft.assertEquals(row.get("last_name"), request.lastName(), "DB last_name");
        soft.assertEquals(String.valueOf(row.get("date_of_birth")), request.dateOfBirth(), "DB date_of_birth");
        soft.assertEquals(row.get("phone"), request.phone(), "DB phone");
        soft.assertEquals(row.get("insurance_plan"), request.insurancePlan(), "DB insurance_plan");
        List<Object> dbAllergies = DbClient.query(PharmacyQueries.ALLERGIES_BY_PATIENT, created.id())
                .stream().map(r -> r.get("allergen")).toList();
        soft.assertEquals(dbAllergies, List.of("PENICILLIN", "SULFONAMIDE"), "DB allergies");
        soft.assertAll();
    }

    @Test(groups = {"api", "regression"}, description = "Same name + date of birth is rejected as a duplicate")
    @Story("RX-102 Patient registration")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-PAT-002")
    public void duplicatePatientIsRejected() {
        PatientRequest request = TestDataFactory.newPatient();
        technician.patients().create(request).then().statusCode(201);

        technician.patients().create(request.withPhone("412-555-9999"))
                .then()
                .statusCode(409)
                .body("code", equalTo("DUPLICATE_PATIENT"));

        assertEquals(DbClient.count("SELECT COUNT(*) FROM patients WHERE first_name = ? AND last_name = ?",
                request.firstName(), request.lastName()), 1, "exactly one patient row");
    }

    @DataProvider(name = "invalidPatients")
    public Object[][] invalidPatients() {
        return TestDataReader.jsonCases("testdata/invalid-patients.json");
    }

    @Test(groups = {"api", "regression"}, dataProvider = "invalidPatients",
            description = "Field-level validation rejects bad patient data and nothing is written to the database")
    @Story("RX-102 Patient registration")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("TC-PAT-003")
    public void invalidPatientIsRejected(Map<String, Object> testCase) {
        PatientRequest valid = TestDataFactory.newPatient();
        String field = (String) testCase.get("field");
        Map<String, Object> payload = TestDataFactory.withOverrides(valid,
                java.util.Collections.singletonMap(field, testCase.get("value")));

        technician.patients().create(payload)
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("details", hasItem(startsWith(field + ":")));

        assertEquals(DbClient.count("SELECT COUNT(*) FROM patients WHERE phone = ? AND date_of_birth = ?",
                valid.phone(), Date.valueOf(valid.dateOfBirth())), 0,
                "no row written for case: " + testCase.get("case"));
    }

    @Test(groups = {"api", "regression"}, description = "Unknown patient id returns 404 with the standard error body")
    @Story("RX-103 Patient lookup")
    @TmsLink("TC-PAT-004")
    public void unknownPatientReturns404() {
        technician.patients().get(999_999)
                .then()
                .statusCode(404)
                .body(matchesJsonSchemaInClasspath("schemas/error.json"))
                .body("code", equalTo("PATIENT_NOT_FOUND"))
                .body("path", equalTo("/api/patients/999999"));
    }

    @Test(groups = {"api", "regression"}, description = "Search by last name finds the patient (case-insensitive)")
    @Story("RX-103 Patient lookup")
    @TmsLink("TC-PAT-005")
    public void searchByLastName() {
        Patient created = technician.patients().createOk(TestDataFactory.newPatient());

        technician.patients().search(created.lastName().toUpperCase())
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", equalTo(created.id()));
    }

    @Test(groups = {"api", "regression"}, description = "Updating a patient replaces phone and allergy list in the database")
    @Story("RX-102 Patient registration")
    @TmsLink("TC-PAT-006")
    public void updatePatientReplacesAllergies() {
        Patient created = technician.patients().createOk(TestDataFactory.newPatientWithAllergies("PENICILLIN"));
        PatientRequest update = new PatientRequest(created.firstName(), created.lastName(), created.dateOfBirth(),
                created.gender(), "724-555-0199", created.email(), created.address(), created.insurancePlan(),
                List.of("OPIOID"));

        technician.patients().update(created.id(), update)
                .then()
                .statusCode(200)
                .body("phone", equalTo("724-555-0199"))
                .body("allergies", equalTo(List.of("OPIOID")));

        assertEquals(DbClient.scalar(String.class, "SELECT phone FROM patients WHERE id = ?", created.id()),
                "724-555-0199");
        assertEquals(DbClient.query(PharmacyQueries.ALLERGIES_BY_PATIENT, created.id()).size(), 1,
                "old allergy rows removed");
    }

    @Test(groups = {"api", "regression"}, description = "Non-numeric id in the path returns 400 INVALID_PARAMETER")
    @Story("RX-103 Patient lookup")
    @TmsLink("TC-PAT-007")
    public void nonNumericIdReturns400() {
        com.rxcare.qa.api.ApiSpecs.request(technician.token()).get("/api/patients/abc")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_PARAMETER"));
    }
}
