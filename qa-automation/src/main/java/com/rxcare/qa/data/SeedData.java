package com.rxcare.qa.data;

/**
 * Reference data created by the application's data.sql seed. Tests rely on these
 * known records instead of hard-coding values in every class.
 */
public final class SeedData {

    private SeedData() {
    }

    // Drugs (NDC 5-4-2 format)
    public static final String NDC_AMOXICILLIN = "50000-0101-01";   // class PENICILLIN
    public static final String NDC_LISINOPRIL = "50000-0202-01";
    public static final String NDC_ATORVASTATIN = "50000-0303-01";
    public static final String NDC_METFORMIN = "50000-0404-01";
    public static final String NDC_SERTRALINE = "50000-0505-01";
    public static final String NDC_SMX_TMP = "50000-0606-01";       // class SULFONAMIDE
    public static final String NDC_OXYCODONE = "50000-0707-01";     // Schedule II controlled substance
    public static final String NDC_ALBUTEROL = "50000-0808-01";     // seeded below its reorder level (8 on hand)
    public static final String NDC_OMEPRAZOLE = "50000-0909-01";
    public static final String NDC_UNKNOWN = "99999-9999-99";

    // Patients
    public static final int PATIENT_JOHN_CARTER = 1;   // PENICILLIN allergy
    public static final int PATIENT_MARIA_LOPEZ = 2;   // SULFONAMIDE allergy
    public static final int PATIENT_AISHA_PATEL = 4;   // OPIOID allergy
    public static final int PATIENT_UNKNOWN = 999_999;

    // Seeded prescriptions
    public static final String RX_SEEDED_NEW = "RX1000001";
    public static final String RX_SEEDED_VERIFIED = "RX1000003";
}
