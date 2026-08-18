package com.baul.cognoDBdemo.dto;

public record RegionResponse(
        String service,
        String environment,
        String region
) {
}