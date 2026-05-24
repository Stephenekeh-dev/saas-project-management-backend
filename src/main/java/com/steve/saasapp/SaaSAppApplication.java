package com.steve.saasapp;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SaaSAppApplication {

	public static void main(String[] args) {

		// Load .env file
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMalformed()
				.ignoreIfMissing()
				.load();

		// Database
		System.setProperty("SPRING_DATASOURCE_URL",
				dotenv.get("SPRING_DATASOURCE_URL"));

		System.setProperty("SPRING_DATASOURCE_USERNAME",
				dotenv.get("SPRING_DATASOURCE_USERNAME"));

		System.setProperty("SPRING_DATASOURCE_PASSWORD",
				dotenv.get("SPRING_DATASOURCE_PASSWORD"));

		// Hibernate / Flyway
		System.setProperty("SPRING_JPA_HIBERNATE_DDL_AUTO",
				dotenv.get("SPRING_JPA_HIBERNATE_DDL_AUTO", "validate"));

		System.setProperty("SPRING_FLYWAY_ENABLED",
				dotenv.get("SPRING_FLYWAY_ENABLED", "true"));

		// JWT
		System.setProperty("APP_JWT_SECRET",
				dotenv.get("APP_JWT_SECRET"));

		System.setProperty("APP_JWT_EXPIRATION",
				dotenv.get("APP_JWT_EXPIRATION", "86400000"));

		SpringApplication.run(SaaSAppApplication.class, args);
	}
}