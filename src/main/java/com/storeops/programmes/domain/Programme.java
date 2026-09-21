package com.storeops.programmes.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A store programme: a named body of work that activities can be grouped under.
 *
 * @param id programme identifier
 * @param storeId store that runs the programme
 * @param name display name
 * @param status lifecycle state
 * @param ownerId staff member accountable for the programme
 * @param members programme membership, never null
 * @param createdAt creation timestamp
 * @param updatedAt timestamp of the last change
 */
public record Programme(
    String id,
    String storeId,
    String name,
    ProgrammeStatus status,
    String ownerId,
    List<ProgrammeMember> members,
    Instant createdAt,
    Instant updatedAt) {

  public Programme {
    members = List.copyOf(members);
  }

  /** Returns a copy with {@code member} appended. */
  public Programme withMember(ProgrammeMember member, Instant at) {
    List<ProgrammeMember> next = new ArrayList<>(members);
    next.add(member);
    return new Programme(id, storeId, name, status, ownerId, next, createdAt, at);
  }

  /** True when {@code staffId} is already a member. */
  public boolean hasMember(String staffId) {
    return members.stream().anyMatch(member -> member.staffId().equals(staffId));
  }

  /** True when the programme no longer accepts membership changes. */
  public boolean isClosed() {
    return status == ProgrammeStatus.COMPLETED || status == ProgrammeStatus.ARCHIVED;
  }
}
