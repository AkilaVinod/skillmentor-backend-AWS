package com.stemlink.skillmentor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SkillmentorApplication {

	public static void main(String[] args) {

        SpringApplication.run(SkillmentorApplication.class, args);
	}

}
