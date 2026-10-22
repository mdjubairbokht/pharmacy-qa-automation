package com.rxcare.pharmacy.auth;

import com.rxcare.pharmacy.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Rejects /api/** calls that do not carry a valid "Authorization: Bearer &lt;token&gt;" header. */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER = "currentUser";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        UserSession user = authService.resolve(extractToken(request))
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHORIZED",
                        "Missing or invalid authentication token"));
        request.setAttribute(CURRENT_USER, user);
        return true;
    }

    public static String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring("Bearer ".length()).trim();
    }
}
