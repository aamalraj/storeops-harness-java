package com.storeops.common.events;

/**
 * Catalogue of facts modules may announce.
 *
 * <p>Publishers own the event type; subscribers (today: the alerts module) react to it. Because the
 * catalogue lives in {@code common}, neither side needs to import the other.
 */
public enum DomainEventType {
  ACTIVITY_CREATED,
  ACTIVITY_UPDATED,
  ACTIVITY_DELETED,
  PROGRAMME_CREATED,
  PROGRAMME_MEMBER_ADDED
}
