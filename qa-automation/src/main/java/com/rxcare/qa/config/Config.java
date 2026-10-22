package com.rxcare.qa.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

/**
 * Layered configuration lookup, highest priority first:
 * <ol>
 *   <li>JVM system property (-Dkey=value)</li>
 *   <li>Environment variable (db.url -> DB_URL, app.baseUrl -> APP_BASE_URL)</li>
 *   <li>config/&lt;env&gt;.properties  (env = -Denv / TEST_ENV, default "local")</li>
 *   <li>config/default.properties</li>
 * </ol>
 */
public final class Config {

    private static final String ENV = resolveEnv();
    private static final Properties PROPS = load();

    private Config() {
    }

    public static String env() {
        return ENV;
    }

    public static String get(String key) {
        String value = System.getProperty(key);
        if (isBlank(value)) {
            value = System.getenv(toEnvVar(key));
        }
        if (isBlank(value)) {
            value = PROPS.getProperty(key);
        }
        return value == null ? null : value.trim();
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return isBlank(value) ? defaultValue : value;
    }

    public static String required(String key) {
        String value = get(key);
        if (isBlank(value)) {
            throw new IllegalStateException("Missing required configuration key '" + key + "' for env " + ENV);
        }
        return value;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        return isBlank(value) ? defaultValue : Integer.parseInt(value);
    }

    public static boolean getBool(String key, boolean defaultValue) {
        String value = get(key);
        return isBlank(value) ? defaultValue : Boolean.parseBoolean(value);
    }

    public static String apiBaseUrl() {
        return stripSlash(required("app.baseUrl"));
    }

    public static String uiBaseUrl() {
        return stripSlash(required("ui.baseUrl"));
    }

    static String toEnvVar(String key) {
        return key.replaceAll("([a-z])([A-Z])", "$1_$2").replace('.', '_').toUpperCase(Locale.ROOT);
    }

    private static String resolveEnv() {
        String env = System.getProperty("env");
        if (isBlank(env)) {
            env = System.getenv("TEST_ENV");
        }
        return isBlank(env) ? "local" : env.trim();
    }

    private static Properties load() {
        Properties props = new Properties();
        loadInto(props, "config/default.properties", true);
        loadInto(props, "config/" + ENV + ".properties", false);
        return props;
    }

    private static void loadInto(Properties props, String resource, boolean required) {
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                if (required) {
                    throw new IllegalStateException("Configuration file not found on classpath: " + resource);
                }
                return;
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + resource, e);
        }
    }

    private static String stripSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
