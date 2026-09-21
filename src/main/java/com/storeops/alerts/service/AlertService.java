package com.storeops.alerts.service;

import com.storeops.alerts.domain.Alert;
import com.storeops.alerts.domain.AlertSeverity;
import com.storeops.alerts.repository.AlertRepository;
import com.storeops.common.auth.Actor;
import com.storeops.common.events.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Operational alert use cases.
 *
 * <p>Stub implementation. Alerts are never created by another module calling in: they are derived
 * from {@link DomainEvent}s delivered by {@link AlertEventSubscriber}, which is what lets the
 * alerts module stay invisible to its notifiers.
 */
@Service
public class AlertService {

  private final AlertRepository repository;

  public AlertService(AlertRepository repository) {
    this.repository = repository;
  }

  /** Lists alerts addressed to the calling user. */
  public List<Alert> listForActor(Actor actor, boolean unacknowledgedOnly) {
    return repository.findByRecipient(actor.userId(), unacknowledgedOnly);
  }

  /**
   * Derives an alert from a published event.
   *
   * <p>Stub routing: the alert goes to the affected assignee when the event names one, otherwise
   * back to the staff member who caused it.
   */
  public Alert raiseFromEvent(DomainEvent event, AlertSeverity severity, String subject) {
    String recipientId = payloadString(event, "assigneeId", event.actorId());
    Alert alert = new Alert(
        UUID.randomUUID().toString(),
        event.storeId(),
        recipientId,
        severity,
        subject,
        event.type().name(),
        event.aggregateId(),
        false,
        Instant.now());
    return repository.save(alert);
  }

  private static String payloadString(DomainEvent event, String key, String fallback) {
    Object value = event.payload().get(key);
    return value instanceof String text && !text.isBlank() ? text : fallback;
  }
}
