package com.baul.cognoDBdemo.dto;

public record AlternativePathResponse(
        String fromDependency,
        String toDependency,
        String relationship
) {
}