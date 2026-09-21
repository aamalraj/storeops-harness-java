package com.storeops.common.events;

import java.time.Instant;
import java.util.Map;

/**
 * An immutable fact published by one module and consumed by any number of others.
 *
 * <p>The payload is intentionally a loose map: it keeps publishers from having to share their
 * domain types with subscribers, which is what would reintroduce a module dependency.
 *
 * @param type what happened
 * @param aggregateId identifier of the record the event is about
 * @param storeId store the record belongs to
 * @param actorId user who caused the event
 * @param payload additional, subscriber-specific attributes
 * @param occurredAt when the fact was recorded
 */
public record DomainEvent(
    DomainEventType type,
    String aggregateId,
    String storeId,
    String actorId,
    Map<String, Object> payload,
    Instant occurredAt) {

  public DomainEvent {
    payload = Map.copyOf(payload);
  }

  public static DomainEvent of(
      DomainEventType type, String aggregateId, String storeId, String actorId) {
    return new DomainEvent(type, aggregateId, storeId, actorId, Map.of(), Instant.now());
  }

  public static DomainEvent of(
      DomainEventType type,
      String aggregateId,
      String storeId,
      String actorId,
      Map<String, Object> payload) {
    return new DomainEvent(type, aggregateId, storeId, actorId, payload, Instant.now());
  }
}
