package com.storeops.alerts.service;

import com.storeops.alerts.domain.AlertSeverity;
import com.storeops.common.events.DomainEvent;
import com.storeops.common.events.DomainEventType;
import com.storeops.common.events.EventBus;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Turns published domain events into alerts.
 *
 * <p>This class is the alerts module's only inbound seam. Because activities and programmes just
 * publish to the {@link EventBus}, neither needs to know this class — or the alerts module —
 * exists; the module-boundary test asserts that no module depends on {@code com.storeops.alerts}.
 */
@Component
public class AlertEventSubscriber {

  private final EventBus eventBus;
  private final AlertService alertService;

  public AlertEventSubscriber(EventBus eventBus, AlertService alertService) {
    this.eventBus = eventBus;
    this.alertService = alertService;
  }

  @PostConstruct
  void register() {
    eventBus.subscribe(DomainEventType.ACTIVITY_CREATED, this::onActivityCreated);
    eventBus.subscribe(DomainEventType.ACTIVITY_UPDATED, this::onActivityUpdated);
    eventBus.subscribe(DomainEventType.PROGRAMME_MEMBER_ADDED, this::onProgrammeMemberAdded);
  }

  private void onActivityCreated(DomainEvent event) {
    alertService.raiseFromEvent(event, severityFor(event), "New activity assigned to your store");
  }

  private void onActivityUpdated(DomainEvent event) {
    alertService.raiseFromEvent(event, AlertSeverity.INFO, "An activity you follow changed");
  }

  private void onProgrammeMemberAdded(DomainEvent event) {
    alertService.raiseFromEvent(event, AlertSeverity.INFO, "You were added to a programme");
  }

  /** Stub severity policy: critical activities raise a warning, everything else is informational. */
  private static AlertSeverity severityFor(DomainEvent event) {
    return "CRITICAL".equals(event.payload().get("priority"))
        ? AlertSeverity.WARNING
        : AlertSeverity.INFO;
  }
}
