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

Dependency
    ↓
Service
    ↓
Service
    ↓
Environment
    ↓
Region

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
