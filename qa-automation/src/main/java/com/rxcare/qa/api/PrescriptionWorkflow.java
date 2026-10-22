package com.rxcare.qa.api;

import com.rxcare.qa.api.models.Prescription;
import com.rxcare.qa.data.TestDataFactory;
import io.qameta.allure.Step;

/**
 * API shortcuts that put a prescription into a given workflow state. Used for fast test
 * setup (arrange via API, then act/assert through the UI, API or database).
 */
public final class PrescriptionWorkflow {

    private PrescriptionWorkflow() {
    }

    @Step("Setup: create a new patient via API")
    public static int newPatient(RxApi api) {
        return api.patients().createOk(TestDataFactory.newPatient()).id();
    }

    @Step("Setup: create a NEW prescription for NDC {ndc}, qty {quantity}")
    public static Prescription created(RxApi api, String ndc, int quantity) {
        int patientId = newPatient(api);
        return api.prescriptions().createOk(TestDataFactory.prescription(patientId, ndc).withQuantity(quantity));
    }

    @Step("Setup: create a VERIFIED prescription")
    public static Prescription verified(RxApi pharmacist, String ndc, int quantity) {
        Prescription rx = created(pharmacist, ndc, quantity);
        pharmacist.prescriptions().verify(rx.id()).then().statusCode(200);
        return pharmacist.prescriptions().getOk(rx.id());
    }

    @Step("Setup: create a FILLED prescription")
    public static Prescription filled(RxApi pharmacist, String ndc, int quantity) {
        Prescription rx = verified(pharmacist, ndc, quantity);
        pharmacist.prescriptions().fill(rx.id()).then().statusCode(200);
        return pharmacist.prescriptions().getOk(rx.id());
    }

    @Step("Setup: create a PICKED_UP prescription paid by {paymentMethod}")
    public static Prescription pickedUp(RxApi pharmacist, String ndc, int quantity, String paymentMethod) {
        Prescription rx = filled(pharmacist, ndc, quantity);
        pharmacist.prescriptions().pickup(rx.id(), paymentMethod).then().statusCode(200);
        return pharmacist.prescriptions().getOk(rx.id());
    }
}
