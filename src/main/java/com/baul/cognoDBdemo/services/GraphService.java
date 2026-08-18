package com.baul.cognoDBdemo.services;

import com.baul.cognoDBdemo.dto.AffectedServiceResponse;
import com.baul.cognoDBdemo.dto.AlternativePathResponse;
import com.baul.cognoDBdemo.dto.BlastRadiusResponse;
import com.baul.cognoDBdemo.dto.GraphNodeResponse;
import com.baul.cognoDBdemo.dto.OwnerResponse;
import com.baul.cognoDBdemo.dto.RegionResponse;
import com.baul.cognoDBdemo.exception.GraphNotFoundException;
import com.baul.cognoDBdemo.repo.GraphRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GraphService {

    private final GraphRepository graphRepository;

    public GraphService(GraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }

    private void validateDependency(String dependencyId) {

        if (!graphRepository.dependencyExists(dependencyId)) {
            throw new GraphNotFoundException(
                    "Dependency not found: " + dependencyId
            );
        }
    }

    public List<GraphNodeResponse> search(String query) {
        return graphRepository.search(query);
    }

    public List<GraphNodeResponse> getDependencies(String serviceId) {
        return graphRepository.findDirectDependencies(serviceId);
    }

    public List<AffectedServiceResponse> getBlastRadius(String dependencyId) {
        validateDependency(dependencyId);
        return graphRepository.findBlastRadius(dependencyId);
    }

    public List<OwnerResponse> getAffectedOwners(String dependencyId) {
        validateDependency(dependencyId);
        return graphRepository.findAffectedOwners(dependencyId);
    }

    public List<RegionResponse> getAffectedRegions(String dependencyId) {
        validateDependency(dependencyId);
        return graphRepository.findAffectedRegions(dependencyId);
    }

    public List<AlternativePathResponse> getAlternatives(String dependencyId) {
        validateDependency(dependencyId);
        return graphRepository.findAlternatives(dependencyId);
    }

    public BlastRadiusResponse getBlastRadiusOverview(String dependencyId) {

        validateDependency(dependencyId);

        List<AffectedServiceResponse> affectedServices =
                graphRepository.findBlastRadius(dependencyId);

        List<OwnerResponse> owners =
                graphRepository.findAffectedOwners(dependencyId);

        List<RegionResponse> regions =
                graphRepository.findAffectedRegions(dependencyId);

        List<AlternativePathResponse> alternatives =
                graphRepository.findAlternatives(dependencyId);

        return new BlastRadiusResponse(
                dependencyId,
                affectedServices,
                owners,
                regions,
                alternatives
        );
    }
}