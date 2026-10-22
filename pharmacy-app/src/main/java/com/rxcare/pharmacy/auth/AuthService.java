package com.rxcare.pharmacy.auth;

import com.rxcare.pharmacy.common.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal token based authentication. Tokens are kept in memory, which is
 * fine for a demo SUT that is restarted for every test run.
 */
@Service
public class AuthService {

    private final JdbcTemplate jdbc;
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    public AuthService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UserSession login(LoginRequest request) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT username, password_hash, display_name, role, active FROM users WHERE username = ?",
                request.username().trim());

        if (rows.isEmpty() || !sha256(request.password()).equals(rows.get(0).get("password_hash"))) {
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid username or password");
        }
        Map<String, Object> user = rows.get(0);
        if (!Boolean.TRUE.equals(user.get("active"))) {
            throw ApiException.forbidden("ACCOUNT_DISABLED", "Account is disabled. Contact your pharmacy manager.");
        }

        UserSession session = new UserSession(
                UUID.randomUUID().toString(),
                (String) user.get("username"),
                (String) user.get("display_name"),
                (String) user.get("role"));
        sessions.put(session.token(), session);
        return session;
    }

    public void logout(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public Optional<UserSession> resolve(String token) {
        return token == null ? Optional.empty() : Optional.ofNullable(sessions.get(token));
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
