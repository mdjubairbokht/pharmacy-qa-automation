package com.rxcare.qa.api.models;

import java.util.List;

/** Patient payload, used both for API calls and for filling the UI form. */
public record PatientRequest(
        String firstName,
        String lastName,
        String dateOfBirth,
        String gender,
        String phone,
        String email,
        String address,
        String insurancePlan,
        List<String> allergies) {

    public String displayName() {
        return lastName + ", " + firstName;
    }

    public PatientRequest withFirstName(String value) {
        return new PatientRequest(value, lastName, dateOfBirth, gender, phone, email, address, insurancePlan, allergies);
    }

    public PatientRequest withLastName(String value) {
        return new PatientRequest(firstName, value, dateOfBirth, gender, phone, email, address, insurancePlan, allergies);
    }

    public PatientRequest withDateOfBirth(String value) {
        return new PatientRequest(firstName, lastName, value, gender, phone, email, address, insurancePlan, allergies);
    }

    public PatientRequest withPhone(String value) {
        return new PatientRequest(firstName, lastName, dateOfBirth, gender, value, email, address, insurancePlan, allergies);
    }

    public PatientRequest withAllergies(List<String> value) {
        return new PatientRequest(firstName, lastName, dateOfBirth, gender, phone, email, address, insurancePlan, value);
    }
}
