package com.rxcare.qa.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StatusHistory(String fromStatus, String toStatus, String changedBy, String changedAt) {
}
