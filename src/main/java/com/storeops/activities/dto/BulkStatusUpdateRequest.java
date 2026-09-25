package com.storeops.activities.dto;

import java.util.List;

/**
 * Body of PATCH /api/activities/bulk-status.
 *
 * <p>{@code status} is validated in the service and is restricted to {@code DONE} or
 * {@code BLOCKED} for this endpoint — the general-purpose status/priority/category/assignee
 * update stays on {@code PATCH /api/activities/{id}}.
 *
 * @param updates the activities to transition; at least one entry is required
 */
public record BulkStatusUpdateRequest(List<Item> updates) {

  /**
   * One activity to transition.
   *
   * @param id activity identifier
   * @param status target status, must be DONE or BLOCKED
   */
  public record Item(String id, String status) {
  }
}
