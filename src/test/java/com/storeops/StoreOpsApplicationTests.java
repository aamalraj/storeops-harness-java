package com.storeops;

import static org.assertj.core.api.Assertions.assertThat;

import com.storeops.common.events.EventBus;
import com.storeops.staff.api.StaffDirectory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/** Smoke test: the context wires all five modules and their shared infrastructure. */
@SpringBootTest
class StoreOpsApplicationTests {

  @Autowired
  private ApplicationContext context;

  @Test
  @DisplayName("The application context loads with every module present")
  void contextLoads() {
    assertThat(context.getBean(EventBus.class)).isNotNull();
    assertThat(context.getBean(StaffDirectory.class)).isNotNull();
    assertThat(context.getBeanNamesForType(Object.class)).isNotEmpty();
  }

  @Test
  @DisplayName("Every module contributes a route, a service and a repository")
  void everyModuleHasThreeLayers() {
    for (String module : new String[] {"activities", "programmes", "staff", "alerts", "reports"}) {
      assertThat(beansInPackage("com.storeops." + module + ".web"))
          .as("%s routes", module).isPositive();
      assertThat(beansInPackage("com.storeops." + module + ".service"))
          .as("%s service", module).isPositive();
      assertThat(beansInPackage("com.storeops." + module + ".repository"))
          .as("%s repository", module).isPositive();
    }
  }

  private long beansInPackage(String packageName) {
    return java.util.Arrays.stream(context.getBeanDefinitionNames())
        .map(context::getType)
        .filter(java.util.Objects::nonNull)
        .filter(type -> type.getName().startsWith(packageName + "."))
        .count();
  }
}
