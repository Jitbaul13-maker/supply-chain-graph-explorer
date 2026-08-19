package com.baul.cognoDBdemo.controller;

import com.baul.cognoDBdemo.dto.AffectedServiceResponse;
import com.baul.cognoDBdemo.dto.AlternativePathResponse;
import com.baul.cognoDBdemo.dto.BlastRadiusResponse;
import com.baul.cognoDBdemo.dto.GraphNodeResponse;
import com.baul.cognoDBdemo.dto.OwnerResponse;
import com.baul.cognoDBdemo.dto.RegionResponse;
import com.baul.cognoDBdemo.service.GraphService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/graph")
@CrossOrigin(origins = "*")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    // Search services/dependencies
    @GetMapping("/search")
    public List<GraphNodeResponse> search(
            @RequestParam String q
    ) {
        return graphService.search(q);
    }

    // Direct dependencies of a service
    @GetMapping("/dependencies/{serviceId}")
    public List<GraphNodeResponse> getDependencies(
            @PathVariable String serviceId
    ) {
        return graphService.getDependencies(serviceId);
    }

    // Multi-hop blast radius
    @GetMapping("/blast-radius/{dependencyId}")
    public List<AffectedServiceResponse> getBlastRadius(
            @PathVariable String dependencyId
    ) {
        return graphService.getBlastRadius(dependencyId);
    }

    // Owners affected by a dependency
    @GetMapping("/owners/{dependencyId}")
    public List<OwnerResponse> getAffectedOwners(
            @PathVariable String dependencyId
    ) {
        return graphService.getAffectedOwners(dependencyId);
    }

    // Regions/environments affected
    @GetMapping("/regions/{dependencyId}")
    public List<RegionResponse> getAffectedRegions(
            @PathVariable String dependencyId
    ) {
        return graphService.getAffectedRegions(dependencyId);
    }

    // Alternative/mitigation paths
    @GetMapping("/alternatives/{dependencyId}")
    public List<AlternativePathResponse> getAlternatives(
            @PathVariable String dependencyId
    ) {
        return graphService.getAlternatives(dependencyId);
    }

    // Complete blast-radius overview
    @GetMapping("/overview/{dependencyId}")
    public BlastRadiusResponse getOverview(
            @PathVariable String dependencyId
    ) {
        return graphService.getBlastRadiusOverview(dependencyId);
    }
}