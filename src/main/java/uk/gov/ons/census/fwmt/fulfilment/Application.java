package uk.gov.ons.census.fwmt.fulfilment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRetry
@EnableScheduling
@EnableIntegration
@EnableJpaRepositories("uk.gov.ons.census.fwmt.fulfilment.repository")
@ComponentScan({"uk.gov.ons.census.fwmt.fulfilment", "uk.gov.ons.census.fwmt.events"})
public class Application {

  public static final String APPLICATION_NAME = "FWMT Gateway Fulfilment Service";

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
