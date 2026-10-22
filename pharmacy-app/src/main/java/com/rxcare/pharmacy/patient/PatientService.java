package com.rxcare.pharmacy.patient;

import com.rxcare.pharmacy.common.ApiException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PatientService {

    private static final String SELECT = "SELECT * FROM patients ";

    private final JdbcTemplate jdbc;

    public PatientService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Patient> search(String term) {
        if (term == null || term.isBlank()) {
            return jdbc.query(SELECT + "ORDER BY last_name, first_name", this::map);
        }
        String like = "%" + term.trim().toLowerCase(Locale.ROOT) + "%";
        return jdbc.query(SELECT
                        + "WHERE LOWER(first_name || ' ' || last_name) LIKE ? OR LOWER(last_name) LIKE ? OR phone LIKE ? "
                        + "ORDER BY last_name, first_name",
                this::map, like, like, like);
    }

    public Patient get(int id) {
        List<Patient> found = jdbc.query(SELECT + "WHERE id = ?", this::map, id);
        if (found.isEmpty()) {
            throw ApiException.notFound("PATIENT_NOT_FOUND", "Patient " + id + " not found");
        }
        return found.get(0);
    }

    @Transactional
    public Patient create(PatientRequest req) {
        Integer id;
        try {
            id = jdbc.queryForObject(
                    "INSERT INTO patients (first_name, last_name, date_of_birth, gender, phone, email, address, insurance_plan) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                    Integer.class,
                    req.firstName().trim(), req.lastName().trim(), req.dateOfBirth(), req.gender(),
                    req.phone(), blankToNull(req.email()), blankToNull(req.address()), blankToNull(req.insurancePlan()));
        } catch (DuplicateKeyException ex) {
            throw ApiException.conflict("DUPLICATE_PATIENT",
                    "A patient with the same name and date of birth already exists");
        }
        saveAllergies(id, req.allergies());
        return get(id);
    }

    @Transactional
    public Patient update(int id, PatientRequest req) {
        int updated;
        try {
            updated = jdbc.update(
                    "UPDATE patients SET first_name = ?, last_name = ?, date_of_birth = ?, gender = ?, phone = ?, "
                            + "email = ?, address = ?, insurance_plan = ? WHERE id = ?",
                    req.firstName().trim(), req.lastName().trim(), req.dateOfBirth(), req.gender(), req.phone(),
                    blankToNull(req.email()), blankToNull(req.address()), blankToNull(req.insurancePlan()), id);
        } catch (DuplicateKeyException ex) {
            throw ApiException.conflict("DUPLICATE_PATIENT",
                    "A patient with the same name and date of birth already exists");
        }
        if (updated == 0) {
            throw ApiException.notFound("PATIENT_NOT_FOUND", "Patient " + id + " not found");
        }
        jdbc.update("DELETE FROM patient_allergies WHERE patient_id = ?", id);
        saveAllergies(id, req.allergies());
        return get(id);
    }

    public boolean exists(int id) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM patients WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private void saveAllergies(int patientId, List<String> allergies) {
        if (allergies == null) {
            return;
        }
        allergies.stream()
                .map(a -> a.trim().toUpperCase(Locale.ROOT))
                .filter(a -> !a.isEmpty())
                .distinct()
                .forEach(a -> jdbc.update(
                        "INSERT INTO patient_allergies (patient_id, allergen) VALUES (?, ?)", patientId, a));
    }

    private List<String> allergiesFor(int patientId) {
        return new ArrayList<>(jdbc.queryForList(
                "SELECT allergen FROM patient_allergies WHERE patient_id = ? ORDER BY allergen",
                String.class, patientId));
    }

    private Patient map(ResultSet rs, int rowNum) throws SQLException {
        int id = rs.getInt("id");
        return new Patient(
                id,
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getObject("date_of_birth", LocalDate.class),
                rs.getString("gender"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("address"),
                rs.getString("insurance_plan"),
                allergiesFor(id),
                rs.getObject("created_at", OffsetDateTime.class));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
