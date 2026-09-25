package com.storeops.activities.service;

import com.storeops.activities.domain.Activity;
import com.storeops.activities.domain.ActivityCategory;
import com.storeops.activities.domain.ActivityPriority;
import com.storeops.activities.domain.ActivityStatus;
import com.storeops.activities.dto.BulkStatusUpdateRequest;
import com.storeops.activities.dto.BulkStatusUpdateResponse;
import com.storeops.activities.dto.CreateActivityRequest;
import com.storeops.activities.dto.UpdateActivityRequest;
import com.storeops.activities.repository.ActivityRepository;
import com.storeops.common.auth.Actor;
import com.storeops.common.error.AppError;
import com.storeops.common.error.ConflictError;
import com.storeops.common.error.ForbiddenError;
import com.storeops.common.error.NotFoundError;
import com.storeops.common.error.ValidationError;
import com.storeops.common.events.DomainEvent;
import com.storeops.common.events.DomainEventType;
import com.storeops.common.events.EventBus;
import com.storeops.common.util.Enums;
import com.storeops.staff.api.StaffDirectory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Operational activity use cases.
 *
 * <p>Stub implementation: the shape of each use case — scope check, validation, persistence, event
 * publication — is in place, but the business rules are deliberately minimal.
 *
 * <p>Module boundaries observed here: staff is reached only through the read-only
 * {@link StaffDirectory} port, and the alerts module is notified only by publishing to the
 * {@link EventBus}.
 */
@Service
public class ActivityService {

  private final ActivityRepository repository;
  private final StaffDirectory staffDirectory;
  private final EventBus eventBus;

  public ActivityService(
      ActivityRepository repository, StaffDirectory staffDirectory, EventBus eventBus) {
    this.repository = repository;
    this.staffDirectory = staffDirectory;
    this.eventBus = eventBus;
  }

  /** Lists the caller's store activities, optionally filtered by programme and status. */
  public List<Activity> list(Actor actor, String programmeId, String statusFilter) {
    ActivityStatus status = Enums.parseOptional(ActivityStatus.class, statusFilter, "status")
        .orElse(null);
    String programme = normalise(programmeId);
    return repository.findByStore(actor.storeId(), programme, status);
  }

  /** Creates an activity owned by the caller. */
  public Activity create(Actor actor, CreateActivityRequest request) {
    ActivityCategory category = Enums.parse(ActivityCategory.class, request.category(), "category");
    ActivityPriority priority =
        Enums.parseOptional(ActivityPriority.class, request.priority(), "priority")
            .orElse(ActivityPriority.NORMAL);
    String assigneeId = requireStoreMember(actor, normalise(request.assigneeId()));

    Instant now = Instant.now();
    Activity activity = new Activity(
        UUID.randomUUID().toString(),
        actor.storeId(),
        normalise(request.programmeId()),
        request.title().trim(),
        ActivityStatus.PLANNED,
        priority,
        category,
        actor.userId(),
        assigneeId,
        now,
        now);

    Activity saved = repository.save(activity);
    Map<String, Object> payload = new HashMap<>();
    payload.put("priority", saved.priority().name());
    payload.put("category", saved.category().name());
    if (saved.assigneeId() != null) {
      payload.put("assigneeId", saved.assigneeId());
    }
    publish(DomainEventType.ACTIVITY_CREATED, saved, actor, payload);
    return saved;
  }

  /** Reads one activity, enforcing store scope. */
  public Activity getById(Actor actor, String id) {
    Activity activity = repository.findById(id)
        .orElseThrow(() -> new NotFoundError("Activity", id));
    if (!activity.storeId().equals(actor.storeId())) {
      throw new NotFoundError("Activity", id);
    }
    return activity;
  }

  /** Applies a partial update to status, priority, category and/or assignee. */
  public Activity update(Actor actor, String id, UpdateActivityRequest request) {
    if (request.isEmpty()) {
      throw new ValidationError(
          "At least one of status, priority, category or assigneeId must be supplied");
    }
    Activity activity = getById(actor, id);
    if (activity.isClosed()) {
      throw new ConflictError("Activity '" + id + "' is " + activity.status() + " and cannot "
          + "be modified");
    }

    Instant now = Instant.now();
    Activity updated = applyStatus(activity, request.status(), now);
    updated = applyPriority(updated, request.priority(), now);
    updated = applyCategory(updated, request.category(), now);
    updated = applyAssignee(updated, actor, request.assigneeId(), now);

    Activity saved = repository.save(updated);
    publish(DomainEventType.ACTIVITY_UPDATED, saved, actor,
        Map.of("status", saved.status().name(), "priority", saved.priority().name()));
    return saved;
  }

