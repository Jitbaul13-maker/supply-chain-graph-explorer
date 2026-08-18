package com.baul.cognoDBdemo.tmp;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

public class CognoConnectionTest {

    public static void main(String[] args) {

        String uri = System.getenv("COGNODB_URI");
        String username = System.getenv("COGNODB_USERNAME");
        String password = System.getenv("COGNODB_PASSWORD");

        try (Driver driver = GraphDatabase.driver(
                uri,
                AuthTokens.basic(username, password))) {

            driver.verifyConnectivity();

            System.out.println("CONNECTED TO COGNODB");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}