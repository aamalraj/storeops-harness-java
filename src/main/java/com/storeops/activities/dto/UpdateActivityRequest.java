package com.storeops.activities.dto;

/**
 * Body of PATCH /api/activities/{id}. Every field is optional; at least one must be present.
 *
 * @param status new lifecycle state
 * @param priority new urgency
 * @param category new kind of work
 * @param assigneeId new assignee, must belong to the caller's store
 */
public record UpdateActivityRequest(
    String status, String priority, String category, String assigneeId) {

  /** True when the caller supplied nothing to change. */
  public boolean isEmpty() {
    return isBlank(status) && isBlank(priority) && isBlank(category) && isBlank(assigneeId);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
