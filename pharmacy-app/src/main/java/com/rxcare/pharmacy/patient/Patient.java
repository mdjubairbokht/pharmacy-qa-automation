package com.rxcare.pharmacy.patient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record Patient(
        int id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        String phone,
        String email,
        String address,
        String insurancePlan,
        List<String> allergies,
        OffsetDateTime createdAt) {
}
