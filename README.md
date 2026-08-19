# CognoDB Impact Explorer

A graph-powered dependency intelligence tool for exploring how a dependency failure propagates through services, teams, environments, and regions.

## 🚀 Live Demo

**[Open the live application](https://supply-chain-graph-explorer-production.up.railway.app/)**

The live deployment is backed by CognoDB and uses the large seeded graph dataset.

### What you can explore

- Search services and dependencies
- Trace multi-hop dependency impact
- Identify affected services
- Identify responsible teams
- Inspect affected environments and regions
- Discover alternative or mitigation paths

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

Dependency → Service → Service → Service

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
          / Bolt protocol
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

### Nodes

| Node | Description |
|---|---|
| `Service` | An application or service that participates in the dependency chain. |
| `Dependency` | An external or shared component that services depend on. |
| `Team` | The team responsible for a service. |
| `Environment` | The deployment environment, such as Production or Sandbox. |
| `Region` | The geographic region where a service is deployed. |

### Relationships

The graph connects these entities through relationships representing dependency, ownership, and deployment information.

At the core of the model is the dependency chain:

```text
Dependency
     │
     ▼
 Service
     │
     ▼
 Service
     │
     ▼
 Service
```
Additional relationships provide operational context:

```text
                    ┌───> Team
                    │
Dependency → Service ───> Environment
                    │          │
                    │          ▼
                    │        Region
                    │
                    └───> Dependency
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

The application uses parameterized Cypher queries to perform the main graph operations.

### 1. Search

Searches both `Service` and `Dependency` nodes by name.

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

The query allows the UI to search across both services and dependencies using a single endpoint.

### 2. Dependency Traversal / Blast Radius

Starting from a dependency, the application traverses downstream service relationships up to three hops.

The traversal identifies the services that can potentially be affected by the dependency.

```text
Dependency
    ↓
Service
    ↓
Service
    ↓
Service
```
The result includes the affected service and its calculated hop distance.

### 3. Affected Owners

Once the affected services are identified, the graph is traversed through ownership relationships to determine the responsible teams.
```text
Affected Service ──OWNED_BY──> Team
```

### 4. Affected Regions

The application follows deployment relationships to determine where affected services are running.

```text
Service
   ↓
Environment
   ↓
Region
```

This allows the application to distinguish impacts such as Production vs. Sandbox and regional deployment differences.

### 5. Alternative Dependencies

The graph also models relationships between dependencies that can represent alternative or related paths.

```text
Dependency 001 ──RELATED_TO──> Dependency 002
```

The application exposes these relationships as potential alternatives or mitigation paths.

### 6. Aggregated Overview

The overview operation combines the graph analysis into a single response containing:

affected services and hop distance
responsible teams
affected environments and regions
alternative dependencies

This allows the frontend to retrieve the complete impact analysis with a single API request.

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
  "alternatives": []
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

Affected Services — services within the calculated blast radius and their hop distance.
Owners — teams responsible for affected services.
Regions & Environments — deployment locations and environments affected.
Alternatives — related or alternative dependency paths.

This allows a user to move from a single dependency to an operational view of its potential impact without manually querying the graph.
