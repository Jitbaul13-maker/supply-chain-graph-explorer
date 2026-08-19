# Supply-Chain Impact Explorer

A graph-powered dependency intelligence tool for exploring how a dependency failure propagates through services, teams, environments, and regions.

## 🚀 Live Demo

**[Open the live application](https://supply-chain-graph-explorer-production.up.railway.app/)**

The live deployment is backed by CognoDB and uses the large seeded graph dataset.

### What you can explore

- Search services and dependencies
- Trace multi-hop dependency impact
- Identify affected services
- Identify incident severity
- Identify responsible teams
- Inspect affected environments and regions
- Discover alternative or mitigation paths

## Screenshots

### 1. Dashboard — Search
The entry point. A user searches for a service or dependency by name.

![Dashboard search](docs/screenshots/01-dashboard-search.png)

### 2. Search Results
Services and dependencies matching the query, ready for selection.

![Search results](docs/screenshots/02-search-results.png)

### 3. Impact Overview — Blast Radius
The core screen. Affected services grouped by hop distance from the
selected dependency.

![Blast radius view](docs/screenshots/03-blast-radius.png)

### 4. Responsible Owners, Deployment & Incident Context
Responsible teams, deployment footprint, and incident context for directly affected services.

![Owners and regions](docs/screenshots/04-owners-regions.png)

### 5. Alternative / Mitigation Paths
Related dependencies that could serve as fallback options.

![Alternatives](docs/screenshots/05-alternatives.png)

### 6. Empty State
Result when a dependency has no downstream impact.

![Empty state](docs/screenshots/06-empty-state.png)

### 7. Error State
Behavior when CognoDB is unreachable.

![Error state](docs/screenshots/07-error-state.png)

## 🎥 Demo

A short walkthrough of the application demonstrating dependency search,
blast-radius analysis, responsible owners, deployment impact,
incident context, alternative paths, and the overall impact-analysis workflow.

**[▶️ Watch the demo](https://drive.google.com/file/d/15TbVEYcVjCQZW-EyAIAvCNqyaqOcqmnG/view?usp=sharing)**

## Tech Stack

- **Java 21**
- **Spring Boot**
- **Spring MVC**
- **Thymeleaf**
- **Neo4j Java Driver**
- **CognoDB**
- **openCypher**
- **Docker**
- **Railway**
- **HTML / CSS / JavaScript**

## Problem

In a service-oriented system, a dependency failure rarely affects only the dependency itself.

A single unavailable dependency can propagate through multiple services and create a wider operational impact across:

- downstream services
- responsible teams
- deployment environments
- geographic regions
- alternative dependency paths

The difficult part is not storing these entities. The difficult part is understanding the relationships between them and answering questions such as:

> If this dependency becomes unavailable, which services are affected, how far does the impact propagate, which teams own those services, where are they deployed, and are there alternative paths?

CognoDB Impact Explorer models these relationships as a graph and uses graph traversal to answer these questions.

## Why a Graph Database?

The core problem is relationship-heavy.

A dependency can affect a service, which can affect another service, which can be deployed in an environment, which runs in a region, while each affected service is owned by a team.

Conceptually, the traversal looks like:

```text
Dependency
    ↓
Service
    ↓
Service
    ↓
Environment
    ↓
Region

```

with ownership and alternative paths attached to the graph.

In a relational design, answering the same questions would require repeatedly joining dependency, service, ownership, deployment, environment, region, and mitigation tables.

A graph database represents these relationships directly.

This makes multi-hop traversal a first-class operation rather than reconstructing the relationship network through repeated relational joins.

### What the graph enables

The application can answer:

1. Which services directly depend on a dependency?
2. Which downstream services can be affected across multiple hops?
3. Which teams own those services?
4. Which environments and regions are affected?
5. Are there alternative or mitigation paths?

These are graph traversal questions, which makes a graph-oriented data model a natural fit.

## Why CognoDB?

CognoDB is a graph database designed for relationship-centric applications and supports querying graph data using openCypher.

It fits this project because the core operations are graph traversals rather than simple record lookups. The application needs to follow relationships between dependencies and services, traverse downstream services across multiple hops, and then retrieve related ownership, deployment, regional, and alternative-path information.

The application communicates with CognoDB using the Neo4j Java Driver over the Bolt protocol. This allows the Spring Boot backend to execute parameterized Cypher queries directly against the graph.

### Why not a relational database?

A relational database could represent the same entities using tables and foreign keys. However, the primary operations in this application are relationship traversals:

- Find services affected by a dependency.
- Traverse downstream dependencies across multiple hops.
- Identify owners of affected services.
- Determine affected environments and regions.
- Find alternative dependency paths.

With a relational approach, these operations would require repeatedly joining relationship tables as traversal depth increases.

With a graph database, the relationships are represented directly as edges in the graph, and Cypher can express the traversal more naturally.

For example, the blast-radius query can start at a dependency and traverse downstream service relationships up to a defined depth:
```text
Dependency → Service → Service → Service
```
The resulting services form the dependency's potential blast radius.

## Architecture

The application follows a simple layered architecture:

```text
                    ┌──────────────────────────────┐
                    │          Web UI              │
                    │   Thymeleaf + JavaScript     │
                    └──────────────┬───────────────┘
                                   │ HTTP / JSON
                                   ▼
                    ┌──────────────────────────────┐
                    │      Spring Boot API         │
                    │                              │
                    │  GraphController             │
                    │          ↓                   │
                    │  GraphService                │
                    │          ↓                   │
                    │  GraphRepository             │
                    └──────────────┬───────────────┘
                                   │
                            Neo4j Java Driver
                                   |
                               Bolt protocol
                                   │
                                   ▼
                    ┌──────────────────────────────┐
                    │          CognoDB             │
                    │                              │
                    │  Services                    │
                    │  Dependencies                │
                    │  Teams                       │
                    │  Environments / Regions      │
                    │  Relationships               │
                    └──────────────────────────────┘
```
## Request flow

### A typical request follows this path:

- The user searches for a service or dependency through the web UI.
- The frontend sends an HTTP request to the Spring Boot REST API.
- GraphController receives the request and delegates the operation to GraphService.
- GraphService coordinates the required graph operations.
- The repository layer executes parameterized Cypher queries through the Neo4j Java Driver.
- CognoDB performs the graph traversal and returns the results.
- The backend converts the results into JSON.
- The frontend uses the response to update the interface.

This separation keeps HTTP handling, application logic, and graph queries independent from each other.

## Graph Model

The application models the supply-chain dependency landscape as a connected graph.

```text

                                                         ┌───────────────┐
                                                         │    Team       |
                                                         |   id: string  |
                                                         |      name     |
                                                         │               |
                                                         └──────┬────────┘
                                                                │ OWNED_BY
                                                                │ (reverse: Service → Team)
                                                                │
                    ┌─────────────┐                             ▼
                    │ Dependency  │                      ┌─────────────┐            ┌───────────────────┐
                    |             |                      |             |            |                   |         
                    │             │  SERVICE_DEPENDS_ON  │   Service   │            |   Incident        |
                    │ id: string  │ ◄─────────────────   │             │◄───────────|                   |
                    │ name        │                      │  id: string │  AFFECTS   |   id: string      |
                    │ type        │                      │      name   │            |    severity       |
                    └─────────────┘                      └──────┬──────┘            └───────────────────┘
                       ▲       |                                │
                       |       |                     DEPLOYED_IN│
                       |_______|                                ▼
                    ALTERNATIVE_TO                       ┌─────────────┐
                    (alt path)                           │  Environment│
                                                         │   name      │
                                                         │   (Prod/    │
                                                         │   Sandbox)  │
                                                         └──────┬──────┘
                                                                │ RUNS_ON
                                                                ▼
                                                        ┌─────────────┐
                                                        │   Region    │
                                                        │    name     │
                                                        └─────────────┘

Legend:
  (Service)-[:SERVICE_DEPENDS_ON]->(Service)                   downstream service dependency
  (Service)-[:SERVICE_DEPENDS_ON]->(Dependency)                service dependency
  (Service)-[:OWNED_BY]->(Team)                                ownership
  (Service)-[:DEPLOYED_IN]->(Environment)                      deployment context
  (Environment)-[:RUNS_ON]->(Region)                           geographic placement
  (Dependency)-[:ALTERNATIVE_TO|RELATED_TO]->(Dependency)      alternative / mitigation path
  (Incident)-[:AFFECTS]->(Service)                             Immediate impact
```

### Nodes

| Node | Description |
|---|---|
| `Service` | An application or service that participates in the dependency chain. |
| `Dependency` | An external or shared component that services depend on. |
| `Incident` | An operational incident associated with a service, including its severity. |
| `Team` | The team responsible for a service. |
| `Environment` | The deployment environment, such as Production or Sandbox. |
| `Region` | The geographic region where a service is deployed. |

### Relationships

The graph connects these entities through relationships representing dependency, ownership, and deployment information.

At the core of the model is the dependency chain:

```text
                    ┌───> Team
                    │
Dependency → Service ───> Environment
               ▲    │          │
               |    │          ▼
           Incident │        Region
                    │
                    └───> Downstream Service
```
This structure allows the application to traverse the dependency graph and then associate the affected services with their owners, deployment environments, regions, and alternative dependencies.

### Graph Traversal

For blast-radius analysis, the application starts from a selected dependency and follows downstream service relationships up to a defined number of hops.

```text
Dependency 001
      │
      ▼
Service 001       hop 0
      │
      ▼
Service 002       hop 1
      │
      ▼
Service 003       hop 2
      │
      ▼
Service 004       hop 3
```
The resulting services represent the potential impact of the dependency failure.

The application then enriches this impact set with ownership, deployment-region, environment, and alternative-path information.

## Core Cypher Queries

The application uses parameterized Cypher queries to perform the main graph operations behind search, dependency impact analysis, ownership, deployment impact, alternatives, and incident context.

These queries operate directly on the graph relationships defined by the application data model.

### Understanding Labels and Variables

Cypher uses variables such as `n`, `d`, `s`, `t`, `e`, and `r` to refer to nodes in a query.

For example:

```cypher
(d:Dependency)
```

means:

- d is the variable used to refer to the node
- Dependency is the node label

```cypher
Similarly:
(s:Service)
```
means that s refers to a node labelled Service.

The variable names themselves are arbitrary. The labels are what tell CognoDB which type of node is being matched.

1. Search Services and Dependencies

The search query allows the UI to search both Service and Dependency nodes using a single endpoint.
```cypher
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
```

How it works:

    MATCH (n) considers nodes in the graph.

The WHERE clause restricts the results to nodes labelled either Service or Dependency:

```cypher
n:Service OR n:Dependency
```

The query then performs a case-insensitive partial match against the node's name:

```cypher
toLower(n.name) CONTAINS toLower($query)
```

The CASE expression determines whether the returned node is a service or dependency so that the frontend can display the appropriate result type.

The query is limited to 20 results to keep the search response small and responsive.

### 2. Direct Dependency Consumers

The fundamental dependency relationship in the graph is:

    Service ──SERVICE_DEPENDS_ON──> Dependency

Therefore, when starting from a Dependency and looking for the services that depend on it, the relationship is traversed in the reverse direction.

```cypher
MATCH (d:Dependency {id: $dependencyId})
      <-[:SERVICE_DEPENDS_ON]-
      (s:Service)
RETURN s.name AS service
ORDER BY service
```

How it works

The query first identifies the selected dependency:
```cypher
(d:Dependency {id: $dependencyId})
```

It then follows incoming SERVICE_DEPENDS_ON relationships to find services that directly depend on that dependency:
```cypher
<-[:SERVICE_DEPENDS_ON]-
```
This gives the immediate consumers of the dependency.

For example:

    Service 012 ──SERVICE_DEPENDS_ON──> Dependency 013
    Service 013 ──SERVICE_DEPENDS_ON──> Dependency 013

If Dependency 013 becomes unavailable, these services are immediately exposed to the failure.

This direct-consumer relationship is also used by the owner, deployment, and incident queries described below.

### 3. Dependency Traversal / Blast Radius

The blast-radius query determines how far the impact of a dependency can propagate through the service dependency graph.

The graph traversal is:
```text
Dependency
    ↓
Direct Service
    ↓
Downstream Service
    ↓
Downstream Service
```
The application traverses downstream service relationships up to three hops.
```cypher
MATCH (d:Dependency {id: $dependencyId})
      <-[:SERVICE_DEPENDS_ON]-
      (s:Service)


MATCH path =
      (s)-[:SERVICE_DEPENDS_ON*0..3]->
      (downstream:Service)


WITH downstream,
     min(length(path)) AS hops


RETURN downstream.name AS service,
       hops
ORDER BY hops, service
```

How it works

The first part finds services that directly depend on the selected dependency:
```cypher
(d:Dependency)
<-[:SERVICE_DEPENDS_ON]-
(s:Service)
```

The second part follows service-to-service dependency relationships:
```cypher
(s)-[:SERVICE_DEPENDS_ON*0..3]->(downstream:Service)
```
The *0..3 is a variable-length relationship pattern.

It means the query can traverse from zero to three relationship hops.

The query then calculates the shortest discovered path to each affected service:
```cypher
min(length(path))
```
The result therefore contains both the affected service and its hop distance.

For example:
```text
Dependency 013
      │
      ├── Service 012     0 hops
      ├── Service 013     0 hops
      ├── Service 062     0 hops
      ├── Service 063     0 hops
      │
      └── Service 014     1 hop
```
A zero-hop service is a direct consumer of the dependency. Higher hop counts represent downstream propagation.

Why this query is useful for a graph database

The important part of this operation is the variable-length traversal:
```cypher
[:SERVICE_DEPENDS_ON*0..3]
```
The number of downstream services is not fixed. One dependency may affect a few services while another may propagate through a much larger service chain.

In a relational schema, the same question can require either:

- Multiple self-joins on a service-dependency table, with one join for each known hop depth, or
- A recursive CTE to repeatedly traverse the dependency relationships.

A graph database expresses this relationship traversal directly as part of the query.

This is particularly useful for impact analysis because the question is fundamentally about connected paths through a dependency graph, rather than simply retrieving rows from one table.

### 4. Responsible Owners

Once the directly affected services are identified, the graph follows ownership relationships to determine which teams are responsible for those services.

The relationship is:
```text
Service ──OWNED_BY──> Team
```
```cypher
MATCH (d:Dependency {id: $dependencyId})
      <-[:SERVICE_DEPENDS_ON]-
      (s:Service)
      -[:OWNED_BY]->
      (t:Team)


RETURN DISTINCT
       s.name AS service,
       t.name AS owner
ORDER BY service
``` 
How it works

The query first finds services that directly depend on the selected dependency.

It then follows:
```cypher
-[:OWNED_BY]->
```
to the team responsible for each service.

The result is displayed in the UI as Responsible Owners.

These are the teams associated with the services that are immediately exposed to the dependency failure and therefore are the primary teams to page when the dependency becomes unavailable.

Example:
```text
Service 012 → Team 12
Service 013 → Team 13
Service 062 → Team 02
Service 063 → Team 03
```
The owner query intentionally focuses on direct consumers rather than every downstream service in the complete blast radius.

### 5. Deployment Impact

The graph also models where services are deployed.

The deployment relationship is:
```text
Service
   ↓ DEPLOYED_IN
Environment
   ↓ RUNS_ON
Region
```
The query follows that path:
```cypher
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
ORDER BY service
```

How it works

The query starts with services that directly depend on the selected dependency.

It then follows:
```text
Service → Environment → Region
```
to determine where those services are running.

The result allows the UI to show deployment information such as:
```text
Service 012    Production · us-east
Service 013    Production · eu-west
Service 062    Sandbox    · us-east
```
This provides operational context around the dependency failure.

The number of deployment entries does not represent the total blast radius. The blast-radius query can identify downstream services that are several hops away, while this query focuses on deployment information for directly affected services.

### 6. Alternative Dependencies

The graph can connect dependencies through relationships representing
alternative or related paths.

The application supports both `RELATED_TO` and `ALTERNATIVE_TO` relationships:
```text
Dependency ──RELATED_TO──────> Dependency
Dependency ──ALTERNATIVE_TO──> Dependency
```
The application uses this relationship to identify potential alternative or mitigation paths.
```cypher
MATCH (d1:Dependency {id: $dependencyId})
      -[r:RELATED_TO|ALTERNATIVE_TO]-
      (d2:Dependency)

RETURN DISTINCT
       d1.name AS fromDependency,
       d2.name AS toDependency,
       type(r) AS relationship
ORDER BY toDependency
```

How it works

The query starts from the selected dependency and follows its `RELATED_TO` or `ALTERNATIVE_TO` relationships.

For example:
```text
Dependency 013
      │
      └──ALTERNATIVE_TO──> Dependency 021
```
This allows the application to expose possible alternatives that could be considered if the selected dependency becomes unavailable.

The graph relationship makes this a direct neighborhood lookup rather than requiring a separate mapping structure.

### 7. Incident Context

The graph also contains operational incident information.

The relationship is:
```text
Incident ──AFFECTS──> Service
```
The application uses this information to show incident context for services that directly consume the selected dependency.
```cypher
MATCH (d:Dependency {id: $dependencyId})
      <-[:SERVICE_DEPENDS_ON]-
      (s:Service)
      <-[:AFFECTS]-
      (i:Incident)


RETURN DISTINCT
       s.name AS service,
       i.name AS incident,
       i.severity AS severity
ORDER BY service, incident
```
How it works

The query first finds services that directly depend on the selected dependency.

It then follows the AFFECTS relationship in reverse to find incidents associated with those services.

The returned severity can be:
```text
LOW
MEDIUM
HIGH
CRITICAL
```
The UI displays the incident alongside the corresponding service in the Blast Radius view.

There is an important distinction between incident context and the blast radius:

- The blast-radius query identifies the complete downstream impact of the dependency.
- The incident query focuses on incidents associated with the services directly consuming the dependency.

This allows the UI to distinguish between immediate operational context and the broader potential propagation of the failure.

### 8. Aggregated Overview

The application combines these graph operations into a single dependency overview request.

The overview response contains:

- Affected services and hop distance
- Responsible owners
- Deployment environments and regions
- Alternative dependencies
- Incident context for directly affected services

Conceptually, the application performs:
```text

                         ┌──> Affected Services
                         │
                         ├──> Responsible Owners
Dependency ── Overview ──┼──> Deployment Impact
                         │
                         ├──> Alternative Dependencies
                         │
                         └──> Incident Context
```
This allows the frontend to retrieve the complete impact analysis with a single API request rather than making separate requests for every panel.

The result is a single operational view of a dependency:
```text
Dependency
    │
    ├── Direct consumers
    │       ├── Responsible teams
    │       ├── Deployment locations
    │       └── Incident context
    │
    ├── Downstream services
    │       └── Blast radius
    │
    └── Alternative dependencies
```
This is the central graph-oriented workflow of the application: starting from one dependency and traversing multiple relationship types to derive operationally useful information from the connected data.

## REST API

The backend exposes a small REST API under `/api/graph`.

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/graph/search?q={query}` | Search for services and dependencies |
| `GET` | `/api/graph/dependencies/{serviceId}` | Retrieve dependencies associated with a service |
| `GET` | `/api/graph/blast-radius/{dependencyId}` | Calculate the downstream impact of a dependency |
| `GET` | `/api/graph/owners/{dependencyId}` | Find teams associated with affected services |
| `GET` | `/api/graph/regions/{dependencyId}` | Find affected environments and regions |
| `GET` | `/api/graph/alternatives/{dependencyId}` | Find alternative or related dependencies |
| `GET` | `/api/graph/overview/{dependencyId}` | Return the complete dependency impact analysis |

### Aggregated Overview

The primary UI workflow uses the overview endpoint:

```text
GET /api/graph/overview/{dependencyId}
```

The response combines the results of the graph analysis into a single JSON document:

```json
{
  "target": "dependency:dep-001",
  "affectedServices": [],
  "owners": [],
  "regions": [],
  "alternatives": [],
  "incidents": []
}
```

This allows the frontend to render the complete impact analysis without making separate HTTP requests for each section.

### API Design

The controllers are intentionally thin. HTTP request handling is separated from the graph traversal and application logic:

```text
GraphController
      ↓
GraphService
      ↓
GraphRepository
      ↓
CognoDB
```

The graph-specific logic remains in Cypher queries rather than being embedded in the HTTP layer.

## UI Flow

The application is designed around a simple dependency-impact investigation workflow.

### 1. Search

The user begins by searching for a service or dependency.

```text
User
 ↓
Search bar
 ↓
GET /api/graph/search
 ↓
Services / Dependencies
```
The search results allow the user to identify the dependency or service they want to investigate.

### 2. Select a Dependency

After selecting a dependency, the application retrieves the dependency information associated with the selected item.

The user can then initiate an impact analysis.

### 3. Impact Analysis

The application requests the aggregated overview:
```
GET /api/graph/overview/{dependencyId}
```
The backend performs the required graph traversals against CognoDB and returns the complete analysis.

### 4. Explore the Impact

The UI presents the results in separate sections:

- **Affected Services** — services within the calculated blast radius and their hop distance.
- **Incidents** - Immediate operational incidents associated with directly affected services.
- **Owners** — teams responsible for affected services.
- **Deployments** — deployment locations and environments affected.
- **Alternatives** — related or alternative dependency paths.

This allows a user to move from a single dependency to an operational view of its potential impact without manually querying the graph.

## Seed Data & Database Initialization

The application uses a large Cypher seed script to populate the CognoDB graph with the services, dependencies, teams, environments, regions, and relationships required for the demonstration.

The seed script is located at:

```text
src/main/resources/scripts/seed-large.cypher
```

### Database Seeding

Database seeding is intentionally kept separate from the normal application startup flow.

The deployed Spring Boot application connects to the already-populated CognoDB instance and does not automatically execute the seed script on startup. This prevents the dataset from being re-created whenever the application restarts or is redeployed.

For reference, the project documentation contains an example SeedRunner implementation showing how the Cypher seed can be loaded from the application classpath and executed through the Neo4j Java Driver.

See the [Database Seeding Documentation](docs/database-seeding.md) for the implementation and initialization details.

The documented runner is intended as a development/reference approach for initializing a fresh database rather than as part of the production application startup path.

### About the Large Seed Dataset

The large demonstration dataset uses a deterministic and structured dependency topology so that graph traversals produce reproducible results.

Each dependency is connected to a consistent set of direct consumer services, while services are connected through a regular downstream dependency pattern. This allows the application to demonstrate bounded multi-hop traversal consistently across the dataset.

As a result, some summary counts may be identical for multiple dependencies. These counts are calculated from the seeded graph topology by the application's Cypher queries; they are not hard-coded in the application.

### Recommended Initialization Flow

For a fresh CognoDB instance:
```text
seed-large.cypher
       ↓
CognoDB
       ↓
Spring Boot application
       ↓
REST API / UI
```

## Local Development

### Prerequisites

- Java 21
- Maven
- Docker
- Access to a CognoDB instance
- Git

### 1. Clone the repository

```bash
git clone https://github.com/Jitbaul13-maker/supply-chain-graph-explorer.git
cd supply-chain-graph-explorer
```

### 2. Creating a CognoDB Instance

For a fresh setup, create a CognoDB instance through the CognoDB Cloud console.

1. Sign in to CognoDB Cloud.
2. Create a new graph database instance.
3. Wait for the instance to become available.
4. Open the instance details and copy the Bolt connection URI.
5. Note the database username and password.
6. Configure these values as environment variables:

### 3. Configure environment variables

Create a `.env` file for local development using the credentials from the CognoDB instance:
```text
COGNODB_URI=<your-cognodb-bolt-uri>
COGNODB_USERNAME=<your-cognodb-username>
COGNODB_PASSWORD=<your-cognodb-password>
```
The .env file is intentionally excluded from version control.

### 4. Populate CognoDB

For a fresh database, execute the provided seed-large.cypher dataset against CognoDB.

The documented SeedRunner example in Database Seeding Documentation demonstrates one way to execute the seed through the Neo4j Java Driver.

### 5. Run the application

The application can be started using Maven:

```bash
mvn spring-boot:run
```
If port 8080 is already occupied in the local environment, run the application on port 8081:
```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=8081" \
  -Dspring-boot.run.jvmArguments="-Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true"
```
The application will then be available at:
```text
http://localhost:8081
```
### Running with Docker

The project also includes a Docker configuration for running the application as a container.
```bash
docker compose up --build
```
The Docker configuration uses the platform-provided PORT when available, while retaining 8081 as the local fallback.

### Environment Variables

The application reads the following variables from the environment:

| Variable | Purpose|
|----------|--------|
| COGNODB_URI |	CognoDB Bolt connection URI|
| COGNODB_USERNAME | CognoDB username|
| COGNODB_PASSWORD | CognoDB password|

Credentials should never be committed to the repository.

## Deployment

The application is containerized using Docker and deployed as a Spring Boot service on Railway.

### Deployment Architecture

```text
GitHub
   │
   │ push to main
   ▼
Railway
   │
   ▼
Docker build
   │
   ▼
Spring Boot application
   │
   ▼
CognoDB
```

### Environment Variables

Production credentials are not stored in the repository.

Railway provides the following environment variables to the application at runtime:
```text
COGNODB_URI
COGNODB_USERNAME
COGNODB_PASSWORD
```
The application reads these values from the environment and uses them to establish the Bolt connection to CognoDB.

### Port Configuration

Railway provides the application port through the PORT environment variable.

The Docker entrypoint uses the platform-provided port when available and falls back to port 8081 for local execution.

```text
Railway
  PORT → Spring Boot

Local
  PORT not set → 8081
```

### Continuous Deployment

The repository is connected to Railway through the Railway GitHub App.

Commits pushed to the configured main branch can therefore trigger a new deployment automatically.

## Project Structure

```text
src/
├── main/
│   ├── java/com/baul/cognoDBdemo/
│   │   ├── config/
│   │   │   └── CognoDbConfig.java
│   │   │
│   │   ├── controller/
│   │   │   ├── GraphController.java
│   │   │   └── PageController.java
│   │   │
│   │   ├── repository/
│   │   │   └── GraphRepository.java
│   │   │
│   │   ├── service/
│   │   │   └── GraphService.java
│   │   │
│   │   └── CognoDBdemoApplication.java
│   │
│   └── resources/
│       ├── scripts/
│       │   └── seed-large.cypher
│       │
│       ├── static/
│       │   ├── css/
│       │   └── js/
│       │
│       ├── templates/
│       │   └── index.html
│       │
│       └── application.properties
│
├── docs/
│   └── database-seeding.md
│
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

### Package Responsibilities

| Component | Responsibility |
|-----------|----------------|
| controller |	Handles HTTP requests and returns API/page responses |
| service |	Coordinates application and graph operations |
| repository |	Contains Cypher queries and communicates with CognoDB |
| config |	Creates and configures the CognoDB/Neo4j driver |
| templates |	Thymeleaf UI |
| static |	Frontend JavaScript and CSS |
| scripts |	Graph seed data |
| docs |	Development and database-seeding documentation |

## Engineering Decisions

### Graph-first data model

The application models the dependency landscape as a graph because the primary operations involve traversing relationships between services and dependencies.

Rather than reconstructing these relationships through multiple relational joins, the application uses Cypher to express the required traversals directly.

### Parameterized Cypher

Graph queries use parameters for user-supplied values instead of constructing Cypher strings through concatenation.

This keeps the query structure separate from input values and provides a safer and cleaner query interface.

### Layered backend

The application separates responsibilities across:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
CognoDB
```

This keeps HTTP handling, application orchestration, and graph-specific queries independent.

### Aggregated overview API

The UI primarily uses a single overview endpoint for dependency-impact analysis.

The backend combines the required graph operations into one response containing affected services, owners, deployment information, alternatives, and incident context.

This avoids forcing the frontend to make several separate requests to render one impact-analysis view.

### Production seeding is separated from application startup

The large graph dataset is initialized separately from the normal application startup lifecycle.

The documented SeedRunner demonstrates how the dataset can be loaded for development or fresh database initialization, while the deployed application connects to the existing populated CognoDB instance without reseeding it on every restart.

### Environment-based configuration

Database connection details are supplied through environment variables rather than being committed to source control.

This keeps local development configuration and production secrets separate from the application code.
