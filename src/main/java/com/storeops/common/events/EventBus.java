package com.storeops.common.events;

import java.util.function.Consumer;

/**
 * The only sanctioned channel for cross-module notification.
 *
 * <p>A module that wants to tell the rest of the system something publishes a {@link DomainEvent};
 * it must not call another module's service to do so. The ArchUnit module-boundary test enforces
 * that no module depends on the alerts module directly.
 */
public interface EventBus {

  /** Announces a fact to every handler subscribed to {@code event.type()}. */
  void publish(DomainEvent event);

  /** Registers {@code handler} for a single event type. */
  void subscribe(DomainEventType type, Consumer<DomainEvent> handler);
}
