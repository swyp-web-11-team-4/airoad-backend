package com.swygbro.airoad.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AiroadBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(AiroadBackendApplication.class, args);
  }
}
