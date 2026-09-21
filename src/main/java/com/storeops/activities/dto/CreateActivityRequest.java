package com.storeops.activities.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/activities.
 *
 * <p>Enum-valued fields arrive as strings and are parsed in the service so an unknown value becomes
 * a 400 VALIDATION_FAILED naming the accepted values.
 *
 * @param title short description of the work, required
 * @param programmeId owning programme, optional
 * @param category one of {@link com.storeops.activities.domain.ActivityCategory}, required
 * @param priority one of {@link com.storeops.activities.domain.ActivityPriority}, defaults to
 *     NORMAL when absent
 * @param assigneeId staff member to assign, optional, must belong to the caller's store
 */
public record CreateActivityRequest(
    @NotBlank(message = "must not be blank") @Size(max = 160, message = "must be at most 160 characters")
    String title,
    String programmeId,
    @NotBlank(message = "must not be blank") String category,
    String priority,
    String assigneeId) {
}
