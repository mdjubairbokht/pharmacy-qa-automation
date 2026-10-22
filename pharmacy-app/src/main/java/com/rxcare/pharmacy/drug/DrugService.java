package com.rxcare.pharmacy.drug;

import com.rxcare.pharmacy.common.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class DrugService {

    private final JdbcTemplate jdbc;

    public DrugService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Drug> list() {
        return jdbc.query("SELECT * FROM drugs ORDER BY name", this::map);
    }

    public List<Drug> lowStock() {
        return jdbc.query("SELECT * FROM drugs WHERE qty_on_hand <= reorder_level ORDER BY name", this::map);
    }

    public Drug get(String ndc) {
        List<Drug> found = jdbc.query("SELECT * FROM drugs WHERE ndc = ?", this::map, ndc);
        if (found.isEmpty()) {
            throw ApiException.notFound("DRUG_NOT_FOUND", "Drug with NDC " + ndc + " not found");
        }
        return found.get(0);
    }

    @Transactional
    public Drug adjust(String ndc, InventoryAdjustmentRequest req, String username) {
        Drug drug = get(ndc);
        int change = req.quantityChange();
        if (change == 0) {
            throw ApiException.unprocessable("INVALID_QUANTITY", "Quantity change must not be zero");
        }
        if ("RECEIVE".equals(req.type()) && change < 0) {
            throw ApiException.unprocessable("INVALID_QUANTITY", "Received quantity must be positive");
        }
        int updated = jdbc.update(
                "UPDATE drugs SET qty_on_hand = qty_on_hand + ? WHERE ndc = ? AND qty_on_hand + ? >= 0",
                change, ndc, change);
        if (updated == 0) {
            throw ApiException.unprocessable("NEGATIVE_INVENTORY",
                    "Adjustment would make on-hand quantity negative (on hand: " + drug.qtyOnHand() + ")");
        }
        jdbc.update("INSERT INTO inventory_transactions (ndc, txn_type, quantity, reason, performed_by) "
                + "VALUES (?, ?, ?, ?, ?)", ndc, req.type(), change, req.reason(), username);
        return get(ndc);
    }

    public List<InventoryTransaction> transactions(String ndc) {
        get(ndc);
        return jdbc.query("SELECT * FROM inventory_transactions WHERE ndc = ? ORDER BY id",
                (rs, n) -> new InventoryTransaction(
                        rs.getInt("id"),
                        rs.getString("ndc"),
                        (Integer) rs.getObject("prescription_id"),
                        rs.getString("txn_type"),
                        rs.getInt("quantity"),
                        rs.getString("reason"),
                        rs.getString("performed_by"),
                        rs.getObject("created_at", OffsetDateTime.class)),
                ndc);
    }

    private Drug map(ResultSet rs, int rowNum) throws SQLException {
        int qty = rs.getInt("qty_on_hand");
        int reorder = rs.getInt("reorder_level");
        return new Drug(
                rs.getString("ndc"),
                rs.getString("name"),
                rs.getString("strength"),
                rs.getString("dosage_form"),
                rs.getString("drug_class"),
                rs.getInt("schedule"),
                rs.getBigDecimal("unit_price"),
                qty,
                reorder,
                qty <= reorder);
    }
}
