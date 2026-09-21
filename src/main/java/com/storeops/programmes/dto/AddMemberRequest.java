package com.storeops.programmes.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of POST /api/programmes/{id}/members.
 *
 * @param staffId staff member to add, must belong to the programme's store
 * @param role one of {@link com.storeops.programmes.domain.ProgrammeRole}, defaults to CONTRIBUTOR
 */
public record AddMemberRequest(
    @NotBlank(message = "must not be blank") String staffId, String role) {
}
