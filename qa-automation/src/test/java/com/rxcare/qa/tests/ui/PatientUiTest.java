package com.rxcare.qa.tests.ui;

import com.rxcare.qa.api.models.PatientRequest;
import com.rxcare.qa.data.TestDataFactory;
import com.rxcare.qa.db.DbClient;
import com.rxcare.qa.pages.PatientsPage;
import com.rxcare.qa.pages.components.PatientDialog;
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
import static org.testng.Assert.assertTrue;

@Epic("Pharmacy Operations")
@Feature("Patient Management")
public class PatientUiTest extends BaseUiTest {

    @Test(groups = {"ui", "smoke", "regression"},
            description = "Register a patient through the UI; it is searchable and stored correctly")
    @Story("RX-102 Patient registration")
    @Severity(SeverityLevel.BLOCKER)
    @TmsLink("TC-UI-PAT-001")
    public void registerPatientThroughUi() {
        PatientRequest patient = TestDataFactory.newPatientWithAllergies("Penicillin", "Sulfonamide");

        PatientsPage patients = loginAsTechnician().goToPatients()
                .openAddPatient()
                .fill(patient)
                .save();
        assertTrue(patients.waitForToastContaining("added").contains(patient.lastName()), "confirmation toast");

        patients.search(patient.lastName());
        assertEquals(patients.displayedNames(), List.of(patient.displayName()), "search result");
        assertEquals(patients.allergiesFor(patient.displayName()), List.of("PENICILLIN", "SULFONAMIDE"));

        Map<String, Object> row = DbClient.queryOne(
                "SELECT phone, insurance_plan FROM patients WHERE first_name = ? AND last_name = ?",
                patient.firstName(), patient.lastName());
        assertEquals(row.get("phone"), patient.phone(), "phone stored");
        assertEquals(row.get("insurance_plan"), patient.insurancePlan(), "insurance stored");
    }

    @Test(groups = {"ui", "regression"}, description = "Invalid input shows field-level errors and saves nothing")
    @Story("RX-102 Patient registration")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-UI-PAT-002")
    public void validationErrorsAreShown() {
        PatientRequest invalid = TestDataFactory.newPatient().withPhone("4125550100").withFirstName("J0hn");

        PatientDialog dialog = loginAsTechnician().goToPatients()
                .openAddPatient()
                .fill(invalid)
                .saveExpectingError();

        assertTrue(dialog.errorMessage().contains("Request validation failed"), dialog.errorMessage());
        List<String> details = dialog.errorDetails();
        assertTrue(details.stream().anyMatch(d -> d.startsWith("phone:")), "phone error in " + details);
        assertTrue(details.stream().anyMatch(d -> d.startsWith("firstName:")), "firstName error in " + details);
        assertEquals(DbClient.count("SELECT COUNT(*) FROM patients WHERE last_name = ?", invalid.lastName()), 0);
    }

    @Test(groups = {"ui", "regression"}, description = "Search with no match shows the empty state")
    @Story("RX-103 Patient lookup")
    @TmsLink("TC-UI-PAT-003")
    public void searchWithoutMatchShowsEmptyState() {
        PatientsPage patients = loginAsTechnician().goToPatients().search("zzqx" + TestDataFactory.uniqueLetters(6));

        assertEquals(patients.rowCount(), 0);
        assertTrue(patients.isEmptyStateShown(), "empty state message");
    }

    @Test(groups = {"ui", "regression"}, description = "Duplicate registration is refused with a clear message")
    @Story("RX-102 Patient registration")
    @TmsLink("TC-UI-PAT-004")
    public void duplicatePatientIsRefused() {
        PatientRequest existing = TestDataFactory.newPatient();
        api().patients().createOk(existing);

        PatientDialog dialog = loginAsTechnician().goToPatients()
                .openAddPatient()
                .fill(existing.withPhone("412-555-0000"))
                .saveExpectingError();

        assertTrue(dialog.errorMessage().contains("already exists"), dialog.errorMessage());
    }
}
