package com.rxcare.qa.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiError(int status, String error, String code, String message, String path, List<String> details) {
}
