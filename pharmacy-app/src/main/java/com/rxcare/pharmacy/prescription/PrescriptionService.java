package com.rxcare.pharmacy.prescription;

import com.rxcare.pharmacy.auth.UserSession;
import com.rxcare.pharmacy.common.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.rxcare.pharmacy.prescription.PrescriptionStatus.CANCELLED;
import static com.rxcare.pharmacy.prescription.PrescriptionStatus.FILLED;
import static com.rxcare.pharmacy.prescription.PrescriptionStatus.NEW;
import static com.rxcare.pharmacy.prescription.PrescriptionStatus.PICKED_UP;
import static com.rxcare.pharmacy.prescription.PrescriptionStatus.VERIFIED;

/**
 * Prescription lifecycle and the pharmacy business rules around it:
 * allergy screening, controlled-substance refill limits, pharmacist-only
 * verification, inventory decrement on fill and the point-of-sale record on pickup.
 */
@Service
public class PrescriptionService {

    private static final String SELECT =
            "SELECT p.*, pt.first_name || ' ' || pt.last_name AS patient_name, "
                    + "d.name || ' ' || d.strength AS drug_name "
                    + "FROM prescriptions p "
                    + "JOIN patients pt ON pt.id = p.patient_id "
                    + "JOIN drugs d ON d.ndc = p.ndc ";

    private final JdbcTemplate jdbc;

