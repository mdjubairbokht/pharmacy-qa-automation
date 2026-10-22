package com.rxcare.qa.api;

import com.rxcare.qa.config.Users;

/** Entry point to the API layer, bound to one user's token: {@code RxApi.asPharmacist().prescriptions().fill(id)}. */
public final class RxApi {

    private final String token;

    private RxApi(String token) {
        this.token = token;
    }

    public static RxApi as(String token) {
        return new RxApi(token);
    }

    public static RxApi anonymous() {
        return new RxApi(null);
    }

    public static RxApi asPharmacist() {
        return new RxApi(AuthApi.cachedToken(Users.pharmacist()));
    }

    public static RxApi asTechnician() {
        return new RxApi(AuthApi.cachedToken(Users.technician()));
    }

    public String token() {
        return token;
    }

    public PatientApi patients() {
        return new PatientApi(token);
    }

    public PrescriptionApi prescriptions() {
        return new PrescriptionApi(token);
    }

    public InventoryApi inventory() {
        return new InventoryApi(token);
    }

    public ReportApi reports() {
        return new ReportApi(token);
    }
}
