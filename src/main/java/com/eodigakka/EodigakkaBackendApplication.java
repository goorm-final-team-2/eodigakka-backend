package com.eodigakka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EodigakkaBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(EodigakkaBackendApplication.class, args);
  }
}
