package com.smarttask;

import com.smarttask.config.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartTaskApplication {

	public static void main(String[] args) {
		DotEnvLoader.load();
		SpringApplication.run(SmartTaskApplication.class, args);
	}

}
