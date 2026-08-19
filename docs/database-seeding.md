# Database Seeding

## Purpose

The project includes a reusable Cypher seed script for initializing the CognoDB graph with the dataset required by the application.

The seed file is:

```text
scripts/seed-large.cypher
```

The seed data contains services, dependencies, teams, environments, regions, and their relationships.

The deployed Spring Boot application does **not** automatically execute the seed script during startup. CognoDB is treated as the external source of truth for graph data.

---

## Recommended Seeding Workflow

For a fresh CognoDB environment:

```text
seed-large.cypher
        ↓
Execute against CognoDB
        ↓
Graph populated
        ↓
Start Spring Boot application
        ↓
Application queries CognoDB
```

This keeps database initialization separate from application startup and prevents the application from automatically modifying or reseeding the graph whenever the service restarts.

---

## Development SeedRunner Example

During development, a `SeedRunner` implementation was used to automate execution of the seed script during Spring Boot startup.

The implementation is shown below as a reference:

```java
package com.baul.cognoDBdemo.seed;

import org.neo4j.driver.Driver;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class SeedRunner implements CommandLineRunner {

    private final Driver driver;

    public SeedRunner(Driver driver) {
        this.driver = driver;
    }

    @Override
    public void run(String... args) throws Exception {

        var resource = new ClassPathResource("scripts/seed-large.cypher");

        String script = Files.readString(
                resource.getFile().toPath(),
                StandardCharsets.UTF_8
        );

        String cleanedScript = script.lines()
                .filter(line -> !line.trim().startsWith("//"))
                .reduce("", (a, b) -> a + b + "\n");

        try (var session = driver.session()) {

            for (String statement : cleanedScript.split(";")) {

                String cypher = statement.trim();

                if (cypher.isEmpty()) {
                    continue;
                }

                session.run(cypher).consume();
            }
        }
    }
}
```

### How it works

The runner:

1. Loads `seed-large.cypher` from the application classpath.
2. Reads the file using UTF-8 encoding.
3. Removes lines beginning with `//`.
4. Splits the resulting script into individual statements using `;`.
5. Ignores empty statements.
6. Executes each Cypher statement through the Neo4j Java Driver.

The `Driver` used here is the same CognoDB driver configured by the application.

---

## Why SeedRunner Is Not Enabled in Deployment

The development runner is intentionally not registered as a Spring component in the deployed application.

In particular, it does not use:

```java
@Component
```

This prevents the seed script from executing automatically every time Spring Boot starts.

The production flow is therefore:

```text
Railway
   ↓
Spring Boot starts
   ↓
Connect to CognoDB
   ↓
Query existing graph
   ↓
Return impact analysis
```

rather than:

```text
Railway
   ↓
Spring Boot starts
   ↓
Automatically execute seed script
   ↓
Modify CognoDB
```

This separation makes the application startup predictable and keeps graph initialization as an explicit setup operation.

---

## Important Note

The `SeedRunner` example uses simple semicolon-based statement splitting and is intended as a development/demo mechanism rather than a general-purpose Cypher migration system.

For this project, the reusable `seed-large.cypher` file remains the primary database initialization artifact.

The deployed application treats CognoDB as the source of truth and uses the graph for runtime dependency and impact analysis.
