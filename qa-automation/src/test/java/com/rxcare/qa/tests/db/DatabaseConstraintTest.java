package com.rxcare.qa.tests.db;

import com.rxcare.qa.db.DbClient;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.sql.SQLException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Negative tests for the database itself: the schema must reject bad data even if it
 * bypasses the API (ETL loads, manual fixes, other integrations). Every statement here is
 * expected to fail, so nothing is ever written.
 */
@Epic("Data Quality")
@Feature("Database Constraints")
public class DatabaseConstraintTest {

    // PostgreSQL SQLSTATE codes
    private static final String CHECK_VIOLATION = "23514";
    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String NOT_NULL_VIOLATION = "23502";

    @DataProvider(name = "badWrites")
    public Object[][] badWrites() {
        return new Object[][]{
                {"Negative on-hand quantity",
                        "UPDATE drugs SET qty_on_hand = -1 WHERE ndc = '50000-0101-01'", CHECK_VIOLATION},
                {"Unknown prescription status",
                        "UPDATE prescriptions SET status = 'SHIPPED' WHERE id = 1", CHECK_VIOLATION},
                {"Days supply above 90",
                        "UPDATE prescriptions SET days_supply = 120 WHERE id = 1", CHECK_VIOLATION},
                {"More refills remaining than written",
                        "UPDATE prescriptions SET refills_remaining = refills + 1 WHERE id = 1", CHECK_VIOLATION},
                {"Date of birth in the future",
                        "INSERT INTO patients (first_name, last_name, date_of_birth, gender, phone) "
                                + "VALUES ('Future', 'Baby', CURRENT_DATE + 1, 'U', '412-555-0000')", CHECK_VIOLATION},
                {"Invalid payment method",
                        "INSERT INTO pharmacy_transactions (prescription_id, amount, payment_method) VALUES (1, 5.00, 'BITCOIN')",
                        CHECK_VIOLATION},
                {"Zero-quantity inventory movement",
                        "INSERT INTO inventory_transactions (ndc, txn_type, quantity, performed_by) "
                                + "VALUES ('50000-0101-01', 'ADJUST', 0, 'qa')", CHECK_VIOLATION},
                {"Prescription for a patient that does not exist",
                        "INSERT INTO prescriptions (rx_number, patient_id, ndc, prescriber, sig, quantity, days_supply, "
                                + "refills, refills_remaining, status, created_by) VALUES ('RXQA00001', 999999, "
                                + "'50000-0101-01', 'Dr. QA', 'Test', 10, 10, 0, 0, 'NEW', 'qa')", FOREIGN_KEY_VIOLATION},
                {"Duplicate patient identity (name + date of birth)",
                        "INSERT INTO patients (first_name, last_name, date_of_birth, gender, phone) "
                                + "SELECT first_name, last_name, date_of_birth, gender, phone FROM patients WHERE id = 1",
                        UNIQUE_VIOLATION},
                {"Duplicate Rx number",
                        "UPDATE prescriptions SET rx_number = (SELECT rx_number FROM prescriptions WHERE id = 1) WHERE id = 2",
                        UNIQUE_VIOLATION},
                {"Patient without a phone number",
                        "INSERT INTO patients (first_name, last_name, date_of_birth, gender) "
                                + "VALUES ('No', 'Phone', DATE '1980-01-01', 'F')", NOT_NULL_VIOLATION}
        };
    }

    @Test(groups = {"db", "regression"}, dataProvider = "badWrites",
            description = "Schema constraint rejects invalid data written directly to the database")
    @Story("RX-113 Audit trail & data integrity")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-DB-003")
    public void constraintRejectsBadData(String scenario, String sql, String expectedSqlState) {
        try {
            DbClient.execute(sql);
            fail("Database accepted invalid data: " + scenario);
        } catch (SQLException e) {
            assertEquals(e.getSQLState(), expectedSqlState, scenario + " -> " + e.getMessage());
        }
    }
}
