package edu.hm.crownet.testbed.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "edu.hm.crownet.testbed.api",
        "edu.hm.crownet.testbed.beacon",
        "edu.hm.crownet.testbed.client",
        "edu.hm.crownet.testbed.scheduler",
        "edu.hm.crownet.testbed.ratecontrol",
        "edu.hm.crownet.testbed.analytics",
        "edu.hm.crownet.testbed.message"
})
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
} 