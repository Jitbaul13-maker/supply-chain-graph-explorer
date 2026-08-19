package com.baul.cognoDBdemo.repo;

import com.baul.cognoDBdemo.dto.AffectedServiceResponse;
import com.baul.cognoDBdemo.dto.AlternativePathResponse;
import com.baul.cognoDBdemo.dto.GraphNodeResponse;
import com.baul.cognoDBdemo.dto.OwnerResponse;
import com.baul.cognoDBdemo.dto.RegionResponse;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.neo4j.driver.Values.parameters;

@Repository
public class GraphRepository {

    private final Driver driver;

    public GraphRepository(Driver driver) {
        this.driver = driver;
    }

    // ---------------------------------------------------------
    // 1. SEARCH SERVICES / DEPENDENCIES
    // ---------------------------------------------------------

    public List<GraphNodeResponse> search(String query) {

        String cypher = """
                MATCH (n)
                WHERE (n:Service OR n:Dependency)
                  AND toLower(n.name) CONTAINS toLower($query)
                RETURN n.id AS id,
                       CASE
                           WHEN n:Service THEN 'Service'
                           WHEN n:Dependency THEN 'Dependency'
                       END AS type,
                       n.name AS name
                ORDER BY n.name
                LIMIT 20
                """;

        try (Session session = driver.session()) {

            return session.run(
                    cypher,
                    parameters("query", query)
            ).list(record -> new GraphNodeResponse(
                    record.get("id").asString(),
                    record.get("type").asString(),
                    record.get("name").asString()
            ));
        }
    }

    // ---------------------------------------------------------
    // 2. DIRECT DEPENDENCIES
    // ---------------------------------------------------------

    public List<GraphNodeResponse> findDirectDependencies(String serviceId) {

        String cypher = """
                MATCH (s:Service {id: $serviceId})
                      -[:SERVICE_DEPENDS_ON]->
                      (d:Dependency)
                RETURN d.id AS id,
                       'Dependency' AS type,
                       d.name AS name
                ORDER BY d.name
                """;

        try (Session session = driver.session()) {

            return session.run(
                    cypher,
                    parameters("serviceId", serviceId)
            ).list(record -> new GraphNodeResponse(
                    record.get("id").asString(),
                    record.get("type").asString(),
                    record.get("name").asString()
            ));
        }
    }

    // ---------------------------------------------------------
    // 3. MULTI-HOP BLAST RADIUS
    // ---------------------------------------------------------

    public List<AffectedServiceResponse> findBlastRadius(String dependencyId) {

        String cypher = """

                MATCH (d:Dependency {id: $dependencyId})
      <-[:SERVICE_DEPENDS_ON]-
                (s:Service)

        OPTIONAL MATCH path =
                (s)-[:SERVICE_DEPENDS_ON*0..3]->
        (downstream:Service)

        WITH downstream,
                CASE
        WHEN downstream = s THEN 0
        ELSE length(path)
                END AS hops

        WITH downstream, min(hops) AS hops

                RETURN
        downstream.name AS service,
                hops
        ORDER BY hops, service
""";
        try (Session session = driver.session()) {

            return session.run(
                    cypher,
                    parameters("dependencyId", dependencyId)
            ).list(record -> new AffectedServiceResponse(
                    record.get("service").asString(),
                    record.get("hops").asInt()
            ));
        }
    }

    // ---------------------------------------------------------
    // 4. AFFECTED OWNERS
    // ---------------------------------------------------------

    public List<OwnerResponse> findAffectedOwners(String dependencyId) {

        String cypher = """
                MATCH (d:Dependency {id: $dependencyId})
                      <-[:SERVICE_DEPENDS_ON]-
                      (s:Service)
                      -[:OWNED_BY]->
                      (t:Team)

                RETURN DISTINCT
                       s.name AS service,
                       t.name AS owner
                ORDER BY service
                """;

        try (Session session = driver.session()) {

            return session.run(
                    cypher,
                    parameters("dependencyId", dependencyId)
            ).list(record -> new OwnerResponse(
                    record.get("service").asString(),
                    record.get("owner").asString()
            ));
        }
    }

    // ---------------------------------------------------------
    // 5. AFFECTED ENVIRONMENTS / REGIONS
    // ---------------------------------------------------------

    public List<RegionResponse> findAffectedRegions(String dependencyId) {

        String cypher = """
                MATCH (d:Dependency {id: $dependencyId})
                      <-[:SERVICE_DEPENDS_ON]-
                      (s:Service)
                      -[:DEPLOYED_IN]->
                      (e:Environment)
                      -[:RUNS_ON]->
                      (r:Region)

                RETURN DISTINCT
                       s.name AS service,
                       e.name AS environment,
                       r.name AS region
                ORDER BY region, service
                """;

        try (Session session = driver.session()) {

            return session.run(
                    cypher,
                    parameters("dependencyId", dependencyId)
            ).list(record -> new RegionResponse(
                    record.get("service").asString(),
                    record.get("environment").asString(),
                    record.get("region").asString()
            ));
        }
    }

    // ---------------------------------------------------------
    // 6. ALTERNATIVE / MITIGATION PATHS
    // ---------------------------------------------------------

    public List<AlternativePathResponse> findAlternatives(String dependencyId) {

        String cypher = """
                MATCH (d1:Dependency {id: $dependencyId})
                      -[r:RELATED_TO|ALTERNATIVE_TO]-
                      (d2:Dependency)

                RETURN DISTINCT
                       d1.name AS fromDependency,
                       d2.name AS toDependency,
                       type(r) AS relationship
                ORDER BY toDependency
                """;

        try (Session session = driver.session()) {

            return session.run(
                    cypher,
                    parameters("dependencyId", dependencyId)
            ).list(record -> new AlternativePathResponse(
                    record.get("fromDependency").asString(),
                    record.get("toDependency").asString(),
                    record.get("relationship").asString()
            ));
        }
    }

    public boolean dependencyExists(String dependencyId) {

        String cypher = """
            MATCH (d:Dependency {id: $dependencyId})
            RETURN count(d) > 0 AS exists
            """;

        try (Session session = driver.session()) {
            return session.run(
                    cypher,
                    parameters("dependencyId", dependencyId)
            ).single().get("exists").asBoolean();
        }
    }
}