package com.rxcare.pharmacy.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** An authenticated user, resolved from the bearer token on every API call. */
public record UserSession(String token, String username, String displayName, String role) {

    public static final String PHARMACIST = "PHARMACIST";
    public static final String TECHNICIAN = "TECHNICIAN";

    @JsonIgnore
    public boolean isPharmacist() {
        return PHARMACIST.equals(role);
    }
}
