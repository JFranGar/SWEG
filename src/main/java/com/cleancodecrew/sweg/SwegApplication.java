package com.cleancodecrew.sweg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SwegApplication {
	public static void main(String[] args) {
		SpringApplication.run(SwegApplication.class, args);
	}
}
