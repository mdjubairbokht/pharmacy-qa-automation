package com.rxcare.qa.config;

/** Seeded application accounts, resolved through {@link Config} so CI can inject other credentials. */
public final class Users {

    public record Credentials(String username, String password) {
    }

    private Users() {
    }

    public static Credentials pharmacist() {
        return new Credentials(Config.required("pharmacist.username"), Config.required("pharmacist.password"));
    }

    public static Credentials technician() {
        return new Credentials(Config.required("technician.username"), Config.required("technician.password"));
    }

    public static Credentials disabled() {
        return new Credentials(Config.required("disabled.username"), Config.required("disabled.password"));
    }
}
