package com.storeops.activities.dto;

import com.storeops.activities.domain.Activity;
import com.storeops.activities.domain.ActivityCategory;
import com.storeops.activities.domain.ActivityPriority;
import com.storeops.activities.domain.ActivityStatus;
import java.time.Instant;

/** Wire representation of an activity. */
public record ActivityResponse(
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

  public static ActivityResponse from(Activity activity) {
    return new ActivityResponse(
        activity.id(),
        activity.storeId(),
        activity.programmeId(),
        activity.title(),
        activity.status(),
        activity.priority(),
        activity.category(),
        activity.ownerId(),
        activity.assigneeId(),
        activity.createdAt(),
        activity.updatedAt());
  }
}
