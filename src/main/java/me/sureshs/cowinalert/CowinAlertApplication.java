package me.sureshs.cowinalert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CowinAlertApplication {

	public static void main(String[] args) {
		SpringApplication.run(CowinAlertApplication.class, args);
	}

}
