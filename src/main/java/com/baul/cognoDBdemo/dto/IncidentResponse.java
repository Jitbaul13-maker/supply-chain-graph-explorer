package com.baul.cognoDBdemo.dto;

public record IncidentResponse(
        String service,
        String incident,
        String severity
) {}