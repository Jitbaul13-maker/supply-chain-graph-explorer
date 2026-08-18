package com.baul.cognoDBdemo.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CognoDbConfig {

    @Bean(destroyMethod = "close")
    public Driver cognoDbDriver() {

        String uri = System.getenv("COGNODB_URI");
        String username = System.getenv("COGNODB_USERNAME");
        String password = System.getenv("COGNODB_PASSWORD");

        return GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }

}
