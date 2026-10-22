package com.rxcare.pharmacy.prescription;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Developer-level unit tests for the prescription state machine (runs in `mvn verify` of the app). */
class PrescriptionStatusTest {

    @ParameterizedTest(name = "{0} -> {1} allowed = {2}")
    @CsvSource({
            "NEW, VERIFIED, true",
            "NEW, CANCELLED, true",
            "NEW, FILLED, false",
            "VERIFIED, FILLED, true",
            "VERIFIED, CANCELLED, true",
            "VERIFIED, PICKED_UP, false",
            "FILLED, PICKED_UP, true",
            "FILLED, CANCELLED, false",
            "PICKED_UP, NEW, true",
            "PICKED_UP, VERIFIED, false",
            "CANCELLED, NEW, false"
    })
    void transitions(PrescriptionStatus from, PrescriptionStatus to, boolean allowed) {
        assertEquals(allowed, from.canTransitionTo(to));
    }

    @Test
    void cancelledIsTerminal() {
        assertTrue(PrescriptionStatus.CANCELLED.allowedNext().isEmpty());
    }
}
