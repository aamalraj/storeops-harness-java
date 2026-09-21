package com.storeops.common.events;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Synchronous in-process {@link EventBus}.
 *
 * <p>Stub implementation: delivery is immediate and on the publishing thread. A subscriber that
 * fails is logged and skipped so one bad handler cannot fail the publisher's request.
 */
@Component
public class InMemoryEventBus implements EventBus {

  private static final Logger LOG = LoggerFactory.getLogger(InMemoryEventBus.class);

  private final Map<DomainEventType, List<Consumer<DomainEvent>>> handlers =
      new ConcurrentHashMap<>();

  @Override
  public void publish(DomainEvent event) {
    List<Consumer<DomainEvent>> subscribers = handlers.getOrDefault(event.type(), List.of());
    LOG.debug("Publishing {} for {} to {} subscriber(s)",
        event.type(), event.aggregateId(), subscribers.size());
    for (Consumer<DomainEvent> subscriber : subscribers) {
      try {
        subscriber.accept(event);
      } catch (RuntimeException failure) {
        LOG.warn("Subscriber failed handling {} for {}", event.type(), event.aggregateId(), failure);
      }
    }
  }

  @Override
  public void subscribe(DomainEventType type, Consumer<DomainEvent> handler) {
    handlers.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>()).add(handler);
  }
}
