package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class LostAndFountBackendApplication {

	static final String APPLICATION_TIME_ZONE = "Europe/Belgrade";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(APPLICATION_TIME_ZONE));
		SpringApplication.run(LostAndFountBackendApplication.class, args);
	}

}
