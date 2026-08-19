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
