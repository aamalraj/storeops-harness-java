package com.storeops.activities.dto;

import com.storeops.activities.domain.ActivityStatus;
import com.storeops.common.error.AppError;
import java.util.List;

/**
 * Body of the PATCH /api/activities/bulk-status response.
 *
 * <p>Always 200 OK: the batch is a partial-success operation, not all-or-nothing, so failure of
 * one item is reported in that item's {@link Outcome} rather than as the response's HTTP status.
 *
 * @param results one outcome per requested item, in the same order as the request
 */
public record BulkStatusUpdateResponse(List<Outcome> results) {

  /**
   * Outcome of one item in the batch.
   *
   * @param id activity identifier
   * @param succeeded whether the update was applied
   * @param status the activity's resulting status; null when not succeeded
   * @param errorCode the {@link AppError#getCode()} that blocked this item; null when succeeded
   * @param errorMessage the {@link AppError} message that blocked this item; null when succeeded
   */
  public record Outcome(
      String id, boolean succeeded, String status, String errorCode, String errorMessage) {

    public static Outcome success(String id, ActivityStatus status) {
      return new Outcome(id, true, status.name(), null, null);
    }

    public static Outcome failure(String id, AppError error) {
      return new Outcome(id, false, null, error.getCode(), error.getMessage());
    }
  }
}
