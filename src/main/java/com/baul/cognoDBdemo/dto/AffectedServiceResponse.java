package com.baul.cognoDBdemo.dto;

public record AffectedServiceResponse(
        String service,
        int hops
) {
}