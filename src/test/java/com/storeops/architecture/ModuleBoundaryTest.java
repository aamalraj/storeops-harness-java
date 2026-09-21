package com.storeops.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.storeops.common.error.AppError;
import com.storeops.staff.api.StaffDirectory;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Executable statement of the StoreOps module boundary rules.
 *
 * <p>These are the rules a reviewer would otherwise have to enforce by reading imports. They run as
 * part of {@code mvn test}, so a boundary violation fails the build rather than a code review.
 */
@AnalyzeClasses(packages = "com.storeops", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryTest {

  /**
   * Layer package patterns.
   *
   * <p>Qualified with {@code com.storeops} on purpose: a bare {@code ..web..} would also match
   * {@code org.springframework.web}, so every route class would look like a layering violation.
   */
  private static final String ROUTES = "com.storeops.*.web..";
  private static final String SERVICES = "com.storeops.*.service..";
  private static final String REPOSITORIES = "com.storeops.*.repository..";

  /** Rule: no circular imports between modules. */
  @ArchTest
  static final ArchRule MODULES_ARE_ACYCLIC =
      slices().matching("com.storeops.(*)..")
          .should().beFreeOfCycles();

  /**
   * Rule: notifications travel by event bus only.
   *
   * <p>No module may reference the alerts module at all: activities and programmes publish a
   * {@code DomainEvent} and the alerts subscriber picks it up.
   */
  @ArchTest
  static final ArchRule ALERTS_IS_REACHED_ONLY_VIA_THE_EVENT_BUS =
      noClasses().that().resideOutsideOfPackage("com.storeops.alerts..")
          .should().dependOnClassesThat().resideInAPackage("com.storeops.alerts..")
          .because("other modules must notify alerts by publishing to the EventBus");

  /**
   * Rule: staff is read-only for other modules.
   *
   * <p>Outside callers get the narrow {@link StaffDirectory} port; the staff service, repository
   * and routes are private to the module.
   */
  @ArchTest
  static final ArchRule STAFF_IS_READ_ONLY_FOR_OTHER_MODULES =
      noClasses().that().resideOutsideOfPackage("com.storeops.staff..")
          .should().dependOnClassesThat().resideInAnyPackage(
              "com.storeops.staff.service..",
              "com.storeops.staff.repository..",
              "com.storeops.staff.web..")
          .because("other modules may only read staff through the StaffDirectory port");

  /** Rule: the staff port itself exposes no mutating operation. */
  @ArchTest
  static final ArchRule STAFF_PORT_EXPOSES_ONLY_QUERIES =
      methods().that().areDeclaredIn(StaffDirectory.class)
          .should().haveNameMatching("^(find|get|list|exists|count).*")
          .because("the staff directory is a read-only port");

  /** Rule: routes are an entry point, never a collaborator. */
  @ArchTest
  static final ArchRule ROUTES_ARE_NOT_DEPENDED_ON =
      noClasses().that().resideOutsideOfPackage(ROUTES)
          .should().dependOnClassesThat().resideInAPackage(ROUTES)
          .because("the route layer is the outermost layer");

  /** Rule: layering runs Routes to Service to Repository, never the other way. */
  @ArchTest
  static final ArchRule SERVICES_DO_NOT_DEPEND_ON_ROUTES =
      noClasses().that().resideInAPackage(SERVICES)
          .should().dependOnClassesThat().resideInAPackage(ROUTES);

  /** Rule: repositories are reached through their own module's service layer. */
  @ArchTest
  static final ArchRule REPOSITORIES_ARE_REACHED_THROUGH_SERVICES =
      classes().that().resideInAPackage(REPOSITORIES)
          .should().onlyBeAccessed().byAnyPackage(REPOSITORIES, SERVICES)
          .because("only a service may talk to a repository");

  /** Rule: domain records and DTOs never reach back into the layers above them. */
  @ArchTest
  static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_LAYERS =
      noClasses().that().resideInAPackage("com.storeops.*.domain..")
          .should().dependOnClassesThat().resideInAnyPackage(
              ROUTES, SERVICES, REPOSITORIES);

  /** Rule: no raw throws in services or routes — only the typed AppError hierarchy. */
  @ArchTest
  static final ArchRule SERVICES_AND_ROUTES_RAISE_ONLY_APP_ERRORS =
      noClasses().that().resideInAnyPackage(SERVICES, ROUTES)
          .should(constructNonAppErrorThrowables())
          .because("services and routes must raise a typed AppError, never a raw throwable");

  private static ArchCondition<JavaClass> constructNonAppErrorThrowables() {
    return new ArchCondition<>("construct a throwable outside the AppError hierarchy") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        for (JavaConstructorCall call : item.getConstructorCallsFromSelf()) {
          JavaClass raised = call.getTargetOwner();
          if (raised.isAssignableTo(Throwable.class) && !raised.isAssignableTo(AppError.class)) {
            events.add(SimpleConditionEvent.satisfied(call,
                call.getDescription() + " constructs " + raised.getName()));
          }
        }
      }
    };
  }
}
