package com.rxcare.qa.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Patient(
        int id,
        String firstName,
        String lastName,
        String dateOfBirth,
        String gender,
        String phone,
        String email,
        String address,
        String insurancePlan,
        List<String> allergies,
        String createdAt) {
}
