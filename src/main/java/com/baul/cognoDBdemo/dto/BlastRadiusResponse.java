package com.baul.cognoDBdemo.dto;

import java.util.List;

public record BlastRadiusResponse(
        String target,
        List<AffectedServiceResponse> affectedServices,
        List<OwnerResponse> owners,
        List<RegionResponse> regions,
        List<AlternativePathResponse> alternatives
) {
}