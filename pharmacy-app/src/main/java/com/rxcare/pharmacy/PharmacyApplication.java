package com.rxcare.pharmacy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RxCare Pharmacy - a small retail pharmacy management system used as the
 * system under test (SUT) for the QA automation framework in ../qa-automation.
 */
@SpringBootApplication
public class PharmacyApplication {

    public static void main(String[] args) {
        SpringApplication.run(PharmacyApplication.class, args);
    }
}
