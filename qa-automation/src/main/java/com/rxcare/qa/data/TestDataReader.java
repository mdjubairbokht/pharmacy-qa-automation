package com.rxcare.qa.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Loads external test data (CSV / JSON on the classpath) for TestNG data providers. */
public final class TestDataReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestDataReader() {
    }

    /**
     * Reads a CSV with a header row. Lines starting with '#' are comments.
     * Values are split on commas (keep test data free of embedded commas).
     */
    public static Object[][] csv(String resource) {
        List<Object[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(open(resource), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                if (header) {
                    header = false;
                    continue;
                }
                String[] cells = line.split(",", -1);
                for (int i = 0; i < cells.length; i++) {
                    cells[i] = cells[i].trim();
                }
                rows.add(cells);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + resource, e);
        }
        return rows.toArray(new Object[0][]);
    }

    /** Reads a JSON array of objects; each object becomes one data-provider row with a single Map argument. */
    public static Object[][] jsonCases(String resource) {
        try (InputStream in = open(resource)) {
            List<Map<String, Object>> cases = MAPPER.readValue(in, new TypeReference<List<Map<String, Object>>>() {
            });
            Object[][] rows = new Object[cases.size()][];
            for (int i = 0; i < cases.size(); i++) {
                rows[i] = new Object[]{cases.get(i)};
            }
            return rows;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + resource, e);
        }
    }

    private static InputStream open(String resource) {
        InputStream in = TestDataReader.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalStateException("Test data file not found on classpath: " + resource);
        }
        return in;
    }
}
