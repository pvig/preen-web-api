package com.pvig.preenWebApi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PreenWebApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PreenWebApiApplication.class, args);
	}

}