  /**
   * Transitions a batch of activities to DONE or BLOCKED — the shift handover close-out flow.
   *
   * <p>Each item is processed independently: one item's failure is recorded in that item's
   * {@link BulkStatusUpdateResponse.Outcome} and does not stop the remaining items from being
   * attempted. An {@code ACTIVITY_UPDATED} event is published only for items that succeed.
   */
  public BulkStatusUpdateResponse bulkUpdateStatus(Actor actor, BulkStatusUpdateRequest request) {
    List<BulkStatusUpdateRequest.Item> items = request.updates();
    if (items == null || items.isEmpty()) {
      throw new ValidationError("At least one update must be supplied");
    }

    List<BulkStatusUpdateResponse.Outcome> outcomes = new ArrayList<>();
    for (BulkStatusUpdateRequest.Item item : items) {
      outcomes.add(applyBulkStatusItem(actor, item));
    }
    return new BulkStatusUpdateResponse(outcomes);
  }

  private BulkStatusUpdateResponse.Outcome applyBulkStatusItem(
      Actor actor, BulkStatusUpdateRequest.Item item) {
    try {
      if (item.id() == null || item.id().isBlank()) {
        throw new ValidationError("id must not be blank");
      }
      ActivityStatus target = parseBulkTargetStatus(item.status());
      Activity activity = getById(actor, item.id());
      if (activity.isClosed()) {
        throw new ConflictError("Activity '" + item.id() + "' is " + activity.status()
            + " and cannot be modified");
      }
      Activity saved = repository.save(activity.withStatus(target, Instant.now()));
      publish(DomainEventType.ACTIVITY_UPDATED, saved, actor,
          Map.of("status", saved.status().name()));
      return BulkStatusUpdateResponse.Outcome.success(saved.id(), saved.status());
    } catch (AppError error) {
      return BulkStatusUpdateResponse.Outcome.failure(item.id(), error);
    }
  }

  /** Bulk-status is narrower than the general update: only DONE or BLOCKED are accepted. */
  private static ActivityStatus parseBulkTargetStatus(String raw) {
    ActivityStatus status = Enums.parse(ActivityStatus.class, raw, "status");
    if (status != ActivityStatus.DONE && status != ActivityStatus.BLOCKED) {
      throw new ValidationError(
          "status must be one of [DONE, BLOCKED] for bulk updates, but was '" + status + "'");
    }
    return status;
  }

  /**
   * Deletes an activity.
   *
   * <p>Only the owner or a store manager may delete: this is the one endpoint in the module with a
   * non-trivial authorisation rule.
   */
  public void delete(Actor actor, String id) {
    Activity activity = getById(actor, id);
    if (!actor.owns(activity.ownerId()) && !actor.role().isManager()) {
      throw new ForbiddenError("Only the activity owner or a store manager may delete it");
    }
    if (!repository.deleteById(id)) {
      throw new NotFoundError("Activity", id);
    }
    publish(DomainEventType.ACTIVITY_DELETED, activity, actor, Map.of());
  }

  /** Read model used by the reports module through the activities query port. */
  public Map<ActivityStatus, Long> countByStatus(String storeId) {
    List<Activity> activities = repository.findByStore(storeId, null, null);
    Map<ActivityStatus, Long> counts = new EnumMap<>(ActivityStatus.class);
    for (ActivityStatus status : ActivityStatus.values()) {
      counts.put(status, activities.stream()
          .filter(activity -> activity.status() == status)
          .count());
    }
    return counts;
  }

  /** Counts activities not yet in a closed state, for the reports module. */
  public long countOpen(String storeId) {
    return repository.findByStore(storeId, null, null).stream()
        .filter(activity -> !activity.isClosed())
        .count();
  }

  private Activity applyStatus(Activity activity, String raw, Instant now) {
    return Enums.parseOptional(ActivityStatus.class, raw, "status")
        .map(status -> activity.withStatus(status, now))
        .orElse(activity);
  }

  private Activity applyPriority(Activity activity, String raw, Instant now) {
    return Enums.parseOptional(ActivityPriority.class, raw, "priority")
        .map(priority -> activity.withPriority(priority, now))
        .orElse(activity);
  }

  private Activity applyCategory(Activity activity, String raw, Instant now) {
    return Enums.parseOptional(ActivityCategory.class, raw, "category")
        .map(category -> activity.withCategory(category, now))
        .orElse(activity);
  }

  private Activity applyAssignee(Activity activity, Actor actor, String raw, Instant now) {
    String assigneeId = normalise(raw);
    if (assigneeId == null) {
      return activity;
    }
    return activity.withAssignee(requireStoreMember(actor, assigneeId), now);
  }

  /** Validates an assignee against the read-only staff directory. */
  private String requireStoreMember(Actor actor, String staffId) {
    if (staffId == null) {
      return null;
    }
    if (!staffDirectory.existsInStore(staffId, actor.storeId())) {
      throw new ValidationError("Assignee '" + staffId + "' is not a member of this store");
    }
    return staffId;
  }

  private void publish(
      DomainEventType type, Activity activity, Actor actor, Map<String, Object> payload) {
    eventBus.publish(DomainEvent.of(type, activity.id(), activity.storeId(), actor.userId(),
        payload));
  }

  private static String normalise(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
