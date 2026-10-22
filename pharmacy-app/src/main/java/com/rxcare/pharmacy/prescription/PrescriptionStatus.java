package com.rxcare.pharmacy.prescription;

import java.util.EnumSet;
import java.util.Set;

/**
 * Prescription workflow:
 * <pre>
 *   NEW --verify--> VERIFIED --fill--> FILLED --pickup--> PICKED_UP
 *    |                 |                                     |
 *    +----cancel-------+--> CANCELLED          refill -> NEW (when refills remain)
 * </pre>
 */
public enum PrescriptionStatus {
    NEW,
    VERIFIED,
    FILLED,
    PICKED_UP,
    CANCELLED;

    public Set<PrescriptionStatus> allowedNext() {
        return switch (this) {
            case NEW -> EnumSet.of(VERIFIED, CANCELLED);
            case VERIFIED -> EnumSet.of(FILLED, CANCELLED);
            case FILLED -> EnumSet.of(PICKED_UP);
            case PICKED_UP -> EnumSet.of(NEW);
            case CANCELLED -> EnumSet.noneOf(PrescriptionStatus.class);
        };
    }

    public boolean canTransitionTo(PrescriptionStatus target) {
        return allowedNext().contains(target);
    }
}
