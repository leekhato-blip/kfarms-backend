package com.kfarms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class KfarmsBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(KfarmsBackendApplication.class, args);
	}
	public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer(){
		return builder -> {
			builder.simpleDateFormat("yyyy-MM-dd HH:mm:ss");
			builder.timeZone(TimeZone.getTimeZone("Africa/Lagos"));
		};
	}

}
