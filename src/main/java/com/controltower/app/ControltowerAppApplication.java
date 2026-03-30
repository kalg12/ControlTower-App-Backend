package com.controltower.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Control Tower — Multi-tenant SaaS platform for monitoring and managing
 * client systems (POS and third-party integrations).
 *
 * Architecture: modular monolith (Spring Modulith), event-driven internally.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class ControltowerAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(ControltowerAppApplication.class, args);
	}
}
