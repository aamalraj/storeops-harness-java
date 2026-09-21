package com.storeops.programmes.domain;

import java.time.Instant;

/**
 * A staff member's membership of a programme.
 *
 * <p>Holds only the staff id: staff details are read from the staff module's directory port when
 * needed, so programmes never copies or owns staff data.
 *
 * @param staffId the member, as known to the staff module
 * @param role the part they play
 * @param addedAt when they joined
 */
public record ProgrammeMember(String staffId, ProgrammeRole role, Instant addedAt) {
}
