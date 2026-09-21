package com.storeops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the StoreOps retail operations API. */
@SpringBootApplication
public class StoreOpsApplication {

  /** Spring instantiates the configuration class; nothing else should. */
  protected StoreOpsApplication() {
  }

  public static void main(String[] args) {
    SpringApplication.run(StoreOpsApplication.class, args);
  }
}
