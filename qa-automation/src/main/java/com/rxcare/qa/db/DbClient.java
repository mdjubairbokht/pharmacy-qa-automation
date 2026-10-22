package com.rxcare.qa.db;

import com.rxcare.qa.config.Config;
import io.qameta.allure.Allure;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin JDBC helper for backend validation. Every query is parameterised (no string
 * concatenation of values) and is attached to the Allure report for traceability.
 */
public final class DbClient {

    private DbClient() {
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(Config.required("db.url"), Config.required("db.user"),
                Config.get("db.password", ""));
    }

    /** All rows as column-name -> value maps (column names are lower case, as returned by PostgreSQL). */
    public static List<Map<String, Object>> query(String sql, Object... params) {
        attach(sql, params);
        try (Connection conn = connect(); PreparedStatement ps = prepare(conn, sql, params);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException("Query failed: " + sql, e);
        }
    }

    /** Exactly one row, otherwise an AssertionError describing what was found. */
    public static Map<String, Object> queryOne(String sql, Object... params) {
        List<Map<String, Object>> rows = query(sql, params);
        if (rows.size() != 1) {
            throw new AssertionError("Expected exactly 1 row but found " + rows.size() + " for: " + sql
                    + " params=" + Arrays.toString(params));
        }
        return rows.get(0);
    }

    /** First column of the first row, converted to the requested type (null when no rows). */
    public static <T> T scalar(Class<T> type, String sql, Object... params) {
        List<Map<String, Object>> rows = query(sql, params);
        if (rows.isEmpty()) {
            return null;
        }
        Object value = rows.get(0).values().iterator().next();
        return convert(value, type);
    }

    public static int count(String sql, Object... params) {
        Integer value = scalar(Integer.class, sql, params);
        return value == null ? 0 : value;
    }

    /** INSERT/UPDATE/DELETE. Used only by negative constraint tests; throws the raw SQLException. */
    public static int execute(String sql, Object... params) throws SQLException {
        attach(sql, params);
        try (Connection conn = connect(); PreparedStatement ps = prepare(conn, sql, params)) {
            return ps.executeUpdate();
        }
    }

    private static PreparedStatement prepare(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        return ps;
    }

    @SuppressWarnings("unchecked")
    static <T> T convert(Object value, Class<T> type) {
        if (value == null || type.isInstance(value)) {
            return (T) value;
        }
        if (value instanceof Number number) {
            if (type == Integer.class) {
                return (T) Integer.valueOf(number.intValue());
            }
            if (type == Long.class) {
                return (T) Long.valueOf(number.longValue());
            }
            if (type == BigDecimal.class) {
                return (T) new BigDecimal(number.toString());
            }
        }
        if (type == String.class) {
            return (T) value.toString();
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass().getName() + " to " + type.getName());
    }

    private static void attach(String sql, Object... params) {
        Allure.addAttachment("SQL", "text/plain", sql + (params.length > 0 ? "\n-- params: " + Arrays.toString(params) : ""));
    }
}
