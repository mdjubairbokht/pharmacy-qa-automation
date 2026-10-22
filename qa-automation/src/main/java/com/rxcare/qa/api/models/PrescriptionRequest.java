package com.rxcare.qa.api.models;

public record PrescriptionRequest(
        Integer patientId,
        String ndc,
        String prescriber,
        String sig,
        Integer quantity,
        Integer daysSupply,
        Integer refills) {

    public PrescriptionRequest withQuantity(Integer value) {
        return new PrescriptionRequest(patientId, ndc, prescriber, sig, value, daysSupply, refills);
    }

    public PrescriptionRequest withRefills(Integer value) {
        return new PrescriptionRequest(patientId, ndc, prescriber, sig, quantity, daysSupply, value);
    }

    public PrescriptionRequest withDaysSupply(Integer value) {
        return new PrescriptionRequest(patientId, ndc, prescriber, sig, quantity, value, refills);
    }

    public PrescriptionRequest withNdc(String value) {
        return new PrescriptionRequest(patientId, value, prescriber, sig, quantity, daysSupply, refills);
    }

    public PrescriptionRequest withPatientId(Integer value) {
        return new PrescriptionRequest(value, ndc, prescriber, sig, quantity, daysSupply, refills);
    }
}
