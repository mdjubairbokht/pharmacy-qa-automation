package com.rxcare.pharmacy.common;

import java.time.OffsetDateTime;
import java.util.List;

/** Uniform error payload returned by every endpoint. */
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<String> details) {
}
