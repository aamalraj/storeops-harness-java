package com.storeops.activities.domain;

import java.time.Instant;

/**
 * A unit of operational work at a store.
 *
 * <p>Immutable: updates produce a copy via the {@code with*} helpers so the repository never hands
 * out aliased mutable state.
 *
 * @param id activity identifier
 * @param storeId store the activity belongs to
 * @param programmeId owning programme, null for standalone activities
 * @param title short description of the work
 * @param status current lifecycle state
 * @param priority urgency
 * @param category kind of work
 * @param ownerId staff member who created the activity
 * @param assigneeId staff member responsible for it, may be null
 * @param createdAt creation timestamp
 * @param updatedAt timestamp of the last change
 */
public record Activity(
    String id,
    String storeId,
    String programmeId,
    String title,
    ActivityStatus status,
    ActivityPriority priority,
    ActivityCategory category,
    String ownerId,
    String assigneeId,
    Instant createdAt,
    Instant updatedAt) {

  public Activity withStatus(ActivityStatus newStatus, Instant at) {
    return new Activity(id, storeId, programmeId, title, newStatus, priority, category,
        ownerId, assigneeId, createdAt, at);
  }

  public Activity withPriority(ActivityPriority newPriority, Instant at) {
    return new Activity(id, storeId, programmeId, title, status, newPriority, category,
        ownerId, assigneeId, createdAt, at);
  }

  public Activity withCategory(ActivityCategory newCategory, Instant at) {
    return new Activity(id, storeId, programmeId, title, status, priority, newCategory,
        ownerId, assigneeId, createdAt, at);
  }

  public Activity withAssignee(String newAssigneeId, Instant at) {
    return new Activity(id, storeId, programmeId, title, status, priority, category,
        ownerId, newAssigneeId, createdAt, at);
  }

  /** True once the activity has reached a state that no longer accepts edits. */
  public boolean isClosed() {
    return status == ActivityStatus.DONE || status == ActivityStatus.CANCELLED;
  }
}
