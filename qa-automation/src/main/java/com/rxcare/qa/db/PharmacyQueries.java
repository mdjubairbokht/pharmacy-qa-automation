package com.rxcare.qa.db;

/**
 * SQL used for backend validation, kept in one place so business analysts and
 * developers can review exactly what each data-integrity check asserts.
 */
public final class PharmacyQueries {

    private PharmacyQueries() {
    }

    // ------------------------------------------------------------ lookups
    public static final String PATIENT_BY_ID =
            "SELECT * FROM patients WHERE id = ?";

    public static final String ALLERGIES_BY_PATIENT =
            "SELECT allergen FROM patient_allergies WHERE patient_id = ? ORDER BY allergen";

    public static final String PRESCRIPTION_BY_ID =
            "SELECT * FROM prescriptions WHERE id = ?";

    public static final String PRESCRIPTION_BY_RX_NUMBER =
            "SELECT * FROM prescriptions WHERE rx_number = ?";

    public static final String DRUG_QTY_ON_HAND =
            "SELECT qty_on_hand FROM drugs WHERE ndc = ?";

    public static final String STATUS_HISTORY_BY_RX =
            "SELECT from_status, to_status, changed_by FROM prescription_status_history "
                    + "WHERE prescription_id = ? ORDER BY id";

    public static final String DISPENSE_TXNS_BY_RX =
            "SELECT ndc, quantity, performed_by FROM inventory_transactions "
                    + "WHERE prescription_id = ? AND txn_type = 'DISPENSE' ORDER BY id";

    public static final String SALES_BY_RX =
            "SELECT amount, payment_method FROM pharmacy_transactions WHERE prescription_id = ? ORDER BY id";

    // ------------------------------------------------------------ integrity checks (expect 0 rows)
    public static final String ORPHAN_PRESCRIPTIONS =
            "SELECT p.id FROM prescriptions p LEFT JOIN patients pt ON pt.id = p.patient_id "
                    + "LEFT JOIN drugs d ON d.ndc = p.ndc WHERE pt.id IS NULL OR d.ndc IS NULL";

    public static final String ORPHAN_ALLERGIES =
            "SELECT a.id FROM patient_allergies a LEFT JOIN patients p ON p.id = a.patient_id WHERE p.id IS NULL";

    /** On-hand quantity must equal the running total of every inventory transaction (opening balance included). */
    public static final String INVENTORY_RECONCILIATION_BREAKS =
            "SELECT d.ndc, d.qty_on_hand, COALESCE(SUM(t.quantity), 0) AS ledger_total "
                    + "FROM drugs d LEFT JOIN inventory_transactions t ON t.ndc = d.ndc "
                    + "GROUP BY d.ndc, d.qty_on_hand HAVING d.qty_on_hand <> COALESCE(SUM(t.quantity), 0)";

    /** The newest history row of every prescription must match its current status. */
    public static final String STATUS_HISTORY_MISMATCHES =
            "SELECT p.id, p.status, h.to_status FROM prescriptions p "
                    + "JOIN LATERAL (SELECT to_status FROM prescription_status_history "
                    + "WHERE prescription_id = p.id ORDER BY id DESC LIMIT 1) h ON TRUE "
                    + "WHERE h.to_status <> p.status";

    public static final String PRESCRIPTIONS_WITHOUT_HISTORY =
            "SELECT p.id FROM prescriptions p WHERE NOT EXISTS "
                    + "(SELECT 1 FROM prescription_status_history h WHERE h.prescription_id = p.id)";

    /** One DISPENSE inventory movement per fill. */
    public static final String FILL_COUNT_VS_DISPENSE_MISMATCHES =
            "SELECT p.id, p.fill_count, COUNT(t.id) AS dispenses FROM prescriptions p "
                    + "LEFT JOIN inventory_transactions t ON t.prescription_id = p.id AND t.txn_type = 'DISPENSE' "
                    + "GROUP BY p.id, p.fill_count HAVING p.fill_count <> COUNT(t.id)";

    /** One sale per completed pickup: fills that are not yet picked up (status FILLED) have no sale. */
    public static final String SALES_VS_PICKUP_MISMATCHES =
            "SELECT p.id, p.status, p.fill_count, COUNT(s.id) AS sales FROM prescriptions p "
                    + "LEFT JOIN pharmacy_transactions s ON s.prescription_id = p.id "
                    + "GROUP BY p.id, p.status, p.fill_count "
                    + "HAVING COUNT(s.id) <> p.fill_count - CASE WHEN p.status = 'FILLED' THEN 1 ELSE 0 END";

    /** Every sale must equal quantity x unit price of the dispensed drug. */
    public static final String SALE_AMOUNT_MISMATCHES =
            "SELECT s.id, s.amount, ROUND(p.quantity * d.unit_price, 2) AS expected FROM pharmacy_transactions s "
                    + "JOIN prescriptions p ON p.id = s.prescription_id JOIN drugs d ON d.ndc = p.ndc "
                    + "WHERE s.amount <> ROUND(p.quantity * d.unit_price, 2)";

    public static final String CONTROLLED_SUBSTANCE_REFILL_VIOLATIONS =
            "SELECT p.id FROM prescriptions p JOIN drugs d ON d.ndc = p.ndc WHERE d.schedule = 2 AND p.refills > 0";

    public static final String ALLERGY_CONFLICTS =
            "SELECT p.id, p.patient_id, d.drug_class FROM prescriptions p JOIN drugs d ON d.ndc = p.ndc "
                    + "JOIN patient_allergies a ON a.patient_id = p.patient_id AND UPPER(a.allergen) = UPPER(d.drug_class) "
                    + "WHERE p.status <> 'CANCELLED'";

    public static final String WORKFLOW_FIELD_GAPS =
            "SELECT id, status FROM prescriptions WHERE "
                    + "(status IN ('VERIFIED','FILLED','PICKED_UP') AND (verified_by IS NULL OR verified_at IS NULL)) "
                    + "OR (status IN ('FILLED','PICKED_UP') AND (filled_by IS NULL OR filled_at IS NULL)) "
                    + "OR (status = 'PICKED_UP' AND picked_up_at IS NULL) "
                    + "OR (status = 'CANCELLED' AND cancel_reason IS NULL)";

    public static final String PLAIN_TEXT_PASSWORDS =
            "SELECT username FROM users WHERE password_hash !~ '^[0-9a-f]{64}$'";

    public static final String DUPLICATE_RX_NUMBERS =
            "SELECT rx_number, COUNT(*) FROM prescriptions GROUP BY rx_number HAVING COUNT(*) > 1";

    // ------------------------------------------------------------ ETL: daily sales
    public static final String ETL_SOURCE_AGGREGATE =
            "SELECT payment_method, COUNT(*) AS transaction_count, SUM(amount) AS total_amount "
                    + "FROM pharmacy_transactions WHERE (created_at AT TIME ZONE 'UTC')::date = ? "
                    + "GROUP BY payment_method ORDER BY payment_method";

    public static final String ETL_TARGET_ROWS =
            "SELECT payment_method, transaction_count, total_amount FROM daily_sales_summary "
                    + "WHERE sales_date = ? ORDER BY payment_method";

    public static final String ETL_DUPLICATE_KEYS =
            "SELECT sales_date, payment_method, COUNT(*) FROM daily_sales_summary "
                    + "GROUP BY sales_date, payment_method HAVING COUNT(*) > 1";
}
