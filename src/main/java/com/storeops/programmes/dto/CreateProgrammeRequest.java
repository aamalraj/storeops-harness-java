package com.storeops.programmes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/programmes.
 *
 * @param name display name, required
 * @param status one of {@link com.storeops.programmes.domain.ProgrammeStatus}, defaults to DRAFT
 */
public record CreateProgrammeRequest(
    @NotBlank(message = "must not be blank") @Size(max = 120, message = "must be at most 120 characters")
    String name,
    String status) {
}
