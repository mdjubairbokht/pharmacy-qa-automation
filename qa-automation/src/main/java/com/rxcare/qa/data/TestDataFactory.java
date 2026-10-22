package com.rxcare.qa.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxcare.qa.api.models.PatientRequest;
import com.rxcare.qa.api.models.PrescriptionRequest;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates unique, valid test data so tests are independent of each other and can
 * run in parallel against a shared database without collisions.
 */
public final class TestDataFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String[] FIRST_NAMES = {"Olivia", "Liam", "Emma", "Noah", "Sophia", "Mason", "Ava", "Lucas"};
    private static final String[] INSURANCE = {"BLUE-PPO", "AETNA-HMO", "MEDICARE-D", "CASH"};

    private TestDataFactory() {
    }

    /** Letters-only unique token (the app only accepts letters in names). */
    public static String uniqueLetters(int length) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }

    public static PatientRequest newPatient() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String first = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String last = "Qa" + uniqueLetters(8);
        LocalDate dob = LocalDate.of(1940, 1, 1).plusDays(random.nextInt(365 * 60));
        String phone = String.format("412-555-%04d", random.nextInt(10_000));
        return new PatientRequest(first, last, dob.toString(), random.nextBoolean() ? "F" : "M", phone,
                (first + "." + last + "@example.com").toLowerCase(),
                random.nextInt(1, 999) + " Test Street, Pittsburgh, PA",
                INSURANCE[random.nextInt(INSURANCE.length)],
                List.of());
    }

    public static PatientRequest newPatientWithAllergies(String... allergens) {
        return newPatient().withAllergies(List.of(allergens));
    }

    public static PrescriptionRequest prescription(int patientId, String ndc) {
        return new PrescriptionRequest(patientId, ndc, "Dr. Quinn Tester", "Take 1 tablet by mouth once daily",
                30, 30, 2);
    }

    /** Converts a typed payload into a mutable map so negative tests can break individual fields. */
    public static Map<String, Object> asMap(Object payload) {
        return new LinkedHashMap<>(MAPPER.convertValue(payload, new TypeReference<Map<String, Object>>() {
        }));
    }

    public static Map<String, Object> withOverrides(Object payload, Map<String, Object> overrides) {
        Map<String, Object> map = asMap(payload);
        map.putAll(overrides);
        return map;
    }
}
