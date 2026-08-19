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