    public PrescriptionService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Prescription> list(String status, Integer patientId) {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append("AND p.status = ? ");
            params.add(parseStatus(status).name());
        }
        if (patientId != null) {
            sql.append("AND p.patient_id = ? ");
            params.add(patientId);
        }
        sql.append("ORDER BY p.id DESC");
        return jdbc.query(sql.toString(), this::map, params.toArray());
    }

    public Prescription get(int id) {
        List<Prescription> found = jdbc.query(SELECT + "WHERE p.id = ?", this::map, id);
        if (found.isEmpty()) {
            throw ApiException.notFound("PRESCRIPTION_NOT_FOUND", "Prescription " + id + " not found");
        }
        return found.get(0);
    }

    @Transactional
    public Prescription create(PrescriptionRequest req, UserSession user) {
        Integer patientCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM patients WHERE id = ?", Integer.class, req.patientId());
        if (patientCount == null || patientCount == 0) {
            throw ApiException.notFound("PATIENT_NOT_FOUND", "Patient " + req.patientId() + " not found");
        }

        List<Map<String, Object>> drugs = jdbc.queryForList(
                "SELECT schedule, drug_class FROM drugs WHERE ndc = ?", req.ndc());
        if (drugs.isEmpty()) {
            throw ApiException.notFound("DRUG_NOT_FOUND", "Drug with NDC " + req.ndc() + " not found");
        }
        int schedule = ((Number) drugs.get(0).get("schedule")).intValue();
        String drugClass = (String) drugs.get(0).get("drug_class");

        // Federal rule: Schedule II controlled substances may not be refilled.
        if (schedule == 2 && req.refills() > 0) {
            throw ApiException.unprocessable("CONTROLLED_SUBSTANCE_REFILLS",
                    "Schedule II controlled substances cannot have refills");
        }

        // Drug utilization review: block a drug whose class matches a documented allergy.
        Integer allergyMatches = jdbc.queryForObject(
                "SELECT COUNT(*) FROM patient_allergies WHERE patient_id = ? AND UPPER(allergen) = UPPER(?)",
                Integer.class, req.patientId(), drugClass);
        if (allergyMatches != null && allergyMatches > 0) {
            throw ApiException.unprocessable("ALLERGY_CONFLICT",
                    "Patient has a documented " + drugClass + " allergy");
        }

        Integer id = jdbc.queryForObject(
                "INSERT INTO prescriptions (rx_number, patient_id, ndc, prescriber, sig, quantity, days_supply, "
                        + "refills, refills_remaining, status, created_by) "
                        + "VALUES ('RX' || nextval('rx_number_seq'), ?, ?, ?, ?, ?, ?, ?, ?, 'NEW', ?) RETURNING id",
                Integer.class,
                req.patientId(), req.ndc(), req.prescriber().trim(), req.sig().trim(), req.quantity(),
                req.daysSupply(), req.refills(), req.refills(), user.username());

        jdbc.update("INSERT INTO prescription_status_history (prescription_id, from_status, to_status, changed_by) "
                + "VALUES (?, NULL, 'NEW', ?)", id, user.username());
        return get(id);
    }

    @Transactional
    public Prescription verify(int id, UserSession user) {
        if (!user.isPharmacist()) {
            throw ApiException.forbidden("PHARMACIST_ONLY", "Only a pharmacist can verify prescriptions");
        }
        requireTransition(id, VERIFIED);
        changeStatus(id, NEW, VERIFIED, user, "verified_by = ?, verified_at = NOW()", user.username());
        return get(id);
    }

    @Transactional
    public Prescription fill(int id, UserSession user) {
        Prescription rx = requireTransition(id, FILLED);

        int updated = jdbc.update(
                "UPDATE drugs SET qty_on_hand = qty_on_hand - ? WHERE ndc = ? AND qty_on_hand >= ?",
                rx.quantity(), rx.ndc(), rx.quantity());
        if (updated == 0) {
            Integer onHand = jdbc.queryForObject("SELECT qty_on_hand FROM drugs WHERE ndc = ?",
                    Integer.class, rx.ndc());
            throw ApiException.unprocessable("INSUFFICIENT_INVENTORY",
                    "Insufficient inventory for " + rx.ndc() + ": on hand " + onHand + ", required " + rx.quantity());
        }
        jdbc.update("INSERT INTO inventory_transactions (ndc, prescription_id, txn_type, quantity, reason, performed_by) "
                        + "VALUES (?, ?, 'DISPENSE', ?, ?, ?)",
                rx.ndc(), id, -rx.quantity(), "Dispensed " + rx.rxNumber(), user.username());

        changeStatus(id, VERIFIED, FILLED, user,
                "filled_by = ?, filled_at = NOW(), fill_count = fill_count + 1", user.username());
        return get(id);
    }

    @Transactional
    public Prescription pickup(int id, PickupRequest req, UserSession user) {
        Prescription rx = requireTransition(id, PICKED_UP);
        BigDecimal unitPrice = jdbc.queryForObject("SELECT unit_price FROM drugs WHERE ndc = ?",
                BigDecimal.class, rx.ndc());
        BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(rx.quantity())).setScale(2, RoundingMode.HALF_UP);

        jdbc.update("INSERT INTO pharmacy_transactions (prescription_id, amount, payment_method) VALUES (?, ?, ?)",
                id, amount, req.paymentMethod());
        changeStatus(id, FILLED, PICKED_UP, user, "picked_up_at = NOW()");
        return get(id);
    }

    @Transactional
    public Prescription cancel(int id, CancelRequest req, UserSession user) {
        Prescription rx = requireTransition(id, CANCELLED);
        changeStatus(id, PrescriptionStatus.valueOf(rx.status()), CANCELLED, user,
                "cancel_reason = ?", req.reason().trim());
        return get(id);
    }

    @Transactional
    public Prescription refill(int id, UserSession user) {
        Prescription rx = requireTransition(id, NEW);
        if (rx.refillsRemaining() <= 0) {
            throw ApiException.unprocessable("NO_REFILLS_REMAINING",
                    "Prescription " + rx.rxNumber() + " has no refills remaining");
        }
        changeStatus(id, PICKED_UP, NEW, user,
                "refills_remaining = refills_remaining - 1, verified_by = NULL, verified_at = NULL, "
                        + "filled_by = NULL, filled_at = NULL, picked_up_at = NULL");
        return get(id);
    }

    public List<StatusHistory> history(int id) {
        get(id);
        return jdbc.query("SELECT * FROM prescription_status_history WHERE prescription_id = ? ORDER BY id",
                (rs, n) -> new StatusHistory(
                        rs.getString("from_status"),
                        rs.getString("to_status"),
                        rs.getString("changed_by"),
                        rs.getObject("changed_at", OffsetDateTime.class)),
                id);
    }

    public List<PharmacyTransaction> transactions(int id) {
        get(id);
        return jdbc.query("SELECT * FROM pharmacy_transactions WHERE prescription_id = ? ORDER BY id",
                (rs, n) -> new PharmacyTransaction(
                        rs.getInt("id"),
                        rs.getInt("prescription_id"),
                        rs.getBigDecimal("amount"),
                        rs.getString("payment_method"),
                        rs.getObject("created_at", OffsetDateTime.class)),
                id);
    }

    private Prescription requireTransition(int id, PrescriptionStatus target) {
        Prescription rx = get(id);
        PrescriptionStatus current = PrescriptionStatus.valueOf(rx.status());
        if (!current.canTransitionTo(target)) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION",
                    "Prescription " + rx.rxNumber() + " cannot move from " + current + " to " + target);
        }
        return rx;
    }

    /** Status update guarded by the expected current status (optimistic concurrency) plus an audit row. */
    private void changeStatus(int id, PrescriptionStatus from, PrescriptionStatus to, UserSession user,
                              String setClause, Object... setArgs) {
        List<Object> params = new ArrayList<>(List.of(setArgs));
        params.add(to.name());
        params.add(id);
        params.add(from.name());
        int updated = jdbc.update("UPDATE prescriptions SET " + setClause + ", status = ? WHERE id = ? AND status = ?",
                params.toArray());
        if (updated == 0) {
            throw ApiException.conflict("CONCURRENT_UPDATE", "Prescription " + id + " was changed by another user");
        }
        jdbc.update("INSERT INTO prescription_status_history (prescription_id, from_status, to_status, changed_by) "
                + "VALUES (?, ?, ?, ?)", id, from.name(), to.name(), user.username());
    }

    private static PrescriptionStatus parseStatus(String status) {
        try {
            return PrescriptionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("INVALID_STATUS", "Unknown prescription status: " + status);
        }
    }

    private Prescription map(ResultSet rs, int rowNum) throws SQLException {
        return new Prescription(
                rs.getInt("id"),
                rs.getString("rx_number"),
                rs.getInt("patient_id"),
                rs.getString("patient_name"),
                rs.getString("ndc"),
                rs.getString("drug_name"),
                rs.getString("prescriber"),
                rs.getString("sig"),
                rs.getInt("quantity"),
                rs.getInt("days_supply"),
                rs.getInt("refills"),
                rs.getInt("refills_remaining"),
                rs.getString("status"),
                rs.getInt("fill_count"),
                rs.getString("created_by"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getString("verified_by"),
                rs.getObject("verified_at", OffsetDateTime.class),
                rs.getString("filled_by"),
                rs.getObject("filled_at", OffsetDateTime.class),
                rs.getObject("picked_up_at", OffsetDateTime.class),
                rs.getString("cancel_reason"));
    }
}
